package com.hczq.fq.oauth2.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeRequest(@NotBlank String token, @NotBlank String clientId,
                            String clientSecret) {}
