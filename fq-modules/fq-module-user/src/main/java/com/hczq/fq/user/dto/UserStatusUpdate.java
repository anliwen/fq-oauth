package com.hczq.fq.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdate(@NotNull String status) {
}
