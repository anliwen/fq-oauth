package com.hczq.fq.delegation.mapper;

import com.hczq.fq.delegation.entity.AgentDelegation;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface DelegationMapper {
    int insert(AgentDelegation delegation);
    List<AgentDelegation> selectByGrantor(@Param("grantorUserId") Long grantorUserId);
    AgentDelegation selectById(@Param("id") Long id);
    int revoke(@Param("id") Long id, @Param("userId") Long userId);
    int restore(@Param("id") Long id, @Param("userId") Long userId);
    List<AgentDelegation> selectCandidates(@Param("agentId") Long agentId, @Param("tenantId") String tenantId,
                                           @Param("resource") String resource, @Param("action") String action,
                                           @Param("resourceId") String resourceId);
}
