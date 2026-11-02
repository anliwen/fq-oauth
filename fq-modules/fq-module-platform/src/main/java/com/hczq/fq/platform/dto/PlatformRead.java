package com.hczq.fq.platform.dto;

import java.time.LocalDateTime;

public record PlatformRead(
        Long id,
        String code,
        String name,
        String desc,
        boolean isSystem,
        long roleCount,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
