package com.hczq.fq.federation.controller;

import com.hczq.fq.authn.dto.TokenResponse;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.federation.dto.FederatedLoginRequest;
import com.hczq.fq.federation.dto.GitHubCodeLoginRequest;
import com.hczq.fq.federation.service.GitHubLoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/federated")
public class FederatedAuthController {
    private final GitHubLoginService github;

    public FederatedAuthController(GitHubLoginService github) { this.github = github; }

    /** The access token must come from GitHub's official OAuth flow; the server verifies it against GitHub. */
    @PostMapping("/github")
    public TokenResponse github(@Valid @RequestBody FederatedLoginRequest request) {
        return github.login(request.accessToken());
    }

    @PostMapping("/github/code")
    public TokenResponse githubCode(@Valid @RequestBody GitHubCodeLoginRequest request) {
        return github.loginCode(request.code(), request.redirectUri());
    }
}
