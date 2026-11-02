package com.hczq.fq.audit.mapper;

import com.hczq.fq.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditLogMapper {
    int insert(AuditLog log);

    List<AuditLog> selectPage(@Param("keyword") String keyword,
                              @Param("action") String action,
                              @Param("result") String result,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword,
                   @Param("action") String action,
                   @Param("result") String result);
}
