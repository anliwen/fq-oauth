package com.hczq.fq.authz.dto;

import jakarta.validation.constraints.Size;

/** Only the name and the order can change; a missing (null) field is left unchanged. */
public record PermissionUpdate(@Size(min = 1, max = 64) String name, Integer sort) {
}
