package com.hczq.fq.federation.service;

import com.hczq.fq.authn.dto.TokenResponse;
import com.hczq.fq.authn.service.AuthService;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.entity.UserStatus;
import com.hczq.fq.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

@Service
public class GitHubLoginService {
    private final UserMapper users;
    private final AuthService auth;
    private final RestClient client;
    private final String clientId;
    private final String clientSecret;

    public GitHubLoginService(UserMapper users, AuthService auth,
                              @Value("${fq.oauth.federation.github.api-base:https://api.github.com}") String apiBase,
                              @Value("${fq.oauth.federation.github.client-id:}") String clientId,
                              @Value("${fq.oauth.federation.github.client-secret:}") String clientSecret) {
        this.users = users;
        this.auth = auth;
        this.client = RestClient.builder().baseUrl(apiBase).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public TokenResponse loginCode(String code, String redirectUri) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw BusinessException.badRequest("GitHub 登录尚未配置 client_id/client_secret");
        }
        try {
            Map<String, Object> result = client.post().uri("https://github.com/login/oauth/access_token")
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .body(Map.of("client_id", clientId, "client_secret", clientSecret, "code", code, "redirect_uri", redirectUri))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            String token = result == null ? null : String.valueOf(result.get("access_token"));
            if (token == null || token.isBlank() || "null".equals(token)) throw BusinessException.unauthorized("GitHub 授权码无效或已过期");
            return login(token);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.unauthorized("GitHub 登录失败");
        }
    }

    public TokenResponse login(String providerAccessToken) {
        String email = verifiedEmail(providerAccessToken);
        User user = users.selectByEmail(email);
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw BusinessException.unauthorized("该 GitHub 邮箱未登记，请联系管理员");
        }
        if (!UserStatus.active.name().equals(user.getStatus())) {
            throw BusinessException.unauthorized("该用户已被禁用，请联系管理员");
        }
        return auth.loginUser(user);
    }

    private String verifiedEmail(String token) {
        try {
            Map<?, ?> profile = client.get().uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve().body(Map.class);
            String profileEmail = profile == null ? null : (String) profile.get("email");
            if (profileEmail != null && !profileEmail.isBlank()) return profileEmail.trim().toLowerCase();
            List<Map<String, Object>> emails = client.get().uri("/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
            if (emails != null) {
                return emails.stream()
                        .filter(e -> Boolean.TRUE.equals(e.get("verified")) && Boolean.TRUE.equals(e.get("primary")))
                        .map(e -> String.valueOf(e.get("email")).trim().toLowerCase()).findFirst()
                        .orElseThrow(() -> BusinessException.unauthorized("GitHub 未提供已验证邮箱，请联系管理员"));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.unauthorized("GitHub 身份验证失败");
        }
        throw BusinessException.unauthorized("GitHub 未提供已验证邮箱，请联系管理员");
    }
}
