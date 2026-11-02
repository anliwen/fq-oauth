package com.hczq.fq.audit.web;

import cn.dev33.satoken.stp.StpUtil;
import com.hczq.fq.audit.entity.AuditLog;
import com.hczq.fq.audit.service.AuditService;
import com.hczq.fq.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/** Records every API write request so new write endpoints are covered automatically. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditWriteFilter extends OncePerRequestFilter {
    private final AuditService auditService;

    public AuditWriteFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isAuditable(request)) {
            chain.doFilter(request, response);
            return;
        }
        Long actorBefore = currentUserId();
        boolean failed = false;
        int failureStatus = 0;
        try {
            chain.doFilter(request, response);
        } catch (BusinessException e) {
            failed = true;
            failureStatus = e.getStatus();
            throw e;
        } catch (ServletException | IOException | RuntimeException | Error e) {
            failed = true;
            throw e;
        } finally {
            writeEntry(request, response, failed, failureStatus, actorBefore);
        }
    }

    private void writeEntry(HttpServletRequest request, HttpServletResponse response, boolean failed, int failureStatus, Long actorBefore) {
        String path = request.getRequestURI();
        int status = failureStatus > 0 ? failureStatus : response.getStatus();
        if (failed && status < 400) status = 500;
        boolean success = !failed && status < 400;
        AuditLog entry = new AuditLog();
        entry.setOperatorId(actorBefore != null ? actorBefore : currentUserId());
        entry.setAction(action(request.getMethod(), path));
        entry.setMethod(request.getMethod());
        entry.setPath(path);
        entry.setTarget(target(path));
        entry.setResult(success ? "SUCCESS" : "FAILURE");
        entry.setStatusCode(status);
        entry.setIp(clientIp(request));
        entry.setUserAgent(limit(request.getHeader("User-Agent"), 512));
        // Request bodies and query strings are deliberately excluded: they may contain passwords, tokens or API keys.
        entry.setDetail(null);
        auditService.record(entry);
    }

    private boolean isAuditable(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        return (request.getRequestURI().startsWith("/api/v1/") || request.getRequestURI().startsWith("/oauth/"))
                && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method));
    }

    private Long currentUserId() {
        try {
            if (!StpUtil.isLogin()) return null;
            return StpUtil.getLoginIdAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return limit(forwarded.split(",")[0].trim(), 64);
        return limit(request.getRemoteAddr(), 64);
    }

    private String target(String path) {
        String[] parts = path.split("/");
        return parts.length > 3 ? limit(parts[3] + (parts.length > 4 ? "/…" : ""), 128) : path;
    }

    private String action(String method, String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith("/auth/token")) return "登录";
        if (p.endsWith("/auth/logout")) return "退出登录";
        if (p.contains("api-key-applications") && p.endsWith("/review")) return "API Key 审批";
        if (p.contains("api-key-applications") && p.endsWith("/revoke")) return "API Key 禁用";
        if (p.endsWith("/me/api-keys")) return "API Key 申请";
        if (p.contains("/api-keys/") && p.endsWith("/issue")) return "API Key 生成";
        if (p.contains("/api-keys/") && p.endsWith("/rotate")) return "API Key 轮换";
        if (p.contains("/api-keys/") && p.endsWith("/revoke")) return "API Key 禁用";
        if (p.contains("oauth-clients")) return "服务客户端维护";
        return method + " 写操作";
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
