package com.hczq.fq.oauth.provider.controller;

import com.hczq.fq.common.api.ResultCode;
import com.hczq.fq.common.api.ResultResponse;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.oauth.provider.dto.*;
import com.hczq.fq.oauth2.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Non-browser direct login contract for registered terminal applications. */
@RestController
@RequestMapping("/oauth/direct")
public class DirectOAuthController {
    private final OAuthService service;
    public DirectOAuthController(OAuthService service) { this.service = service; }

    @PostMapping("/token")
    public ResponseEntity<?> login(@Valid @RequestBody DirectLoginRequest request) {
        return execute(() -> service.directLogin(request.clientId(), request.clientSecret(), request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody DirectRefreshRequest request) {
        return execute(() -> service.directRefresh(request.clientId(), request.clientSecret(), request.refreshToken()));
    }

    @PostMapping("/introspect")
    public ResponseEntity<?> introspect(@Valid @RequestBody DirectTokenRequest request) {
        return execute(() -> service.directIntrospect(request.clientId(), request.clientSecret(), request.token()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody DirectTokenRequest request) {
        return execute(() -> {
            service.revoke(new com.hczq.fq.oauth2.dto.RevokeRequest(request.token(), request.clientId(), request.clientSecret()));
            return Map.of("revoked", true);
        });
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultResponse<Void>> invalidRequest() {
        return response(400, ResultResponse.failure(ResultCode.REQUEST_INVALID, "请求参数校验失败"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultResponse<Void>> unreadableRequest() {
        return response(400, ResultResponse.failure(ResultCode.REQUEST_INVALID, "请求参数格式错误"));
    }

    private static ResponseEntity<?> execute(java.util.function.Supplier<Map<String, Object>> operation) {
        try {
            return response(200, ResultResponse.success(operation.get()));
        } catch (BusinessException e) {
            int status = e.getStatus() == 403 ? 403 : e.getStatus() == 401 ? 401 : 400;
            return response(status, ResultResponse.failure(codeOf(e), e.getMessage()));
        }
    }

    private static ResultCode codeOf(BusinessException e) {
        return switch (e.getMessage()) {
            case "客户端认证失败", "缺少客户端密钥", "公共客户端不得使用客户端密钥" -> ResultCode.CLIENT_SECRET_INVALID;
            case "用户名或密码错误" -> ResultCode.LOGIN_FAILED;
            case "客户端未关联有效平台" -> ResultCode.PLATFORM_INVALID;
            case "用户未关联客户端所属平台的角色" -> ResultCode.PLATFORM_ROLE_MISSING;
            case "直接账号密码登录仅允许终端应用客户端" -> ResultCode.CLIENT_TYPE_INVALID;
            default -> e.getStatus() == 401 ? ResultCode.TOKEN_INVALID
                    : e.getStatus() == 403 ? ResultCode.ACCESS_DENIED : ResultCode.REQUEST_INVALID;
        };
    }

    private static <T> ResponseEntity<ResultResponse<T>> response(int status, ResultResponse<T> body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
