package com.hczq.fq.delegation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record DelegationCreate(
        @NotNull Long agentId,
        @NotBlank @Size(max = 128) String resource,
        @NotEmpty @Size(max = 16) List<@NotBlank @Size(max = 64) @jakarta.validation.constraints.Pattern(regexp = "[a-zA-Z0-9:_-]+") String> actions,
        @NotBlank @Size(max = 512) String resourceScope,
        @NotNull @Future LocalDateTime expiresAt) {}
