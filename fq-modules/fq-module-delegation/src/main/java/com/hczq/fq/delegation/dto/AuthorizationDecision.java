package com.hczq.fq.delegation.dto;

public record AuthorizationDecision(boolean allow, String reason, Long delegationId,
                                    Long grantorUserId, Long agentId) {}
