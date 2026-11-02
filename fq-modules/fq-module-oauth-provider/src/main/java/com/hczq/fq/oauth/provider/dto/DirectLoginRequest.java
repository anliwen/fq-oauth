package com.hczq.fq.oauth.provider.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectLoginRequest(@NotBlank String clientId, @NotBlank String clientSecret,
                                 @NotBlank String username, @NotBlank String password) {}
