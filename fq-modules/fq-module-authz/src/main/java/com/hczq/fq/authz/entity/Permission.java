package com.hczq.fq.authz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Maps t_permissions: one node of a platform's permission tree (directory / menu / button). */
@Data
public class Permission {

    public static final String DIRECTORY = "directory";
    public static final String MENU = "menu";
    public static final String BUTTON = "button";

    /** How many levels the permission tree may have; the root is level 1. */
    public static final int MAX_DEPTH = 5;

    private Long id;
    private Long platformId;
    private Long parentId;
    /** What the frontend checks to show a page or button, and what the backend annotation checks. Never changes. */
    private String code;
    private String name;
    private String type;
    private Integer sort;
    /** Referenced by code (an annotation or the frontend), so it cannot be deleted. */
    private Boolean isSystem;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
