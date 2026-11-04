package com.hczq.fq.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OAuthClientCreate(
        @jakarta.validation.constraints.NotNull Long platformId,
        @NotBlank @Size(max = 128) String name,
        String clientType,
        List<@NotBlank @Size(max = 2048) String> redirectUris,
        @NotEmpty List<@NotBlank @Size(max = 64) String> scopes,
        boolean canIntrospectApiKeys,
        String clientKind,
        String resource,
        boolean skipConsent) {}
