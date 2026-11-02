package com.hczq.fq.delegation.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentDelegation {
    private Long id;
    private Long agentId;
    private Long grantorUserId;
    private String tenantId;
    private String resource;
    private String actions;
    private String resourceScope;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;
    private LocalDateTime revokedAt;
}
