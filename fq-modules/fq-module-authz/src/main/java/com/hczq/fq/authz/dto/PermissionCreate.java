package com.hczq.fq.authz.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The platform, parent, code and type are chosen here and never change afterwards. */
public record PermissionCreate(
        @NotNull @JsonAlias("platform_id") Long platformId,
        @JsonAlias("parent_id") Long parentId,
        @NotNull @Size(min = 1, max = 64)
        @Pattern(regexp = "[a-z0-9][a-z0-9:_-]*", message = "只能包含小写字母、数字、冒号、下划线和连字符")
        String code,
        @NotNull @Size(min = 1, max = 64) String name,
        @NotNull @Pattern(regexp = "directory|menu|button", message = "类型只能是 directory、menu 或 button") String type,
        Integer sort) {

    public PermissionCreate {
        if (sort == null) {
            sort = 0;
        }
    }
}
