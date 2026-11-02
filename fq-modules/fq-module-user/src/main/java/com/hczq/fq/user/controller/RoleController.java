package com.hczq.fq.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.user.dto.RoleCreate;
import com.hczq.fq.user.dto.RoleRead;
import com.hczq.fq.user.dto.RoleUpdate;
import com.hczq.fq.user.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @SaCheckPermission(value = {"role:view", "user:view", "permission:view"}, mode = SaMode.OR)
    public ApiResponse<List<RoleRead>> list() {
        return ApiResponse.ok(roleService.list());
    }

    @PostMapping
    @SaCheckPermission("role:create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoleRead> create(@Valid @RequestBody RoleCreate req) {
        return ApiResponse.ok(roleService.create(req));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("role:update")
    public ApiResponse<RoleRead> update(@PathVariable Long id, @RequestBody RoleUpdate req) {
        return ApiResponse.ok(roleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("role:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.message("删除成功");
    }
}
