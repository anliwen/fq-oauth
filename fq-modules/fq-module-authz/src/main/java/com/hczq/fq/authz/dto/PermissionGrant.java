package com.hczq.fq.authz.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PermissionGrant(@NotNull @JsonAlias("permission_ids") List<Long> permissionIds) {
}
