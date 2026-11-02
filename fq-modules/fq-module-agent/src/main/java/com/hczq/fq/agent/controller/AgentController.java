package com.hczq.fq.agent.controller;

import com.hczq.fq.agent.dto.AgentCreate;
import com.hczq.fq.agent.dto.AgentRead;
import com.hczq.fq.agent.service.AgentService;
import com.hczq.fq.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {
    private final AgentService service;
    public AgentController(AgentService service) { this.service = service; }
    @GetMapping @SaCheckPermission("agent:view")
    public ApiResponse<List<AgentRead>> mine() { return ApiResponse.ok(service.listMine()); }
    @PostMapping @SaCheckPermission("agent:create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgentRead> create(@Valid @RequestBody AgentCreate req) { return ApiResponse.ok(service.create(req)); }
    @PostMapping("/{id}/suspend") @SaCheckPermission("agent:update")
    public ApiResponse<Void> suspend(@PathVariable Long id) { service.suspend(id); return ApiResponse.message("Agent已暂停"); }
    @PostMapping("/{id}/revoke") @SaCheckPermission("agent:delete")
    public ApiResponse<Void> revoke(@PathVariable Long id) { service.revoke(id); return ApiResponse.message("Agent已撤销"); }
}
