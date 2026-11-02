package com.hczq.fq.audit.service;

import com.hczq.fq.audit.dto.AuditLogRead;
import com.hczq.fq.audit.entity.AuditLog;
import com.hczq.fq.audit.mapper.AuditLogMapper;
import com.hczq.fq.common.api.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AuditService {
    private final AuditLogMapper mapper;

    public AuditService(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    /** Audit failure must never turn a successful business request into a 500. */
    public void record(AuditLog entry) {
        try {
            mapper.insert(entry);
        } catch (RuntimeException e) {
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }

    public PageResult<AuditLogRead> list(int page, int pageSize, String keyword, String action, String result) {
        String kw = blankToNull(keyword);
        String act = blankToNull(action);
        String rs = blankToNull(result);
        int offset = (page - 1) * pageSize;
        List<AuditLogRead> items = mapper.selectPage(kw, act, rs, offset, pageSize).stream()
                .map(row -> new AuditLogRead(row.getId(), row.getOperatorId(),
                        row.getOperatorName() == null ? "匿名用户" : row.getOperatorName(),
                        row.getOperatorUsername(), row.getAction(), row.getMethod(), row.getPath(),
                        row.getTarget(), row.getResult(), row.getStatusCode(), row.getIp(),
                        row.getUserAgent(), row.getDetail(), row.getCreateTime()))
                .toList();
        return new PageResult<>(items, mapper.countPage(kw, act, rs), page, pageSize);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
