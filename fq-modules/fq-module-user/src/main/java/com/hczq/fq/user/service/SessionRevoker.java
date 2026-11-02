package com.hczq.fq.user.service;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ends every Sa-Token session of a user. With a server-side session store this replaces alphadesk's JWT
 * {@code token_version} check: a password reset, disable or delete takes effect on the very next request.
 */
@Slf4j
@Component
public class SessionRevoker {

    public void revoke(Long userId) {
        try {
            StpUtil.logout(userId);
        } catch (Exception e) {
            log.warn("failed to revoke sessions of user {}: {}", userId, e.getMessage());
        }
    }
}
