package com.hczq.fq.authn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** OAuth2-style token response: the standard fields stay snake_case, the nested user is camelCase. */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        UserInfo user,
        List<String> permissions) {
}
