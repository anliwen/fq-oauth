package com.hczq.fq.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Maps t_users. Soft-deleted via is_deleted. */
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String employeeNo;
    private String title;
    private String hashedPassword;
    /** The department the user belongs to, or null. */
    private Long deptId;
    /** Read-only, looked up from t_departments. */
    private String deptName;
    /** active / disabled / locked */
    private String status;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private Integer tokenVersion;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
