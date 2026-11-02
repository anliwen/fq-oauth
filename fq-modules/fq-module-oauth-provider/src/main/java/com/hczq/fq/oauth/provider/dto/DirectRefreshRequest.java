package com.hczq.fq.oauth.provider.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectRefreshRequest(@NotBlank String clientId, @NotBlank String clientSecret,
                                   @NotBlank String refreshToken) {}
