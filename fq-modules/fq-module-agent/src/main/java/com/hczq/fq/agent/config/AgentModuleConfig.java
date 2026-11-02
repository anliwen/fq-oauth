package com.hczq.fq.agent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hczq.fq.agent.mapper")
public class AgentModuleConfig {}
