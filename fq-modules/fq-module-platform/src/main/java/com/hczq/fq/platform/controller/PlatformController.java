package com.hczq.fq.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.platform.dto.PlatformCreate;
import com.hczq.fq.platform.dto.PlatformRead;
import com.hczq.fq.platform.dto.PlatformUpdate;
import com.hczq.fq.platform.service.PlatformService;
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

/** Platform management. Every endpoint is guarded by a permission code of this centre. */
@RestController
@RequestMapping("/api/v1/platforms")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    @SaCheckPermission(value = {"platform:view", "role:view", "permission:view", "user:view"}, mode = SaMode.OR)
    public ApiResponse<List<PlatformRead>> list() {
        return ApiResponse.ok(platformService.list());
    }

    @PostMapping
    @SaCheckPermission("platform:create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlatformRead> create(@Valid @RequestBody PlatformCreate req) {
        return ApiResponse.ok(platformService.create(req));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("platform:update")
    public ApiResponse<PlatformRead> update(@PathVariable Long id, @Valid @RequestBody PlatformUpdate req) {
        return ApiResponse.ok(platformService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("platform:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        platformService.delete(id);
        return ApiResponse.message("删除成功");
    }
}
