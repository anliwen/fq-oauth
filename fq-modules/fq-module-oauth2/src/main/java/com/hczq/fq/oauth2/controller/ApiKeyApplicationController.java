package com.hczq.fq.oauth2.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.oauth2.dto.ApiKeyApplicationRead;
import com.hczq.fq.oauth2.dto.ApiKeyReview;
import com.hczq.fq.oauth2.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.hczq.fq.common.api.PageResult;

@RestController
@RequestMapping("/api/v1/api-key-applications")
@SaCheckPermission("api-key-review:view")
public class ApiKeyApplicationController {
    private final OAuthService service;

    public ApiKeyApplicationController(OAuthService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<ApiKeyApplicationRead>> list(@RequestParam(defaultValue="1") int page, @RequestParam(name="page_size", defaultValue="10") int pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String status) {
        int p = Math.max(page, 1), size = Math.min(Math.max(pageSize, 1), 100);
        return ApiResponse.ok(service.listApiKeyApplications(p, size, keyword, status));
    }

    @PostMapping("/{id}/review")
    @SaCheckPermission("api-key:review")
    public ApiResponse<Void> review(@PathVariable long id, @Valid @RequestBody ApiKeyReview request) {
        service.reviewApiKey(id, request.approved(), request.comment());
        return ApiResponse.message(request.approved() ? "API Key 申请已通过" : "API Key 申请已拒绝");
    }

    @PostMapping("/{id}/revoke")
    @SaCheckPermission("api-key:admin-revoke")
    public ApiResponse<Void> revoke(@PathVariable long id) {
        service.revokeApiKeyByAdmin(id);
        return ApiResponse.message("API Key 已禁用");
    }
}
