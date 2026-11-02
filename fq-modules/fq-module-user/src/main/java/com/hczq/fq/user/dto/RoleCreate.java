package com.hczq.fq.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The platform is chosen here and can never be changed afterwards. */
public record RoleCreate(
        @NotNull @JsonAlias("platform_id") Long platformId,
        @NotNull @Size(min = 1, max = 32) String code,
        @NotNull @Size(min = 1, max = 64) String name,
        @Size(max = 255) String desc) {

    public RoleCreate {
        if (desc == null) {
            desc = "";
        }
    }
}
