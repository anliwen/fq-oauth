package com.hczq.fq.delegation.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorizationCheck(@NotBlank String resource, @NotBlank String action,
                                 @NotBlank String resourceId) {}
