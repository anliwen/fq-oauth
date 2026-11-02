package com.hczq.fq.oauth2.dto;

import java.util.List;

public record AuthorizeContext(String clientId, String clientName, String redirectUri, String state,
                              List<String> scopes, List<String> requestedScopes, boolean autoApprove) {}
