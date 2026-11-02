package com.hczq.fq.user.security;

import cn.dev33.satoken.stp.StpUtil;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.entity.UserStatus;
import com.hczq.fq.user.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * The logged-in user, loaded once per request. A valid Sa-Token session is not enough: the account must still
 * exist and be active, otherwise the request is rejected with 401 (same rule as alphadesk's get_current_user).
 */
@Component
public class CurrentUser {

    private static final String ATTR = CurrentUser.class.getName();

    private final UserMapper userMapper;

    public CurrentUser(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User require() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST) instanceof User cached) {
            return cached;
        }
        StpUtil.checkLogin();
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null || !UserStatus.active.name().equals(user.getStatus())) {
            throw BusinessException.unauthorized("用户不存在或已被禁用");
        }
        if (attrs != null) {
            attrs.setAttribute(ATTR, user, RequestAttributes.SCOPE_REQUEST);
        }
        return user;
    }
}
