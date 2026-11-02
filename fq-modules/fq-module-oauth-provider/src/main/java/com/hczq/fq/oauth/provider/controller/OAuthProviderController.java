package com.hczq.fq.oauth.provider.controller;

import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.oauth2.dto.*;
import com.hczq.fq.oauth2.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/** Public OAuth 2.1 protocol surface. Token and authorization business logic stays in OAuthService. */
@RestController
public class OAuthProviderController {
    private final OAuthService service;
    public OAuthProviderController(OAuthService service) { this.service = service; }

    @GetMapping("/oauth/authorize")
    public ResponseEntity<?> authorize(@RequestParam(name="response_type") String responseType, @RequestParam(name="client_id") String clientId,
            @RequestParam(name="redirect_uri") String redirectUri, @RequestParam(required=false) String scope,
            @RequestParam(required=false, defaultValue="") String state, @RequestParam(name="code_challenge") String challenge,
            @RequestParam(name="code_challenge_method", defaultValue="S256") String method) {
        try {
            if (!"code".equals(responseType)) throw new ProtocolException("unsupported_response_type", "仅支持授权码模式");
            service.validateAuthorizationRequest(clientId, redirectUri, scope, state, challenge, method);
            String target = UriComponentsBuilder.fromPath("/consent").queryParam("client_id", clientId).queryParam("redirect_uri", redirectUri)
                    .queryParamIfPresent("scope", Optional.ofNullable(scope)).queryParam("state", state).queryParam("code_challenge", challenge)
                    .queryParam("code_challenge_method", method).build().encode().toUriString();
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, target).build();
        } catch (ProtocolException e) { return error(400, e.code, e.getMessage()); }
        catch (com.hczq.fq.common.exception.BusinessException e) { return error(e.getStatus() == 401 ? 401 : 400, "invalid_request", e.getMessage()); }
    }

    @GetMapping("/api/v1/oauth/authorize")
    public ApiResponse<AuthorizeContext> authorizeContext(@RequestParam(name="client_id") String clientId, @RequestParam(name="redirect_uri") String redirectUri,
            @RequestParam(required=false) String scope, @RequestParam(required=false, defaultValue="") String state,
            @RequestParam(name="code_challenge") String challenge, @RequestParam(name="code_challenge_method", defaultValue="S256") String method) {
        return ApiResponse.ok(service.authorizeContext(clientId, redirectUri, scope, state, challenge, method));
    }

    @PostMapping("/oauth/authorize/confirm")
    public ApiResponse<String> confirm(@Valid @RequestBody AuthorizeConfirm req) { return ApiResponse.ok(service.confirm(req)); }

    @GetMapping("/oauth/userinfo")
    public ResponseEntity<?> userInfo(HttpServletRequest request) {
        String h = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (h == null || !h.startsWith("Bearer ")) return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        try { return noStore(service.userInfo(h.substring(7).trim())); }
        catch (com.hczq.fq.common.exception.BusinessException e) { return error(e.getStatus(), "invalid_token", e.getMessage()); }
    }

    @PostMapping(value="/oauth/token", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> tokenForm(@RequestParam MultiValueMap<String,String> form, HttpServletRequest request) { return protocol(() -> service.token(tokenRequest(form, clientId(form, request)))); }

    @PostMapping(value="/oauth/token", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tokenJson(@RequestBody Map<String,String> body, HttpServletRequest request) {
        MultiValueMap<String,String> form = new org.springframework.util.LinkedMultiValueMap<>(); body.forEach(form::add);
        return protocol(() -> service.token(tokenRequest(form, clientId(form, request))));
    }

    @PostMapping(value="/oauth/revoke", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> revokeForm(@RequestParam MultiValueMap<String,String> form, HttpServletRequest request) { return revoke(form, request); }
    @PostMapping(value="/oauth/revoke", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> revokeJson(@RequestBody Map<String,String> body, HttpServletRequest request) {
        MultiValueMap<String,String> form = new org.springframework.util.LinkedMultiValueMap<>(); body.forEach(form::add); return revoke(form, request);
    }
    /** Convenience logout endpoint: revokes the supplied access or refresh token. */
    @PostMapping(value="/oauth/logout", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> logoutForm(@RequestParam MultiValueMap<String,String> form, HttpServletRequest request) { return revoke(form, request); }
    @PostMapping(value="/oauth/logout", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> logoutJson(@RequestBody Map<String,String> body, HttpServletRequest request) {
        MultiValueMap<String,String> form = new org.springframework.util.LinkedMultiValueMap<>(); body.forEach(form::add); return revoke(form, request);
    }
    private ResponseEntity<?> revoke(MultiValueMap<String,String> form, HttpServletRequest request) {
        return protocol(() -> { service.revoke(new RevokeRequest(required(form, "token"), clientId(form, request), form.getFirst("client_secret"))); return Map.of(); });
    }

    @PostMapping(value="/oauth/introspect", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> introspect(@RequestParam MultiValueMap<String,String> form, HttpServletRequest request) {
        return introspectionProtocol(() -> {
            String hint = form.getFirst("token_type_hint");
            if ("api_key".equals(hint) && (request.getHeader(HttpHeaders.AUTHORIZATION) == null || !request.getHeader(HttpHeaders.AUTHORIZATION).startsWith("Basic "))) throw new ProtocolException("invalid_client", "API Key 校验必须使用 HTTP Basic 客户端认证");
            return service.introspect(required(form, "token"), hint, clientId(form, request), form.getFirst("client_secret"), form.getFirst("resource"));
        });
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String,Object> metadata(HttpServletRequest request) {
        String issuer = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
        return Map.of("issuer", issuer, "authorization_endpoint", issuer + "/oauth/authorize", "token_endpoint", issuer + "/oauth/token", "revocation_endpoint", issuer + "/oauth/revoke", "introspection_endpoint", issuer + "/oauth/introspect", "userinfo_endpoint", issuer + "/oauth/userinfo", "response_types_supported", new String[]{"code"}, "grant_types_supported", new String[]{"authorization_code", "refresh_token"}, "code_challenge_methods_supported", new String[]{"S256"}, "token_endpoint_auth_methods_supported", new String[]{"none", "client_secret_post", "client_secret_basic"});
    }

    private static TokenRequest tokenRequest(MultiValueMap<String,String> f, String clientId) { return new TokenRequest(required(f, "grant_type"), f.getFirst("code"), f.getFirst("redirect_uri"), clientId, f.getFirst("code_verifier"), f.getFirst("refresh_token"), f.getFirst("client_secret")); }
    private static String clientId(MultiValueMap<String,String> f, HttpServletRequest request) {
        String h = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith("Basic ")) {
            try { String[] c = new String(Base64.getDecoder().decode(h.substring(6)), StandardCharsets.UTF_8).split(":", 2); if (c.length == 2) { f.set("client_secret", c[1]); return c[0]; } }
            catch (IllegalArgumentException ignored) { throw new ProtocolException("invalid_client", "客户端认证失败"); }
            throw new ProtocolException("invalid_client", "客户端认证失败");
        }
        return required(f, "client_id");
    }
    private static String required(MultiValueMap<String,String> f, String key) { String value = f.getFirst(key); if (value == null || value.isBlank()) throw new ProtocolException("invalid_request", "缺少参数 " + key); return value; }
    private static ResponseEntity<?> protocol(Supplier<Map<String,Object>> action) { try { return noStore(action.get()); } catch (ProtocolException e) { return error(400, e.code, e.getMessage()); } catch (com.hczq.fq.common.exception.BusinessException e) { return error(e.getStatus() == 401 ? 401 : 400, "UNAUTHORIZED".equals(e.getCode()) ? "invalid_client" : "invalid_grant", e.getMessage()); } }
    private static ResponseEntity<?> introspectionProtocol(Supplier<Map<String,Object>> action) { try { return noStore(action.get()); } catch (ProtocolException e) { return error(400, e.code, e.getMessage()); } catch (com.hczq.fq.common.exception.BusinessException e) { return error(e.getStatus() == 401 ? 401 : e.getStatus() == 403 ? 403 : 400, "UNAUTHORIZED".equals(e.getCode()) ? "invalid_client" : "access_denied", e.getMessage()); } }
    private static ResponseEntity<?> noStore(Object body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA, "no-cache").body(body); }
    private static ResponseEntity<Map<String,String>> error(int status, String code, String message) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA, "no-cache").body(Map.of("error", code, "error_description", message)); }
    private static final class ProtocolException extends RuntimeException { private final String code; private ProtocolException(String code, String message) { super(message); this.code = code; } }
}
