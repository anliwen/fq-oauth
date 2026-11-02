package com.hczq.fq.agent.mapper;

import com.hczq.fq.agent.entity.Agent;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface AgentMapper {
    int insert(Agent agent);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    Agent selectById(@Param("id") Long id);
    List<Agent> selectByOwner(@Param("ownerUserId") Long ownerUserId);
    long countByPlatformAndCode(@Param("platformId") Long platformId, @Param("code") String code);
}
