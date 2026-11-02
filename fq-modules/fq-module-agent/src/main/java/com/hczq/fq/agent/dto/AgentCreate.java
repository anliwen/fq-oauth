package com.hczq.fq.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentCreate(
        @NotNull Long platformId,
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name) {}
