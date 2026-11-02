package com.hczq.fq.org.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.org.dto.DeptCreate;
import com.hczq.fq.org.dto.DeptNode;
import com.hczq.fq.org.dto.DeptUpdate;
import com.hczq.fq.org.service.DeptService;
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

/** Department management. Every endpoint is guarded by a permission code of this centre. */
@RestController
@RequestMapping("/api/v1/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /** Also needed by the user page, to pick a department. */
    @GetMapping
    @SaCheckPermission(value = {"dept:view", "user:view"}, mode = SaMode.OR)
    public ApiResponse<List<DeptNode>> tree() {
        return ApiResponse.ok(deptService.tree());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("dept:create")
    public ApiResponse<DeptNode> create(@Valid @RequestBody DeptCreate req) {
        return ApiResponse.ok(deptService.create(req));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("dept:update")
    public ApiResponse<DeptNode> update(@PathVariable Long id, @Valid @RequestBody DeptUpdate req) {
        return ApiResponse.ok(deptService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("dept:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return ApiResponse.message("删除成功");
    }
}
