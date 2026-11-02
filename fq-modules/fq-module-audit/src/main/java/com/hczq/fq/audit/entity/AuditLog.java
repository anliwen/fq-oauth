package com.hczq.fq.audit.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLog {
    private Long id;
    private Long operatorId;
    private String action;
    private String method;
    private String path;
    private String target;
    private String result;
    private Integer statusCode;
    private String ip;
    private String userAgent;
    private String detail;
    private LocalDateTime createTime;
    private String operatorName;
    private String operatorUsername;
}
