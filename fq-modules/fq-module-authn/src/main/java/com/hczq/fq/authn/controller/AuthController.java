package com.hczq.fq.authn.controller;

import com.hczq.fq.authn.dto.ChangePasswordIn;
import com.hczq.fq.authn.dto.LoginIn;
import com.hczq.fq.authn.dto.MeResponse;
import com.hczq.fq.authn.dto.TokenResponse;
import com.hczq.fq.authn.service.AuthService;
import com.hczq.fq.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Public. Slider captcha is not part of this system, so it is always reported as disabled.
     */
    @GetMapping("/captcha-config")
    public ApiResponse<Map<String, Boolean>> captchaConfig() {
        return ApiResponse.ok(Map.of("enabled", false));
    }

    /**
     * Public. Returns the bare token response (not wrapped), like alphadesk's OAuth2-style endpoint.
     */
    @PostMapping("/token")
    public TokenResponse login(@Valid @RequestBody LoginIn req, HttpServletRequest request) {
        return authService.login(req.username(), req.password(), request.getRemoteAddr());
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.message("已退出登录");
    }

    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordIn req) {
        authService.changePassword(req.oldPassword(), req.newPassword());
        return ApiResponse.message("密码修改成功，请重新登录");
    }
}
