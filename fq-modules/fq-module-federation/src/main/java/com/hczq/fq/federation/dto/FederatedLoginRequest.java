package com.hczq.fq.federation.dto;

import jakarta.validation.constraints.NotBlank;

/** Provider access token obtained from the provider's official OAuth flow. */
public record FederatedLoginRequest(@NotBlank String accessToken) {}
