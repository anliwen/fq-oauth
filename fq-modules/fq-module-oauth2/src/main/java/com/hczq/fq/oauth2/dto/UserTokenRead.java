package com.hczq.fq.oauth2.dto;
import java.time.LocalDateTime;
public record UserTokenRead(Long id, String clientId, String clientName, String tokenType, String scope, LocalDateTime expiresAt, LocalDateTime createTime, boolean revoked) {}
