package com.hczq.fq.user.service;

import com.hczq.fq.common.api.PageResult;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.org.service.DeptService;
import com.hczq.fq.user.dto.UserCreate;
import com.hczq.fq.user.dto.UserRead;
import com.hczq.fq.user.dto.UserUpdate;
import com.hczq.fq.user.entity.Role;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.entity.UserStatus;
import com.hczq.fq.user.mapper.RoleMapper;
import com.hczq.fq.user.mapper.UserMapper;
import com.hczq.fq.user.security.PasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordService passwords;
    private final SessionRevoker sessions;
    private final DeptService deptService;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, PasswordService passwords,
                       SessionRevoker sessions, DeptService deptService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwords = passwords;
        this.sessions = sessions;
        this.deptService = deptService;
    }

    /** status and is_active always move together; this is the only place that writes them. */
    private static void applyStatus(User user, String status) {
        user.setStatus(UserStatus.validate(status));
        user.setIsActive(UserStatus.active.name().equals(user.getStatus()));
    }

    public UserRead toRead(User u) {
        List<Role> roles = roleMapper.selectByUserId(u.getId());
        return new UserRead(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getEmployeeNo(),
                u.getTitle(), u.getDeptId(), u.getDeptName(), u.getStatus(),
                Boolean.TRUE.equals(u.getIsActive()), roles.stream().map(Role::getId).toList(),
                roles.stream().map(Role::getCode).toList(), u.getLastLoginAt(), u.getCreateTime(),
                u.getUpdateTime());
    }

    public PageResult<UserRead> list(int page, int pageSize, String keyword, Long deptId, String status) {
        String st = status == null ? null : UserStatus.validate(status);
        String kw = keyword == null || keyword.isBlank() ? null : keyword;
        int offset = (page - 1) * pageSize;
        List<User> users = userMapper.selectPage(kw, deptId, st, offset, pageSize);
        long total = userMapper.countPage(kw, deptId, st);
        return new PageResult<>(users.stream().map(this::toRead).toList(), total, page, pageSize);
    }

    public UserRead get(Long id) {
        return toRead(load(id));
    }

    @Transactional
    public UserRead create(UserCreate req) {
        if (userMapper.selectByUsername(req.username()) != null) {
            throw BusinessException.conflict("用户名 " + req.username() + " 已存在");
        }
        if (userMapper.selectByEmail(req.email()) != null) {
            throw BusinessException.conflict("邮箱 " + req.email() + " 已存在");
        }
        if (req.employeeNo() != null && !req.employeeNo().isEmpty()
                && userMapper.selectByEmployeeNo(req.employeeNo()) != null) {
            throw BusinessException.conflict("工号 " + req.employeeNo() + " 已存在");
        }
        List<Long> roleIds = loadRoleIds(req.roleIds());
        Long deptId = checkDept(req.deptId());
        PasswordService.validateStrength(req.password());

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setFullName(req.fullName());
        user.setEmployeeNo(blankToNull(req.employeeNo()));
        user.setTitle(req.title());
        user.setDeptId(deptId);
        user.setHashedPassword(passwords.hash(req.password()));
        applyStatus(user, UserStatus.active.name());
        userMapper.insert(user);
        if (!roleIds.isEmpty()) {
            userMapper.insertRoles(user.getId(), roleIds);
        }
        return toRead(userMapper.selectById(user.getId()));
    }

    @Transactional
    public UserRead update(Long id, UserUpdate req) {
        User user = load(id);

        if (req.has("email") && !Objects.equals(req.getEmail(), user.getEmail())) {
            if (req.getEmail() != null && userMapper.selectByEmail(req.getEmail()) != null) {
                throw BusinessException.conflict("邮箱 " + req.getEmail() + " 已存在");
            }
        }
        if (req.has("employeeNo") && !Objects.equals(req.getEmployeeNo(), user.getEmployeeNo())) {
            if (req.getEmployeeNo() != null && !req.getEmployeeNo().isEmpty()
                    && userMapper.selectByEmployeeNo(req.getEmployeeNo()) != null) {
                throw BusinessException.conflict("工号 " + req.getEmployeeNo() + " 已存在");
            }
        }

        if (req.has("status") && req.getStatus() != null) applyStatus(user, req.getStatus());
        if (req.has("email") && req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.has("fullName") && req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.has("employeeNo")) user.setEmployeeNo(blankToNull(req.getEmployeeNo()));
        if (req.has("title")) user.setTitle(req.getTitle());
        if (req.has("deptId")) user.setDeptId(checkDept(req.getDeptId()));
        userMapper.update(user);

        if (req.has("roleIds") && req.getRoleIds() != null) {
            List<Long> roleIds = loadRoleIds(req.getRoleIds());
            userMapper.deleteRoles(id);
            if (!roleIds.isEmpty()) {
                userMapper.insertRoles(id, roleIds);
            }
        }
        if (!user.getIsActive()) {
            sessions.revoke(id);
        }
        return toRead(userMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id, User operator) {
        if (id.equals(operator.getId())) {
            throw BusinessException.conflict("不能删除当前登录用户");
        }
        load(id);
        userMapper.softDelete(id);
        sessions.revoke(id);
    }

    @Transactional
    public UserRead setStatus(Long id, String status) {
        User user = load(id);
        applyStatus(user, status);
        userMapper.updateStatus(id, user.getStatus(), user.getIsActive());
        if (!user.getIsActive()) {
            sessions.revoke(id);
        }
        return toRead(userMapper.selectById(id));
    }

    /** token_version + 1 and every live session of the target is ended: they must log in with the new password. */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        PasswordService.validateStrength(newPassword);
        load(id);
        userMapper.updatePassword(id, passwords.hash(newPassword));
        sessions.revoke(id);
    }

    private User load(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户 " + id + " 不存在");
        }
        return user;
    }

    private List<Long> loadRoleIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = ids.stream().distinct().toList();
        List<Role> roles = roleMapper.selectByIds(new HashSet<>(distinct));
        if (roles.size() != distinct.size()) {
            throw BusinessException.notFound("部分角色不存在");
        }
        return distinct;
    }

    /** A department must exist and not be deleted; null means "no department". */
    private Long checkDept(Long deptId) {
        return deptId == null ? null : deptService.require(deptId).getId();
    }

    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
