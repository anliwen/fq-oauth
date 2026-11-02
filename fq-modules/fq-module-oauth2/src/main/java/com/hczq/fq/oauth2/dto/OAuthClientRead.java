package com.hczq.fq.oauth2.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OAuthClientRead(String clientId, String name, String clientType, List<String> redirectUris, List<String> scopes,
                              String status, LocalDateTime createTime, String clientSecret,
                              boolean canIntrospectApiKeys, String clientKind, String resource,
                              Long platformId, String platformName, boolean skipConsent) {}
