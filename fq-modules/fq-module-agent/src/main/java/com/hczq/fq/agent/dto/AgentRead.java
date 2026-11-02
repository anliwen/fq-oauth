package com.hczq.fq.agent.dto;

import java.time.LocalDateTime;

public record AgentRead(Long id, Long platformId, String tenantId, Long ownerUserId,
                        String code, String name, String status,
                        LocalDateTime createTime, LocalDateTime updateTime) {}
