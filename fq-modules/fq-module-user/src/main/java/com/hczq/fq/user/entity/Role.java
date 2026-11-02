package com.hczq.fq.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Maps t_roles. Soft-deleted via is_deleted. Every role belongs to exactly one platform and never moves.
 * The data_scope column is left at its default and not used.
 */
@Data
public class Role {
    private Long id;
    private Long platformId;
    /** Read-only, joined from t_platforms. */
    private String platformCode;
    /** Read-only, joined from t_platforms. */
    private String platformName;
    /** Unique within its platform, not globally. */
    private String code;
    private String name;
    /** Column `desc` (reserved word in SQL, always back-quoted in statements). */
    private String desc;
    private Boolean isSystem;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
