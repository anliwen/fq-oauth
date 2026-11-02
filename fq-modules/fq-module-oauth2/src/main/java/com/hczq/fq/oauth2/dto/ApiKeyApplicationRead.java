package com.hczq.fq.oauth2.dto;

import java.time.LocalDateTime;

public record ApiKeyApplicationRead(Long id, Long userId, String username, String fullName, String email,
                                    String name, String reason, String prefix, String status,
                                    LocalDateTime createTime, String reviewerName, LocalDateTime reviewedAt,
                                    String reviewComment, String audience, Long validationCount, LocalDateTime lastUsedAt) {}
