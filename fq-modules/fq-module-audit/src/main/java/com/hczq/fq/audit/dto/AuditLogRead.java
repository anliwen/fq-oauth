package com.hczq.fq.audit.dto;

import java.time.LocalDateTime;

public record AuditLogRead(
        Long id,
        Long operatorId,
        String operatorName,
        String operatorUsername,
        String action,
        String method,
        String path,
        String target,
        String result,
        Integer statusCode,
        String ip,
        String userAgent,
        String detail,
        LocalDateTime createTime) {
}
