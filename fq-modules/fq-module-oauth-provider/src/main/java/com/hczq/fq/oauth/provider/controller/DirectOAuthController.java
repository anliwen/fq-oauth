package com.hczq.fq.oauth.provider.controller;

import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.oauth.provider.dto.*;
import com.hczq.fq.oauth2.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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

    private static ResponseEntity<?> execute(java.util.function.Supplier<Map<String, Object>> operation) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operation.get());
        } catch (BusinessException e) {
            int status = e.getStatus() == 403 ? 403 : e.getStatus() == 401 ? 401 : 400;
            String error = e.getStatus() == 401 ? "invalid_client" : e.getStatus() == 403 ? "access_denied" : "invalid_request";
            return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("error", error, "error_description", e.getMessage()));
        }
    }
}
