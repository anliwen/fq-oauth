package com.hczq.fq.delegation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DelegationRead(Long id, Long agentId, Long grantorUserId, String tenantId,
                             String resource, List<String> actions, String resourceScope,
                             String status, LocalDateTime expiresAt, LocalDateTime createTime,
                             LocalDateTime revokedAt) {}
