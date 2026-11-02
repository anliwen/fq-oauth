package com.hczq.fq.oauth2.dto;
import java.time.LocalDateTime;
public record ApiKeyRead(Long id, String name, String prefix, String status, LocalDateTime createTime, LocalDateTime lastUsedAt, String apiKey, String applyReason, LocalDateTime reviewedAt, String reviewComment, String audience) {}
