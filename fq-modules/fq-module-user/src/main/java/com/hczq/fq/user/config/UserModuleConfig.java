package com.hczq.fq.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hczq.fq.user.mapper")
public class UserModuleConfig {
}
