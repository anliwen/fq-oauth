package com.hczq.fq.agent.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Agent {
    private Long id;
    private Long platformId;
    private String tenantId;
    private Long ownerUserId;
    private String code;
    private String name;
    private String status;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
