package com.hczq.fq.agent.service;

import com.hczq.fq.agent.dto.AgentCreate;
import com.hczq.fq.agent.dto.AgentRead;
import com.hczq.fq.agent.entity.Agent;
import com.hczq.fq.agent.mapper.AgentMapper;
import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.platform.service.PlatformService;
import com.hczq.fq.user.entity.User;
import com.hczq.fq.user.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AgentService {
    private final AgentMapper mapper;
    private final PlatformService platforms;
    private final CurrentUser currentUser;

    public AgentService(AgentMapper mapper, PlatformService platforms, CurrentUser currentUser) {
        this.mapper = mapper;
        this.platforms = platforms;
        this.currentUser = currentUser;
    }

    public List<AgentRead> listMine() {
        return mapper.selectByOwner(currentUser.require().getId()).stream().map(AgentService::read).toList();
    }

    @Transactional
    public AgentRead create(AgentCreate req) {
        platforms.require(req.platformId());
        if (mapper.countByPlatformAndCode(req.platformId(), req.code()) > 0) {
            throw BusinessException.conflict("该平台下Agent编码已存在");
        }
        User user = currentUser.require();
        Agent agent = new Agent();
        agent.setPlatformId(req.platformId());
        agent.setTenantId(req.tenantId());
        agent.setOwnerUserId(user.getId());
        agent.setCode(req.code());
        agent.setName(req.name());
        agent.setStatus("ACTIVE");
        mapper.insert(agent);
        return read(mapper.selectById(agent.getId()));
    }

    @Transactional
    public void suspend(Long id) { changeStatus(id, "SUSPENDED"); }

    @Transactional
    public void revoke(Long id) { changeStatus(id, "REVOKED"); }

    public Agent requireActive(Long id) {
        Agent agent = mapper.selectById(id);
        if (agent == null) throw BusinessException.notFound("Agent " + id + " 不存在");
        if (!"ACTIVE".equals(agent.getStatus())) throw BusinessException.forbidden("Agent 当前不可执行任务");
        return agent;
    }

    private void changeStatus(Long id, String status) {
        Agent agent = mapper.selectById(id);
        if (agent == null) throw BusinessException.notFound("Agent " + id + " 不存在");
        if (!agent.getOwnerUserId().equals(currentUser.require().getId())) throw BusinessException.forbidden("无权管理该Agent");
        mapper.updateStatus(id, status);
    }

    private static AgentRead read(Agent a) {
        return new AgentRead(a.getId(), a.getPlatformId(), a.getTenantId(), a.getOwnerUserId(), a.getCode(), a.getName(), a.getStatus(), a.getCreateTime(), a.getUpdateTime());
    }
}
