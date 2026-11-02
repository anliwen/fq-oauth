package com.hczq.fq.authn.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordIn(
        @NotNull @Size(min = 1, max = 128) @JsonAlias("old_password") String oldPassword,
        @NotNull @Size(min = 6, max = 128) @JsonAlias("new_password") String newPassword) {
}
