package com.hczq.fq.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hczq.fq.platform.mapper")
public class PlatformModuleConfig {
}
