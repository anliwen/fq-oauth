package com.hczq.fq.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hczq.fq.audit.dto.AuditLogRead;
import com.hczq.fq.audit.service.AuditService;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.common.api.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@SaCheckPermission("audit:view")
public class AuditLogController {
    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLogRead>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result) {
        int p = Math.min(Math.max(page, 1), 1_000_000);
        int size = Math.min(Math.max(pageSize, 1), 100);
        return ApiResponse.ok(auditService.list(p, size, keyword, action, result));
    }
}
