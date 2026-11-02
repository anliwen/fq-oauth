package com.hczq.fq.authz.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hczq.fq.authz.mapper")
public class AuthzModuleConfig {
}
