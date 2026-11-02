package com.hczq.fq.user.service;

import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.platform.entity.Platform;
import com.hczq.fq.platform.service.PlatformService;
import com.hczq.fq.user.dto.RoleCreate;
import com.hczq.fq.user.dto.RoleRead;
import com.hczq.fq.user.dto.RoleUpdate;
import com.hczq.fq.user.entity.Role;
import com.hczq.fq.user.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final PlatformService platformService;

    public RoleService(RoleMapper roleMapper, PlatformService platformService) {
        this.roleMapper = roleMapper;
        this.platformService = platformService;
    }

    public List<RoleRead> list() {
        return roleMapper.selectAll().stream().map(this::toRead).toList();
    }

    @Transactional
    public RoleRead create(RoleCreate req) {
        Platform platform = platformService.require(req.platformId());
        if (roleMapper.selectByPlatformAndCode(platform.getId(), req.code()) != null) {
            throw BusinessException.conflict("该平台下角色编码已存在");
        }
        Role role = new Role();
        role.setPlatformId(platform.getId());
        role.setCode(req.code());
        role.setName(req.name());
        role.setDesc(req.desc());
        role.setIsSystem(false);
        roleMapper.insert(role);
        return toRead(roleMapper.selectById(role.getId()));
    }

    /** The platform and the code are immutable; name and description can be changed, including on built-in roles. */
    @Transactional
    public RoleRead update(Long id, RoleUpdate req) {
        Role role = load(id);
        if (req.has("name") && req.getName() != null) role.setName(req.getName());
        if (req.has("desc") && req.getDesc() != null) role.setDesc(req.getDesc());
        roleMapper.update(role);
        return toRead(roleMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        Role role = load(id);
        if (role.getIsSystem()) {
            throw BusinessException.conflict("内置角色不可删除");
        }
        long count = roleMapper.countUsers(id);
        if (count > 0) {
            throw BusinessException.conflict("仍有 " + count + " 名用户引用该角色，请先解绑");
        }
        roleMapper.softDelete(id);
    }

    private Role load(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.notFound("角色 " + id + " 不存在");
        }
        return role;
    }

    private RoleRead toRead(Role r) {
        return new RoleRead(r.getId(), r.getPlatformId(), r.getPlatformCode(), r.getPlatformName(), r.getCode(), r.getName(), r.getDesc(),
                Boolean.TRUE.equals(r.getIsSystem()), roleMapper.countUsers(r.getId()),
                r.getCreateTime(), r.getUpdateTime());
    }
}
