package com.hczq.fq.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AuthorizeConfirm(@NotBlank String clientId, @NotBlank String redirectUri,
                               @NotBlank String state, @NotBlank String codeChallenge,
                               @NotBlank String codeChallengeMethod, @NotBlank String requestedScope,
                               @NotEmpty List<String> scopes, boolean approved) {}
