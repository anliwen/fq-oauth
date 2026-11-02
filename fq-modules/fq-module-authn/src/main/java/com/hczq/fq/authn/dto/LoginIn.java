package com.hczq.fq.authn.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginIn(
        @NotNull @Size(min = 1, max = 64) String username,
        @NotNull @Size(min = 1, max = 128) String password,
        @Size(max = 65536) @JsonAlias("captcha_verify_param") String captchaVerifyParam) {
}
