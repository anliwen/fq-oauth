package com.hczq.fq.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.common.api.PageResult;
import com.hczq.fq.user.dto.ResetPasswordIn;
import com.hczq.fq.user.dto.UserCreate;
import com.hczq.fq.user.dto.UserRead;
import com.hczq.fq.user.dto.UserStatusUpdate;
import com.hczq.fq.user.dto.UserUpdate;
import com.hczq.fq.user.security.CurrentUser;
import com.hczq.fq.user.service.UserService;
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

/** User management. Every endpoint is guarded by a permission code of this centre. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @SaCheckPermission("user:view")
    public ApiResponse<PageResult<UserRead>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "dept_id", required = false) Long deptId,
            @RequestParam(required = false) String status) {
        int p = Math.max(page, 1);
        int size = Math.min(Math.max(pageSize, 1), 100);
        return ApiResponse.ok(userService.list(p, size, keyword, deptId, status));
    }

    @PostMapping
    @SaCheckPermission("user:create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserRead> create(@Valid @RequestBody UserCreate req) {
        return ApiResponse.ok(userService.create(req));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("user:view")
    public ApiResponse<UserRead> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("user:update")
    public ApiResponse<UserRead> update(@PathVariable Long id, @Valid @RequestBody UserUpdate req) {
        return ApiResponse.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("user:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id, currentUser.require());
        return ApiResponse.message("删除成功");
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("user:update")
    public ApiResponse<UserRead> setStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdate req) {
        return ApiResponse.ok(userService.setStatus(id, req.status()));
    }

    @PutMapping("/{id}/password/reset")
    @SaCheckPermission("user:update")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordIn req) {
        userService.resetPassword(id, req.newPassword());
        return ApiResponse.message("密码已重置");
    }
}
