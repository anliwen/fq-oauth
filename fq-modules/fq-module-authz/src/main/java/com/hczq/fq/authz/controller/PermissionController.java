package com.hczq.fq.authz.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.hczq.fq.authz.dto.PermissionCreate;
import com.hczq.fq.authz.dto.PermissionGrant;
import com.hczq.fq.authz.dto.PermissionNode;
import com.hczq.fq.authz.dto.PermissionUpdate;
import com.hczq.fq.authz.service.PermissionService;
import com.hczq.fq.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The permission tree of a platform, and what each role is granted from it. */
@RestController
@RequestMapping("/api/v1")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/permissions")
    @SaCheckPermission(value = {"permission:view", "role:view"}, mode = SaMode.OR)
    public ApiResponse<List<PermissionNode>> tree(@RequestParam("platform_id") Long platformId) {
        return ApiResponse.ok(permissionService.tree(platformId));
    }

    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("permission:create")
    public ApiResponse<PermissionNode> create(@Valid @RequestBody PermissionCreate req) {
        return ApiResponse.ok(permissionService.create(req));
    }

    @PutMapping("/permissions/{id}")
    @SaCheckPermission("permission:update")
    public ApiResponse<PermissionNode> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdate req) {
        return ApiResponse.ok(permissionService.update(id, req));
    }

    @DeleteMapping("/permissions/{id}")
    @SaCheckPermission("permission:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.message("删除成功");
    }

    /** Ids of the permissions granted to a role. */
    @GetMapping("/roles/{roleId}/permissions")
    @SaCheckPermission(value = {"role:view", "permission:view"}, mode = SaMode.OR)
    public ApiResponse<List<Long>> granted(@PathVariable Long roleId) {
        return ApiResponse.ok(permissionService.grantedIds(roleId));
    }

    /** Replaces the permissions of a role; the response is what the role holds afterwards (with ancestors added). */
    @PutMapping("/roles/{roleId}/permissions")
    @SaCheckPermission("role:grant")
    public ApiResponse<List<Long>> grant(@PathVariable Long roleId, @Valid @RequestBody PermissionGrant req) {
        return ApiResponse.ok(permissionService.grant(roleId, req.permissionIds()));
    }
}
