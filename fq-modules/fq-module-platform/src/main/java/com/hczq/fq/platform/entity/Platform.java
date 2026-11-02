package com.hczq.fq.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Maps t_platforms: a system that uses this centre for unified login and permission management. */
@Data
public class Platform {

    /** Code of the built-in platform that represents this centre itself. */
    public static final String CENTER_CODE = "fq-oauth";
    /** The role of this centre that always holds every permission of the centre platform. */
    public static final String CENTER_ADMIN_ROLE = "superadmin";

    private Long id;
    private String code;
    private String name;
    /** Column `desc` (reserved word in SQL, always back-quoted in statements). */
    private String desc;
    private Boolean isSystem;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
