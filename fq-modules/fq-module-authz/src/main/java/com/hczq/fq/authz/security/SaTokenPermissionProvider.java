package com.hczq.fq.authz.security;

import cn.dev33.satoken.stp.StpInterface;
import com.hczq.fq.authz.service.PermissionService;
import com.hczq.fq.platform.entity.Platform;
import com.hczq.fq.user.mapper.RoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Feeds Sa-Token's {@code @SaCheckPermission}. Permissions are calculated only for this centre's own platform:
 * roles and permissions of connected platforms have their own meaning there and must never satisfy a check here.
 */
@Component
public class SaTokenPermissionProvider implements StpInterface {

    private final PermissionService permissionService;
    private final RoleMapper roleMapper;

    public SaTokenPermissionProvider(PermissionService permissionService, RoleMapper roleMapper) {
        this.permissionService = permissionService;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.copyOf(permissionService.permissionsOf(Long.valueOf(loginId.toString())));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleMapper.selectCodesByUserIdInPlatform(Long.valueOf(loginId.toString()), Platform.CENTER_CODE);
    }
}
