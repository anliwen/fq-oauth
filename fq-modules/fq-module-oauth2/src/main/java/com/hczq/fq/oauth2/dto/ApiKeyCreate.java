package com.hczq.fq.oauth2.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ApiKeyCreate(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=500) String reason,
                           @NotBlank @Size(max=128) String audience) {}
