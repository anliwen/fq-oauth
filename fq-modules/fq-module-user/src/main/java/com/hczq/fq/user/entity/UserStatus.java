package com.hczq.fq.user.entity;

import com.hczq.fq.common.exception.BusinessException;

public enum UserStatus {
    active, disabled, locked;

    public static String validate(String status) {
        try {
            return valueOf(status).name();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw BusinessException.badRequest("无效的用户状态: " + status);
        }
    }
}
