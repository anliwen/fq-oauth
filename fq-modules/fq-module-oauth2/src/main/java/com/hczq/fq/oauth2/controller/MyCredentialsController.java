package com.hczq.fq.oauth2.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.oauth2.dto.ApiKeyCreate;
import com.hczq.fq.oauth2.dto.ApiKeyRead;
import com.hczq.fq.oauth2.dto.UserTokenRead;
import com.hczq.fq.oauth2.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class MyCredentialsController {
    private final OAuthService service;

    public MyCredentialsController(OAuthService service) {
        this.service = service;
    }

    @GetMapping("/api-keys")
    @SaCheckPermission("credential:view")
    public ApiResponse<List<ApiKeyRead>> keys() {
        return ApiResponse.ok(service.listApiKeys());
    }

    @PostMapping("/api-keys")
    @SaCheckPermission("credential:create")
    public ApiResponse<ApiKeyRead> createKey(@Valid @RequestBody ApiKeyCreate req) {
        return ApiResponse.ok(service.createApiKey(req));
    }

    @PostMapping("/api-keys/{id}/issue")
    @SaCheckPermission("credential:issue")
    public ApiResponse<ApiKeyRead> issueKey(@PathVariable long id) {
        return ApiResponse.ok(service.issueApiKey(id));
    }

    @PostMapping("/api-keys/{id}/revoke")
    @SaCheckPermission("credential:revoke")
    public ApiResponse<Void> revokeKey(@PathVariable Long id) {
        service.revokeApiKey(id);
        return ApiResponse.message("API Key 已撤销");
    }

    @PostMapping("/api-keys/{id}/rotate")
    @SaCheckPermission("credential:rotate")
    public ApiResponse<ApiKeyRead> rotateKey(@PathVariable long id) {
        return ApiResponse.ok(service.rotateApiKey(id));
    }

    @GetMapping("/tokens")
    @SaCheckPermission("token:view")
    public ApiResponse<List<UserTokenRead>> tokens() {
        return ApiResponse.ok(service.listUserTokens());
    }

    @PostMapping("/tokens/{id}/revoke")
    @SaCheckPermission("token:revoke")
    public ApiResponse<Void> revokeToken(@PathVariable Long id) {
        service.revokeUserToken(id);
        return ApiResponse.message("令牌已撤销");
    }
}
