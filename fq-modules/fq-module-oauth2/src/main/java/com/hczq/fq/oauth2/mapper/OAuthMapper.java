package com.hczq.fq.oauth2.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface OAuthMapper {
    int insertClient(Map<String, Object> row);
    List<Map<String, Object>> selectOwnedClients(@Param("owner") long owner, @Param("keyword") String keyword, @Param("clientKind") String clientKind, @Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);
    long countOwnedClients(@Param("owner") long owner, @Param("keyword") String keyword, @Param("clientKind") String clientKind, @Param("status") String status);
    Map<String, Object> selectOwnedClient(@Param("clientId") String clientId, @Param("owner") long owner);
    int updateClient(Map<String, Object> row);
    int updateClientStatus(@Param("clientId") String clientId, @Param("owner") long owner, @Param("status") String status);
    int deleteClient(@Param("clientId") String clientId, @Param("owner") long owner);
    int insertApiKey(Map<String, Object> row);
    Map<String, Object> selectApiKeyForIssue(@Param("id") long id, @Param("userId") long userId);
    int issueApiKey(Map<String, Object> row);
    List<Map<String, Object>> selectApiKeys(@Param("userId") long userId);
    List<Map<String, Object>> selectApiKeyApplications(@Param("keyword") String keyword, @Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);
    long countApiKeyApplications(@Param("keyword") String keyword, @Param("status") String status);
    int incrementApiKeyValidationCount(@Param("id") long id);
    int reviewApiKey(Map<String, Object> row);
    int revokeApiKey(@Param("id") long id, @Param("userId") long userId);
    int revokeApiKeyByAdmin(@Param("id") long id);
    Map<String,Object> selectApiKeyForRotate(@Param("id") long id, @Param("userId") long userId);
    int rotateApiKey(Map<String,Object> row);
    List<Map<String, Object>> selectUserTokens(@Param("userId") long userId);
    Map<String, Object> selectUserToken(@Param("id") long id, @Param("userId") long userId);
    int revokeRefreshAndChildren(@Param("id") long id);
    int revokeToken(@Param("id") long id);
    int revokeTokenAndChildren(@Param("id") long id);
    int insertAuthorizationCode(Map<String, Object> row);
    Map<String, Object> selectAuthorizationCode(@Param("codeHash") String codeHash, @Param("clientId") String clientId, @Param("redirectUri") String redirectUri);
    int consumeAuthorizationCode(@Param("id") long id);
    int revokeActiveToken(@Param("id") long id);
    int revokeChildren(@Param("parentId") long parentId);
    int insertToken(Map<String, Object> row);
    Long selectTokenId(@Param("tokenHash") String tokenHash);
    Map<String, Object> selectToken(@Param("tokenHash") String tokenHash, @Param("type") String type);
    Map<String, Object> selectTokenByHashAndClient(@Param("tokenHash") String tokenHash, @Param("clientId") String clientId);
    Map<String, Object> selectTokenIntrospection(@Param("tokenHash") String tokenHash);
    Map<String, Object> selectApiKeyIntrospection(@Param("keyHash") String keyHash);
    int touchApiKey(@Param("id") long id);
    Map<String, Object> selectClient(@Param("clientId") String clientId);
    Map<String, Object> selectUserInfoToken(@Param("tokenHash") String tokenHash);
}
