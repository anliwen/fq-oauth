package com.hczq.fq.authz.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/** A missing (null) field is left unchanged. */
public record PermissionUpdate(
        @Size(min = 1, max = 64)
        @Pattern(regexp = "[a-z0-9][a-z0-9:_-]*", message = "只能包含小写字母、数字、冒号、下划线和连字符")
        String code,
        @Pattern(regexp = "directory|menu|button", message = "类型只能是 directory、menu 或 button")
        String type,
        @Size(min = 1, max = 64) String name,
        Integer sort) {
}
