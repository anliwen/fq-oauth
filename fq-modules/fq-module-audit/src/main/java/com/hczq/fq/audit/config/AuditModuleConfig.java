package com.hczq.fq.audit.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hczq.fq.audit.mapper")
public class AuditModuleConfig {
}
