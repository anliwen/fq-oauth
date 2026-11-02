package com.hczq.fq.delegation.service;

import com.hczq.fq.agent.entity.Agent;
import com.hczq.fq.agent.service.AgentService;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.delegation.dto.*;
import com.hczq.fq.delegation.entity.AgentDelegation;
import com.hczq.fq.delegation.mapper.DelegationMapper;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DelegationService {
    private final DelegationMapper mapper;
    private final AgentService agents;
    private final CurrentUser currentUser;

    public DelegationService(DelegationMapper mapper, AgentService agents, CurrentUser currentUser) {
        this.mapper = mapper;
        this.agents = agents;
        this.currentUser = currentUser;
    }

    public List<DelegationRead> mine() {
        return mapper.selectByGrantor(currentUser.require().getId()).stream().map(DelegationService::read).toList();
    }

    @Transactional
    public DelegationRead create(DelegationCreate req) {
        Agent agent = agents.requireActive(req.agentId());
        User user = currentUser.require();
        if (!agent.getOwnerUserId().equals(user.getId())) {
            throw BusinessException.forbidden("无权为该Agent创建委托");
        }
        if (req.expiresAt().isAfter(LocalDateTime.now().plusDays(1))) {
            throw BusinessException.badRequest("单次委托有效期不能超过24小时");
        }
        AgentDelegation d = new AgentDelegation();
        d.setAgentId(agent.getId()); d.setGrantorUserId(user.getId()); d.setTenantId(agent.getTenantId());
        d.setResource(req.resource()); d.setActions(String.join(",", req.actions()));
        d.setResourceScope(req.resourceScope()); d.setExpiresAt(req.expiresAt()); d.setStatus("ACTIVE");
        mapper.insert(d);
        return read(mapper.selectById(d.getId()));
    }

    @Transactional
    public void revoke(Long id) {
        if (mapper.revoke(id, currentUser.require().getId()) == 0) {
            throw BusinessException.notFound("委托不存在或已撤销");
        }
    }

    @Transactional
    public void restore(Long id) {
        if (mapper.restore(id, currentUser.require().getId()) == 0) {
            throw BusinessException.badRequest("委托不存在、未撤销或已过期，无法恢复");
        }
    }

    public AuthorizationDecision check(Long agentId, String tenantId, AuthorizationCheck req) {
        currentUser.require();
        Agent agent = agents.requireActive(agentId);
        if (!agent.getTenantId().equals(tenantId)) return new AuthorizationDecision(false, "tenant_mismatch", null, null, agentId);
        AgentDelegation d = mapper.selectCandidates(agentId, tenantId, req.resource(), req.action(), req.resourceId())
                .stream().findFirst().orElse(null);
        if (d == null) return new AuthorizationDecision(false, "no_matching_delegation", null, null, agentId);
        return new AuthorizationDecision(true, "delegation_scope_match", d.getId(), d.getGrantorUserId(), agentId);
    }

    private static DelegationRead read(AgentDelegation d) {
        return new DelegationRead(d.getId(), d.getAgentId(), d.getGrantorUserId(), d.getTenantId(), d.getResource(),
                Arrays.asList(d.getActions().split(",")), d.getResourceScope(), d.getStatus(), d.getExpiresAt(), d.getCreateTime(), d.getRevokedAt());
    }
}
