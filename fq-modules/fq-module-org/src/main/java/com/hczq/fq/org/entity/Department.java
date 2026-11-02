package com.hczq.fq.org.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Maps t_departments: a node of the organisation tree. Soft-deleted via is_deleted. */
@Data
public class Department {
    private Long id;
    private String name;
    private Long parentId;
    private String leader;
    private Integer sort;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
