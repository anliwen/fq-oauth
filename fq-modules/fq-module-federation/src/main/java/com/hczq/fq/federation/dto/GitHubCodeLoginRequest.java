package com.hczq.fq.federation.dto;

import jakarta.validation.constraints.NotBlank;

public record GitHubCodeLoginRequest(@NotBlank String code, @NotBlank String redirectUri) {}
