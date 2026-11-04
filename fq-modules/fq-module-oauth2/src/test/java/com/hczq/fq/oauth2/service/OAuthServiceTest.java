package com.hczq.fq.oauth2.service;

import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.oauth2.mapper.OAuthMapper;
import com.hczq.fq.oauth2.dto.TokenRequest;
import com.hczq.fq.authz.service.PermissionService;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.mapper.RoleMapper;
import com.hczq.fq.user.mapper.UserMapper;
import com.hczq.fq.user.security.CurrentUser;
import com.hczq.fq.user.security.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OAuthServiceTest {
    private static final String CLIENT_ID = "service-client";
    private static final String CLIENT_SECRET = "confidential-service-secret";
    private static final String API_KEY = "fqk_user-api-key-value";

    private OAuthMapper mapper;
    private OAuthService service;

    @BeforeEach
    void setUp() {
        mapper = mock(OAuthMapper.class);
        service = new OAuthService(mapper, mock(CurrentUser.class));
    }

    @Test
    void activeApiKeyReturnsOwnerAndUpdatesLastUsed() throws Exception {
        stubQueries(List.of(client(true)), List.of(Map.of("id", 27L, "user_id", 103L, "audience", "fintech-mcp-server")));
        when(mapper.touchApiKey(27L)).thenReturn(1);

        Map<String, Object> result = service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(true, result.get("active"));
        assertEquals("103", result.get("sub"));
        assertEquals("27", result.get("credential_id"));
        assertEquals("api_key", result.get("token_type"));
        assertEquals("fintech-mcp-server", result.get("aud"));
        verify(mapper).touchApiKey(27L);
    }

    @Test
    void apiKeyForDifferentAudienceIsInactive() throws Exception {
        stubQueries(List.of(client(true)), List.of(Map.of("id", 27L, "user_id", 103L, "audience", "other-service")));

        Map<String, Object> result = service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(Map.of("active", false), result);
        verify(mapper, never()).touchApiKey(anyLong());
    }

    @Test
    void missingOrRevokedApiKeyReturnsInactiveWithoutTouchingUsage() throws Exception {
        stubQueries(List.of(client(true)), List.of());

        Map<String, Object> result = service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(Map.of("active", false), result);
        verify(mapper, never()).touchApiKey(anyLong());
    }

    @Test
    void clientWithoutApiKeyPermissionCannotIntrospectKeys() throws Exception {
        stubQueries(List.of(client(false)), List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server"));

        assertEquals(403, error.getStatus());
        verify(mapper, never()).selectApiKeyIntrospection(anyString());
    }

    @Test
    void applicationClientCannotIntrospectApiKeysEvenWhenPermissionIsEnabled() throws Exception {
        Map<String, Object> application = new java.util.HashMap<>(client(true));
        application.put("client_kind", "APPLICATION");
        stubQueries(List.of(application), List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server"));

        assertEquals(403, error.getStatus());
        verify(mapper, never()).selectApiKeyIntrospection(anyString());
    }

    @Test
    void serviceClientMustMatchRequestedResource() throws Exception {
        Map<String, Object> serviceClient = new java.util.HashMap<>(client(true));
        serviceClient.put("resource", "another-service");
        stubQueries(List.of(serviceClient), List.of());

        Map<String, Object> result = service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(Map.of("active", false), result);
        verify(mapper, never()).selectApiKeyIntrospection(anyString());
    }

    @Test
    void numericApiKeyPermissionFromMapperIsAccepted() throws Exception {
        stubQueries(List.of(clientWithPermissionValue(1)), List.of(Map.of("id", 27L, "user_id", 103L, "audience", "fintech-mcp-server")));
        when(mapper.touchApiKey(27L)).thenReturn(1);

        Map<String, Object> result = service.introspect(API_KEY, "api_key", CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(true, result.get("active"));
    }

    @Test
    void oauthTokenIntrospectionRemainsUnchanged() throws Exception {
        Map<String, Object> token = Map.of("client_id", CLIENT_ID, "user_id", 103L, "scope", "mcp",
                "audience", "fintech-mcp-server", "expires_at", Timestamp.valueOf(LocalDateTime.now().plusMinutes(5)), "revoked", 0);
        stubQueries(List.of(client(false)), List.of(token));

        Map<String, Object> result = service.introspect("oauth-access-token", null, CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(true, result.get("active"));
        assertEquals("103", result.get("sub"));
        assertEquals("mcp", result.get("scope"));
        assertEquals("fintech-mcp-server", result.get("aud"));
    }

    @Test
    void oauthTokenForDifferentResourceIsInactive() throws Exception {
        Map<String, Object> token = Map.of("client_id", CLIENT_ID, "user_id", 103L, "scope", "mcp",
                "audience", "another-service", "expires_at", Timestamp.valueOf(LocalDateTime.now().plusMinutes(5)), "revoked", 0);
        stubQueries(List.of(client(false)), List.of(token));

        Map<String, Object> result = service.introspect("oauth-access-token", null, CLIENT_ID, CLIENT_SECRET, "fintech-mcp-server");

        assertEquals(Map.of("active", false), result);
    }

    @Test
    void booleanAuthorizationCodeUsedFlagIsHandled() throws Exception {
        Map<String, Object> client = new java.util.HashMap<>(client(false));
        client.put("client_type", "PUBLIC");
        when(mapper.selectClient(CLIENT_ID)).thenReturn(client);
        when(mapper.selectAuthorizationCode(anyString(), eq(CLIENT_ID), eq("https://example.test/callback")))
                .thenReturn(Map.of("used", Boolean.TRUE, "expires_at", Timestamp.valueOf(LocalDateTime.now().plusMinutes(5))));

        BusinessException error = assertThrows(BusinessException.class, () -> service.token(new TokenRequest(
                "authorization_code", "code", "https://example.test/callback", CLIENT_ID,
                "verifier-verifier-verifier-verifier-verifier-verifier", null, null)));

        assertEquals("授权码已使用或过期", error.getMessage());
    }

    @Test
    void directLoginRequiresRoleOnClientPlatformAndReturnsThatPlatformsIdentity() throws Exception {
        OAuthMapper oauthMapper = mock(OAuthMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PasswordService passwordService = mock(PasswordService.class);
        Map<String, Object> application = new java.util.HashMap<>(client(false));
        application.put("client_kind", "APPLICATION");
        application.put("platform_id", 7L);
        application.put("platform_code", "third-party");
        when(oauthMapper.selectClient(CLIENT_ID)).thenReturn(application);
        when(oauthMapper.selectClientPlatform(7L)).thenReturn(Map.of("platform_id", 7L, "platform_code", "third-party"));
        User user = activeUser();
        when(userMapper.selectByUsername("alice")).thenReturn(user);
        when(passwordService.verify("password", "hash")).thenReturn(true);
        when(roleMapper.selectCodesByUserIdInPlatform(31L, "third-party")).thenReturn(List.of("operator"));
        when(permissionService.permissionsOf(31L, "third-party")).thenReturn(java.util.Set.of("report:view"));
        OAuthService directService = new OAuthService(oauthMapper, mock(CurrentUser.class), null, userMapper,
                roleMapper, permissionService, passwordService, null);

        Map<String, Object> result = directService.directLogin(CLIENT_ID, CLIENT_SECRET, "alice", "password");

        assertEquals(List.of("operator"), result.get("roles"));
        assertEquals(java.util.Set.of("report:view"), result.get("permissions"));
        assertNotNull(result.get("access_token"));
        verify(roleMapper).selectCodesByUserIdInPlatform(31L, "third-party");
        verify(userMapper).updateLastLogin(eq(31L), any());
    }

    @Test
    void directLoginRejectsUserWithoutRoleOnClientPlatformBeforeIssuingTokens() throws Exception {
        OAuthMapper oauthMapper = mock(OAuthMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PasswordService passwordService = mock(PasswordService.class);
        Map<String, Object> application = new java.util.HashMap<>(client(false));
        application.put("client_kind", "APPLICATION");
        application.put("platform_id", 7L);
        application.put("platform_code", "third-party");
        when(oauthMapper.selectClient(CLIENT_ID)).thenReturn(application);
        when(oauthMapper.selectClientPlatform(7L)).thenReturn(Map.of("platform_id", 7L, "platform_code", "third-party"));
        when(userMapper.selectByUsername("alice")).thenReturn(activeUser());
        when(passwordService.verify("password", "hash")).thenReturn(true);
        when(roleMapper.selectCodesByUserIdInPlatform(31L, "third-party")).thenReturn(List.of());
        OAuthService directService = new OAuthService(oauthMapper, mock(CurrentUser.class), null, userMapper,
                roleMapper, mock(PermissionService.class), passwordService, null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> directService.directLogin(CLIENT_ID, CLIENT_SECRET, "alice", "password"));

        assertEquals(403, error.getStatus());
        verify(userMapper, never()).updateLastLogin(eq(31L), any());
        verify(oauthMapper, never()).insertToken(any());
    }

    @Test
    void directLoginRejectsUnknownClientBeforeAnyOtherLookup() {
        OAuthMapper oauthMapper = mock(OAuthMapper.class);
        OAuthService directService = new OAuthService(oauthMapper, mock(CurrentUser.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> directService.directLogin(CLIENT_ID, CLIENT_SECRET, "alice", "password"));

        assertEquals(401, error.getStatus());
        assertEquals("客户端认证失败", error.getMessage());
        verify(oauthMapper).selectClient(CLIENT_ID);
        verifyNoMoreInteractions(oauthMapper);
    }

    private static User activeUser() {
        User user = new User();
        user.setId(31L);
        user.setUsername("alice");
        user.setHashedPassword("hash");
        user.setStatus("active");
        user.setIsActive(true);
        user.setIsDeleted(false);
        user.setFullName("Alice");
        user.setEmail("alice@example.test");
        return user;
    }

    private void stubQueries(List<Map<String, Object>> clients, List<Map<String, Object>> credentials) {
        when(mapper.selectClient(CLIENT_ID)).thenReturn(clients.isEmpty() ? null : clients.get(0));
        when(mapper.selectApiKeyIntrospection(anyString())).thenReturn(credentials.isEmpty() ? null : credentials.get(0));
        when(mapper.selectTokenIntrospection(anyString())).thenReturn(credentials.isEmpty() ? null : credentials.get(0));
    }

    private static Map<String, Object> client(boolean allowed) throws Exception {
        return clientWithPermissionValue(allowed);
    }

    private static Map<String, Object> clientWithPermissionValue(Object allowed) throws Exception {
        return Map.of("client_id", CLIENT_ID, "client_secret_hash", sha256(CLIENT_SECRET),
                "client_type", "CONFIDENTIAL", "name", "test", "redirect_uris", "https://example.test/callback",
                "scopes", "mcp", "status", "ACTIVE", "client_kind", "SERVICE", "resource", "fintech-mcp-server", "can_introspect_api_keys", allowed);
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
