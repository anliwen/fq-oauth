package com.hczq.fq.oauth2.config;

import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;

@Configuration
@MapperScan("com.hczq.fq.oauth2.mapper")
public class OAuth2ModuleConfig {}
