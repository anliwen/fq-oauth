package com.hczq.fq.authz.service;

import com.hczq.fq.authz.dto.PermissionCreate;
import com.hczq.fq.authz.dto.PermissionNode;
import com.hczq.fq.authz.dto.PermissionUpdate;
import com.hczq.fq.authz.entity.Permission;
import com.hczq.fq.authz.mapper.PermissionMapper;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.platform.entity.Platform;
import com.hczq.fq.platform.service.PlatformService;
import com.hczq.fq.user.entity.Role;
import com.hczq.fq.user.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final PlatformService platformService;
    private final RoleMapper roleMapper;

    public PermissionService(PermissionMapper permissionMapper, PlatformService platformService,
                             RoleMapper roleMapper) {
        this.permissionMapper = permissionMapper;
        this.platformService = platformService;
        this.roleMapper = roleMapper;
    }

    // ---------------------------------------------------------------- the tree

    public List<PermissionNode> tree(Long platformId) {
        platformService.require(platformId);
        Map<Long, PermissionNode> nodes = new LinkedHashMap<>();
        for (Permission p : permissionMapper.selectByPlatform(platformId)) {
            nodes.put(p.getId(), PermissionNode.of(p));
        }
        List<PermissionNode> roots = new ArrayList<>();
        for (PermissionNode n : nodes.values()) {
            PermissionNode parent = n.parentId() == null ? null : nodes.get(n.parentId());
            if (parent != null) {
                parent.children().add(n);
            } else {
                roots.add(n);
            }
        }
        return roots;
    }

    @Transactional
    public PermissionNode create(PermissionCreate req) {
        Platform platform = platformService.require(req.platformId());
        Permission parent = null;
        if (req.parentId() != null) {
            parent = permissionMapper.selectById(req.parentId());
            if (parent == null || !parent.getPlatformId().equals(platform.getId())) {
                throw BusinessException.notFound("上级节点不存在");
            }
        }
        checkDepth(parent);
        if (permissionMapper.selectByPlatformAndCode(platform.getId(), req.code()) != null) {
            throw BusinessException.conflict("该平台下权限编码已存在");
        }
        Permission p = new Permission();
        p.setPlatformId(platform.getId());
        p.setParentId(parent == null ? null : parent.getId());
        p.setCode(req.code());
        p.setName(req.name());
        p.setType(req.type());
        p.setSort(req.sort());
        p.setIsSystem(false);
        permissionMapper.insert(p);
        return PermissionNode.of(permissionMapper.selectById(p.getId()));
    }

    /**
     * The tree is at most {@link Permission#MAX_DEPTH} levels deep, the root being level 1. A parent never changes
     * after creation, so walking up from it always terminates and the depth of a node is stable.
     */
    private void checkDepth(Permission parent) {
        int level = 0;
        for (Permission cur = parent; cur != null; cur = cur.getParentId() == null ? null : permissionMapper.selectById(cur.getParentId())) {
            level++;
            if (level >= Permission.MAX_DEPTH) {
                throw BusinessException.badRequest("权限树最多 " + Permission.MAX_DEPTH + " 层，不能再往下添加");
            }
        }
    }

    @Transactional
    public PermissionNode update(Long id, PermissionUpdate req) {
        Permission p = load(id);
        if (req.name() != null) p.setName(req.name());
        if (req.sort() != null) p.setSort(req.sort());
        permissionMapper.update(p);
        return PermissionNode.of(permissionMapper.selectById(id));
    }

    /** Grants of the node disappear with it (foreign key cascade). */
    @Transactional
    public void delete(Long id) {
        Permission p = load(id);
        if (Boolean.TRUE.equals(p.getIsSystem())) {
            throw BusinessException.conflict("内置权限已被代码引用，不可删除");
        }
        long children = permissionMapper.countChildren(id);
        if (children > 0) {
            throw BusinessException.conflict("该节点下还有 " + children + " 个子节点，请先删除子节点");
        }
        permissionMapper.delete(id);
    }

    private Permission load(Long id) {
        Permission p = permissionMapper.selectById(id);
        if (p == null) {
            throw BusinessException.notFound("权限 " + id + " 不存在");
        }
        return p;
    }

    // ---------------------------------------------------------- role grants

    public List<Long> grantedIds(Long roleId) {
        loadRole(roleId);
        return permissionMapper.selectIdsByRole(roleId);
    }

    /**
     * Replaces the permissions of a role. Only permissions of the role's own platform are accepted, and every
     * ancestor of a granted node is granted too, so a button never exists without the page that contains it.
     */
    @Transactional
    public List<Long> grant(Long roleId, List<Long> ids) {
        Role role = loadRole(roleId);
        if (Platform.CENTER_CODE.equals(role.getPlatformCode()) && Platform.CENTER_ADMIN_ROLE.equals(role.getCode())) {
            throw BusinessException.conflict("该角色始终拥有本平台的全部权限，无需分配");
        }
        Map<Long, Permission> byId = new LinkedHashMap<>();
        permissionMapper.selectByPlatform(role.getPlatformId()).forEach(p -> byId.put(p.getId(), p));

        Set<Long> wanted = new LinkedHashSet<>(ids);
        for (Long id : wanted) {
            if (!byId.containsKey(id)) {
                throw BusinessException.badRequest("包含不属于该角色所属平台的权限: " + id);
            }
        }
        Set<Long> full = new LinkedHashSet<>(wanted);
        for (Long id : wanted) {
            Long cur = byId.get(id).getParentId();
            while (cur != null && full.add(cur)) {
                Permission parent = byId.get(cur);
                cur = parent == null ? null : parent.getParentId();
            }
        }
        permissionMapper.deleteGrants(roleId);
        if (!full.isEmpty()) {
            permissionMapper.insertGrants(roleId, full);
        }
        return permissionMapper.selectIdsByRole(roleId);
    }

    private Role loadRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色 " + roleId + " 不存在");
        }
        return role;
    }

    // ------------------------------------------------------- what a user may do

    /**
     * Permission codes of this centre that a user holds. The centre's own {@code superadmin} role always holds every code
     * (also codes added later), so it can never lock itself out; everyone else holds what their roles of the centre
     * platform were granted.
     */
    public Set<String> permissionsOf(Long userId) {
        List<String> roles = roleMapper.selectCodesByUserIdInPlatform(userId, Platform.CENTER_CODE);
        if (roles.contains(Platform.CENTER_ADMIN_ROLE)) {
            return new TreeSet<>(permissionMapper.selectAllCodes(Platform.CENTER_CODE));
        }
        return new TreeSet<>(permissionMapper.selectGrantedCodes(userId, Platform.CENTER_CODE));
    }
}
