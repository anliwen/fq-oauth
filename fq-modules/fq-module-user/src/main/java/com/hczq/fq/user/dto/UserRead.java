package com.hczq.fq.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserRead(
        Long id,
        String username,
        String email,
        String fullName,
        String employeeNo,
        String title,
        Long deptId,
        String deptName,
        String status,
        boolean isActive,
        List<Long> roleIds,
        List<String> roleCodes,
        LocalDateTime lastLoginAt,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
