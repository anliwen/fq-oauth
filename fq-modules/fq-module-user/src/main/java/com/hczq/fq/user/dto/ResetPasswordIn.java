package com.hczq.fq.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordIn(@NotNull @Size(min = 6, max = 128) @JsonAlias("new_password") String newPassword) {
}
