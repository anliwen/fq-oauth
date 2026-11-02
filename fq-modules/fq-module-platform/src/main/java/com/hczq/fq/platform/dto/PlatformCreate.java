package com.hczq.fq.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlatformCreate(
        @NotNull @Size(min = 1, max = 32)
        @Pattern(regexp = "[a-z0-9][a-z0-9_-]*", message = "只能包含小写字母、数字、下划线和连字符")
        String code,
        @NotNull @Size(min = 1, max = 64) String name,
        @Size(max = 255) String desc) {

    public PlatformCreate {
        if (desc == null) {
            desc = "";
        }
    }
}
