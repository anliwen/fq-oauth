package com.hczq.fq.authn.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.hczq.fq.web.GlobalExceptionHandler.ErrorBody;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token owns authentication and authorization for /api/**.
 */
@Configuration
public class AuthnWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Also enables @SaCheckPermission on controllers.
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/auth/token", "/api/v1/auth/captcha-config", "/api/v1/auth/federated/**");
    }

    /** Must outrank the catch-all Exception handler in fq-starter-web, otherwise a missing token becomes a 500. */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RestControllerAdvice
    public static class AuthExceptionHandler {

        @ExceptionHandler(NotLoginException.class)
        public ResponseEntity<ErrorBody> notLogin(NotLoginException e) {
            return ResponseEntity.status(401)
                    .body(new ErrorBody("UNAUTHORIZED", "认证凭证无效或已过期", null, null));
        }

        @ExceptionHandler(NotPermissionException.class)
        public ResponseEntity<ErrorBody> noPermission(NotPermissionException e) {
            return ResponseEntity.status(403)
                    .body(new ErrorBody("FORBIDDEN", "缺少权限: " + e.getPermission(), null, null));
        }

        @ExceptionHandler(NotRoleException.class)
        public ResponseEntity<ErrorBody> noRole(NotRoleException e) {
            return ResponseEntity.status(403)
                    .body(new ErrorBody("FORBIDDEN", "无权限访问", null, null));
        }
    }
}
