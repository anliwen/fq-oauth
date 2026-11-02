package com.hczq.fq.platform.dto;

import jakarta.validation.constraints.Size;

/** Both fields optional; a missing (null) field is left unchanged. The code can never change. */
public record PlatformUpdate(@Size(min = 1, max = 64) String name, @Size(max = 255) String desc) {
}
