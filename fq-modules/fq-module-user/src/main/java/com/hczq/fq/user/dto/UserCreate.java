package com.hczq.fq.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public record UserCreate(
        @NotNull @Size(min = 1, max = 64) String username,
        @NotNull @Email @Size(max = 128) String email,
        @Size(max = 64) @JsonAlias("full_name") String fullName,
        @Size(max = 32) @JsonAlias("employee_no") String employeeNo,
        @Size(max = 64) String title,
        @JsonAlias("dept_id") Long deptId,
        @JsonAlias("role_ids") List<Long> roleIds,
        @NotNull @Size(min = 6, max = 128) String password) {

    public UserCreate {
        if (fullName == null) {
            fullName = "";
        }
        if (roleIds == null) {
            roleIds = new ArrayList<>();
        }
    }
}
