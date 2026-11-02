package com.hczq.fq.delegation.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hczq.fq.delegation.mapper")
public class DelegationModuleConfig {}
