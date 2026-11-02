package com.hczq.fq.authn.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.hczq.fq.authn.dto.MeResponse;
import com.hczq.fq.authn.dto.TokenResponse;
import com.hczq.fq.authn.dto.UserInfo;
import com.hczq.fq.authz.service.PermissionService;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.entity.UserStatus;
import com.hczq.fq.user.mapper.UserMapper;
import com.hczq.fq.user.security.CurrentUser;
import com.hczq.fq.user.security.PasswordService;
import com.hczq.fq.platform.entity.Platform;
import com.hczq.fq.user.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AuthService {

    /** Failure counting window and thresholds: 5 per username, 20 per IP within 15 minutes. */
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(15);
    private static final int USER_FAIL_LIMIT = 5;
    private static final int IP_FAIL_LIMIT = 20;

    private final UserMapper userMapper;
    private final PasswordService passwords;
    private final RoleMapper roleMapper;
    private final StringRedisTemplate redis;
    private final CurrentUser currentUser;
    private final PermissionService permissionService;
    /** Compared against when the username does not exist, so response time does not reveal which names exist. */
    private final String dummyHash;

    public AuthService(UserMapper userMapper, PasswordService passwords, RoleMapper roleMapper,
                       StringRedisTemplate redis, CurrentUser currentUser,
                       PermissionService permissionService) {
        this.userMapper = userMapper;
        this.passwords = passwords;
        this.roleMapper = roleMapper;
        this.redis = redis;
        this.currentUser = currentUser;
        this.permissionService = permissionService;
        this.dummyHash = passwords.hash("not-a-real-password-0");
    }

    public TokenResponse login(String username, String password, String clientIp) {
        if (isLocked(username, clientIp)) {
            // same message as a wrong password: do not tell the caller they are locked
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        User user;
        try {
            user = authenticate(username, password);
        } catch (BusinessException e) {
            recordFailure(username, clientIp);
            throw e;
        }

        clearFailures(username);
        return loginUser(user);
    }

    /** Completes a federated login after the provider identity has been verified by its adapter. */
    public TokenResponse loginUser(User user) {
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted()) || !UserStatus.active.name().equals(user.getStatus())) {
            throw BusinessException.unauthorized("账号已被禁用或不存在");
        }
        userMapper.updateLastLogin(user.getId(), LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        StpUtil.login(user.getId());
        return new TokenResponse(StpUtil.getTokenValue(), "bearer", SaManager.getConfig().getTimeout(),
                userInfo(user), permissionList(user));
    }

    public void logout() {
        StpUtil.logout();
    }

    public MeResponse me() {
        User user = currentUser.require();
        return new MeResponse(userInfo(user), permissionList(user));
    }

    /** Verifies the old password, sets the new one (token_version + 1) and ends all of the user's sessions. */
    public void changePassword(String oldPassword, String newPassword) {
        User user = currentUser.require();
        if (!passwords.verify(oldPassword, user.getHashedPassword())) {
            throw BusinessException.badRequest("旧密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw BusinessException.badRequest("新密码不能与旧密码相同");
        }
        PasswordService.validateStrength(newPassword);
        userMapper.updatePassword(user.getId(), passwords.hash(newPassword));
        StpUtil.logout(user.getId());
    }

    private User authenticate(String username, String password) {
        User user = userMapper.selectByUsername(username);
        boolean ok = passwords.verify(password, user != null ? user.getHashedPassword() : dummyHash)
                && user != null && !Boolean.TRUE.equals(user.getIsDeleted());
        if (!ok) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (!UserStatus.active.name().equals(user.getStatus())) {
            throw BusinessException.unauthorized("账号已被禁用或锁定");
        }
        return user;
    }

    /** The permission codes of this centre the user holds; the frontend shows pages and buttons from them. */
    private List<String> permissionList(User user) {
        return List.copyOf(permissionService.permissionsOf(user.getId()));
    }

    private UserInfo userInfo(User user) {
        // Only this centre's own roles: the frontend decides what to show from them, and a role with the same code
        // on a connected platform must not look like a role here.
        return new UserInfo(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), null,
                roleMapper.selectCodesByUserIdInPlatform(user.getId(), Platform.CENTER_CODE));
    }

    // ---- login throttling (Redis; unavailable Redis never blocks a login) ----

    private boolean isLocked(String username, String ip) {
        try {
            List<String> keys = new ArrayList<>(List.of("auth:lock:u:" + username));
            if (ip != null) {
                keys.add("auth:lock:ip:" + ip);
            }
            Long n = redis.countExistingKeys(keys);
            return n > 0;
        } catch (Exception e) {
            log.warn("login lock check failed (Redis unavailable?): {}", e.getMessage());
            return false;
        }
    }

    private void recordFailure(String username, String ip) {
        try {
            bump("auth:fail:u:" + username, "auth:lock:u:" + username, USER_FAIL_LIMIT);
            if (ip != null) {
                bump("auth:fail:ip:" + ip, "auth:lock:ip:" + ip, IP_FAIL_LIMIT);
            }
        } catch (Exception e) {
            log.warn("login failure counting failed (Redis unavailable?): {}", e.getMessage());
        }
    }

    private void bump(String failKey, String lockKey, int limit) {
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1) {
            redis.expire(failKey, FAIL_WINDOW);
        }
        if (count != null && count >= limit) {
            redis.opsForValue().set(lockKey, "1", FAIL_WINDOW);
        }
    }

    /** Only the username counters are cleared: IP counters keep expiring on their own. */
    private void clearFailures(String username) {
        try {
            redis.delete(List.of("auth:fail:u:" + username, "auth:lock:u:" + username));
        } catch (Exception e) {
            log.warn("login failure cleanup failed (Redis unavailable?): {}", e.getMessage());
        }
    }
}
