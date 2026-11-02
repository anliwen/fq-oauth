package com.hczq.fq.user.dto;

import java.time.LocalDateTime;

public record RoleRead(
        Long id,
        Long platformId,
        String platformCode,
        String platformName,
        String code,
        String name,
        String desc,
        boolean isSystem,
        long userCount,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
