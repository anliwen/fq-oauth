package com.hczq.fq.oauth2.service;

import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.common.api.PageResult;
import com.hczq.fq.oauth2.dto.*;
import com.hczq.fq.user.security.CurrentUser;
import com.hczq.fq.oauth2.mapper.OAuthMapper;
import com.hczq.fq.platform.service.PlatformService;
import com.hczq.fq.authz.service.PermissionService;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.mapper.RoleMapper;
import com.hczq.fq.user.mapper.UserMapper;
import com.hczq.fq.user.security.PasswordService;
import com.hczq.fq.platform.entity.Platform;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.security.MessageDigest;

@Service
public class OAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final String TOKEN_CACHE_PREFIX = "oauth:token:";
    private final OAuthMapper mapper;
    private final CurrentUser currentUser;
    private final PlatformService platforms;
    private final UserMapper users;
    private final RoleMapper roles;
    private final PermissionService permissions;
    private final PasswordService passwords;
    private final StringRedisTemplate redis;

    @Autowired
    public OAuthService(OAuthMapper mapper, CurrentUser currentUser, PlatformService platforms,
                        UserMapper users, RoleMapper roles, PermissionService permissions, PasswordService passwords,
                        StringRedisTemplate redis) {
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.platforms = platforms;
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.passwords = passwords;
        this.redis = redis;
    }

    /** Compatibility constructor for isolated service tests; production wiring uses platform validation. */
    public OAuthService(OAuthMapper mapper, CurrentUser currentUser) {
        this(mapper, currentUser, null, null, null, null, null, null);
    }

    /** Direct username/password login for registered APPLICATION clients; no browser redirect is involved. */
    @Transactional
    public Map<String, Object> directLogin(String clientId, String clientSecret, String username, String password) {
        Map<String, Object> c = directClient(clientId, clientSecret);
        User user = users == null || passwords == null ? null : users.selectByUsername(username);
        if (user == null || !passwords.verify(password, user.getHashedPassword()) || Boolean.TRUE.equals(user.getIsDeleted())
                || !"active".equals(user.getStatus()) || !Boolean.TRUE.equals(user.getIsActive())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        userMapperUpdateLastLogin(user);
        Map<String, Object> token = issueTokens(clientId, user.getId(), (String) c.get("scopes"));
        token.putAll(identity(user, clientId, (String) c.get("scopes")));
        return token;
    }

    /** Refreshes a direct-login token pair. The underlying OAuth service rotates the refresh token. */
    public Map<String, Object> directRefresh(String clientId, String clientSecret, String refreshToken) {
        directClient(clientId, clientSecret);
        return token(new TokenRequest("refresh_token", null, null, clientId, null, refreshToken, clientSecret));
    }

    public Map<String, Object> directIntrospect(String clientId, String clientSecret, String accessToken) {
        directClient(clientId, clientSecret);
        Map<String, Object> status = introspect(accessToken, "access_token", clientId, clientSecret);
        if (Boolean.TRUE.equals(status.get("active"))) {
            status = new HashMap<>(status);
            status.putAll(userInfo(accessToken));
        }
        return status;
    }

    private Map<String, Object> directClient(String clientId, String clientSecret) {
        Map<String, Object> c = client(clientId);
        if (!"APPLICATION".equals(c.get("client_kind"))) throw BusinessException.forbidden("直接账号密码登录仅允许终端应用客户端");
        authenticateTokenClient(c, clientSecret);
        return c;
    }

    private void userMapperUpdateLastLogin(User user) { users.updateLastLogin(user.getId(), LocalDateTime.now()); }

    private Map<String, Object> identity(User user, String clientId, String scope) {
        return Map.of("user", Map.of("id", user.getId(), "username", user.getUsername(), "fullName", user.getFullName(), "email", user.getEmail()),
                "roles", roles.selectCodesByUserIdInPlatform(user.getId(), Platform.CENTER_CODE),
                "permissions", permissions.permissionsOf(user.getId()), "client_id", clientId, "scope", scope);
    }

    public Map<String, Object> userInfo(String rawAccessToken) {
        if (users == null || roles == null || permissions == null) throw BusinessException.badRequest("用户信息服务未配置");
        Map<String, Object> token = cachedToken(rawAccessToken, "ACCESS");
        if (token == null && redis == null) {
            token = mapper.selectUserInfoToken(sha256(rawAccessToken));
            if (token != null && !isTrue(token.get("revoked")) && date(token.get("expires_at")).isAfter(LocalDateTime.now())) {
                cacheToken(rawAccessToken, token, token.get("audience"));
            }
        }
        if (token == null || isTrue(token.get("revoked")) || !date(token.get("expires_at")).isAfter(LocalDateTime.now())) {
            throw BusinessException.unauthorized("访问令牌无效或已过期");
        }
        Long userId = num(token.get("user_id"));
        User user = users.selectById(userId);
        if (user == null || !"active".equals(user.getStatus()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw BusinessException.unauthorized("用户不存在或已被禁用");
        }
        return Map.of("sub", String.valueOf(userId),
                "user", Map.of("id", user.getId(), "username", user.getUsername(), "fullName", user.getFullName(), "email", user.getEmail()),
                "roles", roles.selectCodesByUserIdInPlatform(userId, Platform.CENTER_CODE),
                "permissions", permissions.permissionsOf(userId),
                "client_id", token.get("client_id"), "scope", token.get("scope"));
    }

    @Transactional
    public OAuthClientRead createClient(OAuthClientCreate req) {
        if (platforms == null) throw BusinessException.badRequest("OAuth 客户端必须绑定已登记平台");
        var platform = platforms.require(req.platformId());
        String clientType = req.clientType() == null || req.clientType().isBlank() ? "CONFIDENTIAL" : req.clientType().toUpperCase(Locale.ROOT);
        if (!Set.of("CONFIDENTIAL", "PUBLIC").contains(clientType)) throw BusinessException.badRequest("客户端类型无效");
        String clientKind = req.clientKind() == null || req.clientKind().isBlank() ? "APPLICATION" : req.clientKind().toUpperCase(Locale.ROOT);
        if (!Set.of("SERVICE", "APPLICATION").contains(clientKind)) throw BusinessException.badRequest("客户端身份类型无效");
        if (req.skipConsent() && !"APPLICATION".equals(clientKind)) throw BusinessException.badRequest("只有终端应用可以跳过同意页");
        String resource = req.resource() == null || req.resource().isBlank() ? null : req.resource().trim();
        if ("SERVICE".equals(clientKind) && (resource == null || !resource.matches("[a-zA-Z0-9._:/-]{1,128}"))) throw BusinessException.badRequest("服务客户端必须提供有效 resource");
        if (req.canIntrospectApiKeys() && (!"CONFIDENTIAL".equals(clientType) || !"SERVICE".equals(clientKind))) throw BusinessException.badRequest("只有机密服务客户端可以校验 API Key");
        if (req.redirectUris().stream().distinct().count() != req.redirectUris().size()) throw BusinessException.badRequest("redirect_uri 不能重复");
        for (String uri : req.redirectUris()) validateRedirectUri(uri);
        for (String scope : req.scopes()) if (!scope.matches("[a-zA-Z0-9:_-]{1,64}")) throw BusinessException.badRequest("scope 格式无效");
        String clientId = random(24), secret = "PUBLIC".equals(clientType) ? null : random(40);
        Map<String, Object> client = new HashMap<>();
        client.put("clientId", clientId); client.put("secretHash", secret == null ? null : sha256(secret));
        client.put("platformId", platform.getId()); client.put("clientType", clientType); client.put("clientKind", clientKind); client.put("resource", resource); client.put("name", req.name().trim());
        client.put("redirectUris", String.join("\n", req.redirectUris())); client.put("scopes", String.join(" ", req.scopes()));
        client.put("owner", currentUser.require().getId()); client.put("canIntrospect", req.canIntrospectApiKeys()); client.put("skipConsent", req.skipConsent());
        mapper.insertClient(client);
        return new OAuthClientRead(clientId, req.name().trim(), clientType, req.redirectUris(), req.scopes(), "ACTIVE", LocalDateTime.now(), secret, req.canIntrospectApiKeys(), clientKind, resource, platform.getId(), platform.getName(), req.skipConsent());
    }

    public PageResult<OAuthClientRead> listClients(int page, int pageSize, String keyword, String clientKind, String status) {
        Long owner = currentUser.require().getId();
        String kw = blankToNull(keyword); String kind = blankToNull(clientKind); String st = blankToNull(status); int offset = (page - 1) * pageSize;
        List<OAuthClientRead> items = mapper.selectOwnedClients(owner, kw, kind, st, offset, pageSize).stream().map(r -> new OAuthClientRead((String)r.get("client_id"), (String)r.get("name"), (String)r.get("client_type"), lines((String)r.get("redirect_uris")), words((String)r.get("scopes")), (String)r.get("status"), date(r.get("create_time")), null, isTrue(r.get("can_introspect_api_keys")), (String)r.get("client_kind"), (String)r.get("resource"), numOrNull(r.get("platform_id")), (String)r.get("platform_name"), isTrue(r.get("skip_consent")))).toList();
        return new PageResult<>(items, mapper.countOwnedClients(owner, kw, kind, st), page, pageSize);
    }

    @Transactional
    public OAuthClientRead updateClient(String clientId, OAuthClientUpdate req) {
        long owner = currentUser.require().getId();
        Map<String, Object> existing = mapper.selectOwnedClient(clientId, owner);
        if (existing == null) throw BusinessException.notFound("OAuth 客户端不存在");
        if (platforms == null) throw BusinessException.badRequest("OAuth 客户端必须绑定已登记平台");
        var platform = platforms.require(req.platformId());
        String resource = req.resource() == null || req.resource().isBlank() ? null : req.resource().trim();
        if ("SERVICE".equals(existing.get("client_kind")) && (resource == null || !resource.matches("[a-zA-Z0-9._:/-]{1,128}")))
            throw BusinessException.badRequest("服务客户端必须提供有效 resource");
        if (req.canIntrospectApiKeys() && (!"CONFIDENTIAL".equals(existing.get("client_type")) || !"SERVICE".equals(existing.get("client_kind"))))
            throw BusinessException.badRequest("只有机密服务客户端可以校验 API Key");
        if (req.skipConsent() && !"APPLICATION".equals(existing.get("client_kind"))) throw BusinessException.badRequest("只有终端应用可以跳过同意页");
        validateClientUrisAndScopes(req.redirectUris(), req.scopes());
        Map<String, Object> row = new HashMap<>();
        row.put("clientId", clientId); row.put("owner", owner); row.put("platformId", platform.getId()); row.put("name", req.name().trim());
        row.put("resource", resource); row.put("redirectUris", String.join("\n", req.redirectUris()));
        row.put("scopes", String.join(" ", req.scopes())); row.put("canIntrospect", req.canIntrospectApiKeys()); row.put("skipConsent", req.skipConsent());
        mapper.updateClient(row);
        existing.put("skip_consent", req.skipConsent());
        return toClientRead(existing, req.name().trim(), req.redirectUris(), req.scopes(), resource, req.canIntrospectApiKeys(), platform.getId(), platform.getName());
    }

    @Transactional
    public void updateClientStatus(String clientId, String status) {
        if (!Set.of("ACTIVE", "DISABLED").contains(status)) throw BusinessException.badRequest("客户端状态无效");
        if (mapper.updateClientStatus(clientId, currentUser.require().getId(), status) == 0) throw BusinessException.notFound("OAuth 客户端不存在");
    }

    @Transactional
    public void deleteClient(String clientId) {
        if (mapper.deleteClient(clientId, currentUser.require().getId()) == 0) throw BusinessException.notFound("OAuth 客户端不存在");
    }

    private void validateClientUrisAndScopes(List<String> redirectUris, List<String> scopes) {
        if (redirectUris.stream().distinct().count() != redirectUris.size()) throw BusinessException.badRequest("redirect_uri 不能重复");
        for (String uri : redirectUris) validateRedirectUri(uri);
        for (String scope : scopes) if (!scope.matches("[a-zA-Z0-9:_-]{1,64}")) throw BusinessException.badRequest("scope 格式无效");
    }

    private OAuthClientRead toClientRead(Map<String, Object> existing, String name, List<String> redirectUris, List<String> scopes, String resource, boolean canIntrospect, Long platformId, String platformName) {
        return new OAuthClientRead((String) existing.get("client_id"), name, (String) existing.get("client_type"), redirectUris, scopes,
                (String) existing.get("status"), date(existing.get("create_time")), null, canIntrospect,
                (String) existing.get("client_kind"), resource, platformId, platformName, isTrue(existing.get("skip_consent")));
    }

    @Transactional
    public ApiKeyRead createApiKey(ApiKeyCreate req) {
        long userId = currentUser.require().getId();
        String reason = req.reason() == null || req.reason().isBlank() ? null : req.reason().trim();
        String audience = req.audience().trim();
        if (!audience.matches("[a-zA-Z0-9._:/-]{1,128}")) throw BusinessException.badRequest("audience 格式无效");
        Map<String,Object> inserted = new HashMap<>(); inserted.put("userId", userId); inserted.put("name", req.name().trim()); inserted.put("reason", reason); inserted.put("audience", audience);
        mapper.insertApiKey(inserted);
        Long id = ((Number)inserted.get("id")).longValue();
        return new ApiKeyRead(id, req.name().trim(), null, "PENDING", LocalDateTime.now(), null, null, reason, null, null, audience);
    }

    @Transactional
    public ApiKeyRead issueApiKey(long id) {
        long userId = currentUser.require().getId();
        Map<String,Object> row = mapper.selectApiKeyForIssue(id, userId);
        if (row == null) throw BusinessException.badRequest("申请尚未通过审核或密钥已生成");
        String key = "fqk_" + random(36), prefix = key.substring(0, 12);
        if (mapper.issueApiKey(Map.of("keyHash", sha256(key), "prefix", prefix, "id", id, "userId", userId)) != 1)
            throw BusinessException.badRequest("申请状态已变化，请刷新后重试");
        return new ApiKeyRead(id, (String)row.get("name"), prefix, "ACTIVE", LocalDateTime.now(), null, key, (String)row.get("apply_reason"), null, null, (String)row.get("audience"));
    }

    public List<ApiKeyRead> listApiKeys() {
        return mapper.selectApiKeys(currentUser.require().getId()).stream().map(rs -> new ApiKeyRead(num(rs.get("id")), (String)rs.get("name"), (String)rs.get("key_prefix"), (String)rs.get("status"), date(rs.get("create_time")), nullableDate(rs.get("last_used_at")), null, (String)rs.get("apply_reason"), nullableDate(rs.get("reviewed_at")), (String)rs.get("review_comment"), (String)rs.get("audience"))).toList();
    }

    public PageResult<ApiKeyApplicationRead> listApiKeyApplications(int page, int pageSize, String keyword, String status) {
        String kw = blankToNull(keyword); String st = blankToNull(status); int offset = (page - 1) * pageSize;
        List<ApiKeyApplicationRead> items = mapper.selectApiKeyApplications(kw, st, offset, pageSize).stream().map(rs -> new ApiKeyApplicationRead(num(rs.get("id")), num(rs.get("user_id")), (String)rs.get("username"), (String)rs.get("full_name"), (String)rs.get("email"), (String)rs.get("name"), (String)rs.get("apply_reason"), (String)rs.get("key_prefix"), (String)rs.get("status"), date(rs.get("create_time")), (String)rs.get("reviewer_name"), nullableDate(rs.get("reviewed_at")), (String)rs.get("review_comment"), (String)rs.get("audience"), rs.get("validation_count") == null ? 0L : num(rs.get("validation_count")), nullableDate(rs.get("last_used_at")))).toList();
        return new PageResult<>(items, mapper.countApiKeyApplications(kw, st), page, pageSize);
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    @Transactional
    public void reviewApiKey(long id, boolean approved, String comment) {
        String status = approved ? "APPROVED" : "REJECTED";
        String reason = comment == null || comment.isBlank() ? null : comment.trim();
        if (reason != null && reason.length() > 500) throw BusinessException.badRequest("审核意见不能超过 500 个字符");
        if (!approved && reason == null) throw BusinessException.badRequest("拒绝申请时必须填写原因");
        Map<String, Object> review = new HashMap<>();
        review.put("status", status); review.put("reviewer", currentUser.require().getId());
        review.put("comment", reason); review.put("id", id);
        int updated = mapper.reviewApiKey(review);
        if (updated == 0) throw BusinessException.notFound("待审核的 API Key 申请不存在或已处理");
    }

    @Transactional
    public void revokeApiKey(long id) {
        if (mapper.revokeApiKey(id, currentUser.require().getId()) == 0) throw BusinessException.notFound("API Key 不存在或尚未生效");
    }

    @Transactional
    public void revokeApiKeyByAdmin(long id) {
        if (mapper.revokeApiKeyByAdmin(id) == 0) throw BusinessException.notFound("API Key 不存在或已失效");
    }

    @Transactional
    public ApiKeyRead rotateApiKey(long id) {
        long userId = currentUser.require().getId();
        Map<String,Object> row = mapper.selectApiKeyForRotate(id, userId);
        if (row == null) throw BusinessException.notFound("API Key 不存在或尚未生效");
        String key = "fqk_" + random(36);
        String oldHash = (String) row.get("key_hash");
        if (mapper.rotateApiKey(Map.of("id", id, "userId", userId, "oldHash", oldHash, "keyHash", sha256(key), "prefix", key.substring(0, 12))) != 1)
            throw BusinessException.conflict("API Key 状态已变化，请刷新后重试");
        return new ApiKeyRead(id, (String) row.get("name"), key.substring(0, 12), "ACTIVE", LocalDateTime.now(), null,
                key, null, null, "ROTATED", (String) row.get("audience"));
    }

    public List<UserTokenRead> listUserTokens() {
        return mapper.selectUserTokens(currentUser.require().getId()).stream().map(rs -> new UserTokenRead(num(rs.get("id")), (String)rs.get("client_id"), (String)rs.get("client_name"), (String)rs.get("token_type"), (String)rs.get("scope"), date(rs.get("expires_at")), date(rs.get("create_time")), isTrue(rs.get("revoked")))).toList();
    }

    @Transactional
    public void revokeUserToken(long id) {
        Long userId = currentUser.require().getId();
        Map<String,Object> t = mapper.selectUserToken(id, userId);
        if (t == null) throw BusinessException.notFound("令牌不存在");
        if ("REFRESH".equals(t.get("token_type"))) mapper.revokeRefreshAndChildren(id);
        else { mapper.revokeToken(id); if (t.get("parent_id") != null) mapper.revokeToken(num(t.get("parent_id"))); }
    }

    public AuthorizeContext authorizeContext(String clientId, String redirectUri, String scope, String state, String challenge, String method) {
        currentUser.require();
        validateAuthorizationRequest(clientId, redirectUri, scope, state, challenge, method);
        Map<String,Object> c = client(clientId);
        List<String> allowed = words((String)c.get("scopes"));
        List<String> requested = scope == null || scope.isBlank() ? allowed : words(scope);
        return new AuthorizeContext(clientId, (String)c.get("name"), redirectUri, state, allowed, requested,
                isTrue(c.get("skip_consent")) && "APPLICATION".equals(c.get("client_kind")));
    }

    public void validateAuthorizationRequest(String clientId, String redirectUri, String scope, String state, String challenge, String method) {
        if (state == null || state.isBlank() || state.length() > 1024) throw BusinessException.badRequest("state 必须提供且不能超过 1024 个字符");
        validatePkce(challenge, method);
        Map<String,Object> c = client(clientId);
        validateClientRedirect(c, redirectUri);
        List<String> allowed = words((String)c.get("scopes"));
        List<String> requested = scope == null || scope.isBlank() ? allowed : words(scope);
        if (requested.isEmpty() || !allowed.containsAll(requested)) throw BusinessException.badRequest("请求的 scope 不在客户端允许范围内");
    }

    @Transactional
    public String confirm(AuthorizeConfirm req) {
        Long uid = currentUser.require().getId();
        Map<String,Object> c = client(req.clientId());
        validateClientRedirect(c, req.redirectUri());
        List<String> allowed = words((String)c.get("scopes"));
        List<String> requested = words(req.requestedScope());
        if (requested.isEmpty() || !allowed.containsAll(requested) || req.scopes().isEmpty() || !requested.containsAll(req.scopes())) throw BusinessException.badRequest("授权范围无效");
        if (req.state().isBlank() || req.state().length() > 1024) throw BusinessException.badRequest("state 无效");
        if (!req.approved()) return append(req.redirectUri(), Map.of("error", "access_denied", "state", req.state()));
        validatePkce(req.codeChallenge(), req.codeChallengeMethod());
        String code = random(32);
        mapper.insertAuthorizationCode(Map.of("codeHash", sha256(code), "clientId", req.clientId(), "userId", uid,
                "scope", String.join(" ", req.scopes()), "redirectUri", req.redirectUri(), "challenge", req.codeChallenge(),
                "method", req.codeChallengeMethod(), "expiresAt", LocalDateTime.now().plusMinutes(10)));
        return append(req.redirectUri(), Map.of("code", code, "state", req.state()));
    }

    @Transactional
    public Map<String,Object> token(TokenRequest req) {
        if ("authorization_code".equals(req.grantType())) {
            if (req.code() == null || req.clientId() == null || req.redirectUri() == null || req.codeVerifier() == null) throw BusinessException.badRequest("缺少授权码参数");
            Map<String,Object> c = client(req.clientId()); validateClientRedirect(c, req.redirectUri());
            authenticateTokenClient(c, req.clientSecret());
            Map<String,Object> row = mapper.selectAuthorizationCode(sha256(req.code()), req.clientId(), req.redirectUri());
            if (row == null) throw BusinessException.badRequest("授权码无效");
            if (isTrue(row.get("used")) || !date(row.get("expires_at")).isAfter(LocalDateTime.now())) throw BusinessException.badRequest("授权码已使用或过期");
            if (req.codeVerifier().length() < 43 || req.codeVerifier().length() > 128 || !req.codeVerifier().matches("[A-Za-z0-9._~-]+")) throw BusinessException.badRequest("PKCE verifier 格式无效");
            String expected = B64.encodeToString(sha256Bytes(req.codeVerifier()));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), ((String)row.get("code_challenge")).getBytes(StandardCharsets.US_ASCII))) throw BusinessException.badRequest("PKCE 校验失败");
            if (mapper.consumeAuthorizationCode(num(row.get("id"))) != 1) throw BusinessException.badRequest("授权码已使用或过期");
            return issueTokens(req.clientId(), num(row.get("user_id")), (String)row.get("scope"));
        }
        if ("refresh_token".equals(req.grantType()) && req.refreshToken() != null && req.clientId() != null) {
            Map<String,Object> c = client(req.clientId());
            authenticateTokenClient(c, req.clientSecret());
            Map<String,Object> row = tokenRow(req.refreshToken(), "REFRESH");
            if (!req.clientId().equals(row.get("client_id"))) throw BusinessException.unauthorized("刷新令牌不属于该客户端");
            if (mapper.revokeActiveToken(num(row.get("id"))) != 1) throw BusinessException.unauthorized("刷新令牌已使用或过期");
            mapper.revokeChildren(num(row.get("id")));
            deleteUserTokenCaches(num(row.get("user_id")));
            return issueTokens((String)row.get("client_id"), num(row.get("user_id")), (String)row.get("scope"));
        }
        throw BusinessException.badRequest("不支持的 grant_type");
    }

    private Map<String,Object> issueTokens(String clientId, long userId, String scope) {
        String access = random(32), refresh = random(40); LocalDateTime now = LocalDateTime.now();
        LocalDateTime refreshExpiresAt = now.plusDays(30);
        LocalDateTime accessExpiresAt = now.plusHours(2);
        mapper.insertToken(token("REFRESH", sha256(refresh), clientId, userId, scope, refreshExpiresAt, null));
        Long refreshId = mapper.selectTokenId(sha256(refresh));
        mapper.insertToken(token("ACCESS", sha256(access), clientId, userId, scope, accessExpiresAt, refreshId));
        Object audience = tokenAudience(clientId);
        boolean refreshCached = cacheIssuedToken(refresh, "REFRESH", clientId, userId, scope, refreshExpiresAt, audience);
        boolean accessCached = cacheIssuedToken(access, "ACCESS", clientId, userId, scope, accessExpiresAt, audience);
        if (!refreshCached || !accessCached) {
            deleteUserTokenCaches(userId);
            throw new IllegalStateException("OAuth Token Redis 存储失败");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", access); response.put("token_type", "Bearer"); response.put("expires_in", 7200);
        response.put("refresh_token", refresh); response.put("refresh_expires_in", 2_592_000); response.put("scope", scope);
        return response;
    }

    private Object tokenAudience(String clientId) {
        Map<String, Object> c = mapper.selectClient(clientId);
        if (c == null) return null;
        return c.get("resource") == null || String.valueOf(c.get("resource")).isBlank() ? c.get("name") : c.get("resource");
    }

    private boolean cacheIssuedToken(String raw, String type, String clientId, long userId, String scope,
                                     LocalDateTime expiresAt, Object audience) {
        Map<String, Object> row = new HashMap<>();
        row.put("token_type", type);
        row.put("client_id", clientId);
        row.put("user_id", userId);
        row.put("scope", scope);
        row.put("expires_at", expiresAt);
        row.put("revoked", false);
        return cacheToken(raw, row, audience);
    }
    private Map<String,Object> tokenRow(String raw, String type) {
        Map<String,Object> row = cachedToken(raw, type);
        if (row == null && redis == null) {
            row = mapper.selectToken(sha256(raw), type);
            if (row != null && !isTrue(row.get("revoked")) && date(row.get("expires_at")).isAfter(LocalDateTime.now())) {
                cacheToken(raw, row, null);
            }
        }
        if (row == null || date(row.get("expires_at")).isBefore(LocalDateTime.now())) throw BusinessException.unauthorized("令牌无效或已过期");
        return row;
    }

    private Map<String,Object> cachedToken(String raw, String type) {
        if (redis == null || raw == null || raw.isBlank()) return null;
        try {
            Map<Object, Object> values = redis.opsForHash().entries(TOKEN_CACHE_PREFIX + sha256(raw));
            if (values.isEmpty() || !type.equals(String.valueOf(values.get("token_type")))) return null;
            Map<String, Object> row = new HashMap<>();
            row.put("token_type", values.get("token_type"));
            row.put("client_id", values.get("client_id"));
            row.put("user_id", Long.valueOf(String.valueOf(values.get("user_id"))));
            row.put("scope", values.get("scope"));
            row.put("expires_at", LocalDateTime.parse(String.valueOf(values.get("expires_at"))));
            row.put("revoked", Boolean.valueOf(String.valueOf(values.get("revoked"))));
            if (values.get("audience") != null) row.put("audience", values.get("audience"));
            if (isTrue(row.get("revoked")) || !date(row.get("expires_at")).isAfter(LocalDateTime.now())) {
                deleteTokenCache(raw);
                return null;
            }
            return row;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean cacheToken(String raw, Map<String,Object> row, Object audience) {
        if (redis == null) return true;
        if (raw == null || row == null) return false;
        try {
            LocalDateTime expiresAt = date(row.get("expires_at"));
            Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
            if (ttl.isNegative() || ttl.isZero()) return false;
            String key = TOKEN_CACHE_PREFIX + sha256(raw);
            Map<String, String> values = new HashMap<>();
            values.put("token_type", String.valueOf(row.getOrDefault("token_type", "ACCESS")));
            values.put("client_id", String.valueOf(row.get("client_id")));
            values.put("user_id", String.valueOf(row.get("user_id")));
            values.put("scope", String.valueOf(row.getOrDefault("scope", "")));
            values.put("expires_at", expiresAt.toString());
            values.put("revoked", String.valueOf(isTrue(row.get("revoked"))));
            if (audience != null) values.put("audience", String.valueOf(audience));
            redis.opsForHash().putAll(key, values);
            redis.expire(key, ttl);
            String userSet = TOKEN_CACHE_PREFIX + "user:" + row.get("user_id");
            redis.opsForSet().add(userSet, key);
            redis.expire(userSet, ttl);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void deleteTokenCache(String raw) {
        if (redis == null || raw == null) return;
        try { redis.delete(TOKEN_CACHE_PREFIX + sha256(raw)); } catch (RuntimeException ignored) { }
    }

    private void deleteUserTokenCaches(long userId) {
        if (redis == null) return;
        try {
            String userSet = TOKEN_CACHE_PREFIX + "user:" + userId;
            Set<String> keys = redis.opsForSet().members(userSet);
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
            redis.delete(userSet);
        } catch (RuntimeException ignored) { }
    }

    @Transactional
    public void revoke(RevokeRequest req) {
        Map<String,Object> client = client(req.clientId());
        authenticateTokenClient(client, req.clientSecret());
        Map<String,Object> token = mapper.selectTokenByHashAndClient(sha256(req.token()), req.clientId());
        if (token == null) return;
        mapper.revokeTokenAndChildren(num(token.get("id")));
        deleteUserTokenCaches(num(token.get("user_id")));
        if ("ACCESS".equals(token.get("token_type")) && token.get("parent_id") != null) {
            mapper.revokeToken(num(token.get("parent_id")));
        }
    }

    public Map<String,Object> introspect(String raw, String tokenTypeHint, String clientId, String clientSecret) {
        return introspect(raw, tokenTypeHint, clientId, clientSecret, null);
    }
    public Map<String,Object> introspect(String raw, String tokenTypeHint, String clientId, String clientSecret, String resource) {
        if ("api_key".equals(tokenTypeHint)) {
            authenticateForApiKeyIntrospection(clientId, clientSecret);
            Map<String,Object> serviceClient = client(clientId);
            if (resource == null || !resource.equals(serviceClient.get("resource"))) return Map.of("active", false);
            return introspectApiKey(raw, resource);
        }
        authenticateForProtocol(clientId, clientSecret);
        Map<String,Object> r = cachedToken(raw, "ACCESS");
        if (r == null && redis == null) {
            r = mapper.selectTokenIntrospection(sha256(raw));
            if (r != null && !isTrue(r.get("revoked")) && date(r.get("expires_at")).isAfter(LocalDateTime.now())) {
                cacheToken(raw, r, r.get("audience"));
            }
        }
        if (r == null) return Map.of("active", false);
        boolean active = !isTrue(r.get("revoked")) && date(r.get("expires_at")).isAfter(LocalDateTime.now());
        if (!active) return Map.of("active", false);
        if (resource != null && !resource.isBlank() && !resource.equals(r.get("audience"))) return Map.of("active", false);
        return Map.of("active", true, "client_id", r.get("client_id"), "aud", r.get("audience"), "sub", String.valueOf(r.get("user_id")), "scope", r.get("scope"), "exp", date(r.get("expires_at")).atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
    }

    private Map<String,Object> introspectApiKey(String raw, String resource) {
        if (raw == null || !raw.startsWith("fqk_") || raw.length() > 128) return Map.of("active", false);
        Map<String,Object> key = mapper.selectApiKeyIntrospection(sha256(raw));
        if (key == null) return Map.of("active", false);
        if (resource != null && !resource.isBlank() && key.get("audience") != null && !resource.equals(key.get("audience"))) return Map.of("active", false);
        mapper.incrementApiKeyValidationCount(num(key.get("id")));
        mapper.touchApiKey(num(key.get("id")));
        String audience = key.get("audience") == null ? "fintech-mcp-server" : String.valueOf(key.get("audience"));
        return Map.of("active", true, "sub", String.valueOf(key.get("user_id")),
                "credential_id", String.valueOf(key.get("id")), "token_type", "api_key", "aud", audience);
    }

    private void authenticateClient(String clientId, String secret) {
        authenticateClientSecret(client(clientId), secret);
    }
    private static void authenticateClientSecret(Map<String,Object> c, String secret) {
        if (secret == null || secret.isBlank() || c.get("client_secret_hash") == null) throw BusinessException.unauthorized("客户端认证失败");
        if (!MessageDigest.isEqual(sha256(secret).getBytes(StandardCharsets.US_ASCII), ((String)c.get("client_secret_hash")).getBytes(StandardCharsets.US_ASCII))) throw BusinessException.unauthorized("客户端认证失败");
    }
    public void authenticateForIntrospection(String clientId, String secret) { authenticateClient(clientId, secret); }

    public void authenticateForProtocol(String clientId, String secret) { authenticateClient(clientId, secret); }

    public void authenticateForApiKeyIntrospection(String clientId, String secret) {
        Map<String,Object> c = client(clientId);
        authenticateClientSecret(c, secret);
        if (!"CONFIDENTIAL".equals(c.get("client_type"))
                || !"SERVICE".equals(c.get("client_kind"))
                || !isTrue(c.get("can_introspect_api_keys"))) {
            throw BusinessException.forbidden("该客户端未获准校验 API Key");
        }
        if (c.get("resource") == null || ((String) c.get("resource")).isBlank()) throw BusinessException.forbidden("服务客户端未绑定 resource");
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return value != null && "true".equalsIgnoreCase(value.toString());
    }

    private static long num(Object value) {
        return ((Number) value).longValue();
    }

    private static Long numOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static LocalDateTime date(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof String string) return LocalDateTime.parse(string);
        throw new IllegalStateException("数据库时间字段类型无效");
    }

    private static LocalDateTime nullableDate(Object value) {
        return value == null ? null : date(value);
    }

    private static Map<String, Object> token(String type, String tokenHash, String clientId, long userId, String scope,
                                             LocalDateTime expiresAt, Long parentId) {
        Map<String, Object> row = new HashMap<>();
        row.put("type", type); row.put("tokenHash", tokenHash); row.put("clientId", clientId);
        row.put("userId", userId); row.put("scope", scope); row.put("expiresAt", expiresAt); row.put("parentId", parentId);
        return row;
    }

    private void authenticateTokenClient(Map<String,Object> client, String secret) {
        if ("PUBLIC".equals(client.get("client_type"))) {
            if (secret != null && !secret.isBlank()) throw BusinessException.unauthorized("公共客户端不得使用客户端密钥");
            return;
        }
        if (secret == null || secret.isBlank()) throw BusinessException.unauthorized("缺少客户端密钥");
        authenticateClient((String)client.get("client_id"), secret);
    }

    private Map<String,Object> client(String id) {
        Map<String,Object> client = mapper.selectClient(id);
        if (client == null || !"ACTIVE".equals(client.get("status"))) throw BusinessException.badRequest("OAuth 客户端不存在或已停用");
        return client;
    }
    private static void validateRedirectUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getFragment() != null || uri.getUserInfo() != null || !("https".equalsIgnoreCase(uri.getScheme()) || ("http".equalsIgnoreCase(uri.getScheme()) && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))))) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw BusinessException.badRequest("redirect_uri 必须是 HTTPS 地址（本机开发可使用 localhost HTTP）"); }
    }
    private static void validateClientRedirect(Map<String,Object> c, String redirectUri) {
        validateRedirectUri(redirectUri);
        if (!lines((String)c.get("redirect_uris")).contains(redirectUri)) throw BusinessException.badRequest("redirect_uri 与客户端登记值不匹配");
    }
    private static void validatePkce(String challenge, String method) {
        if (!"S256".equals(method) || challenge == null || !challenge.matches("[A-Za-z0-9_-]{43}")) throw BusinessException.badRequest("必须使用 PKCE S256");
    }
    private static List<String> words(String v) { return v == null || v.isBlank() ? List.of() : Arrays.stream(v.trim().split("\\s+")).distinct().toList(); }
    private static List<String> lines(String v) { return v == null || v.isBlank() ? List.of() : Arrays.asList(v.split("\\n", -1)); }
    private static String random(int size) { byte[] b = new byte[size]; RANDOM.nextBytes(b); return B64.encodeToString(b); }
    private static String sha256(String value) {
        return HexFormat.of().formatHex(sha256Bytes(value));
    }
    private static byte[] sha256Bytes(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static String append(String uri, Map<String,String> params) {
        try {
            URI parsed = URI.create(uri);
            StringJoiner j = new StringJoiner("&"); params.forEach((k,v) -> j.add(java.net.URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + java.net.URLEncoder.encode(v, StandardCharsets.UTF_8)));
            String query = parsed.getRawQuery();
            String base = parsed.getScheme() + "://" + parsed.getRawAuthority() + (parsed.getRawPath() == null ? "" : parsed.getRawPath());
            return base + "?" + (query == null || query.isEmpty() ? j : query + "&" + j);
        } catch (Exception e) { throw BusinessException.badRequest("redirect_uri 无效"); }
    }
}
