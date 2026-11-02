package com.hczq.fq.delegation.controller;

import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.delegation.dto.*;
import com.hczq.fq.delegation.service.DelegationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import java.util.List;

@RestController
@RequestMapping("/api/v1/delegations")
public class DelegationController {
    private final DelegationService service;
    public DelegationController(DelegationService service) { this.service = service; }
    @GetMapping @SaCheckPermission("delegation:view")
    public ApiResponse<List<DelegationRead>> mine() { return ApiResponse.ok(service.mine()); }
    @PostMapping @SaCheckPermission("delegation:create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DelegationRead> create(@Valid @RequestBody DelegationCreate req) { return ApiResponse.ok(service.create(req)); }
    @PostMapping("/{id}/revoke") @SaCheckPermission("delegation:delete")
    public ApiResponse<Void> revoke(@PathVariable Long id) { service.revoke(id); return ApiResponse.message("委托已撤销"); }
    @PostMapping("/{id}/restore") @SaCheckPermission("delegation:update")
    public ApiResponse<Void> restore(@PathVariable Long id) { service.restore(id); return ApiResponse.message("委托已恢复"); }
    @PostMapping("/check") @SaCheckPermission("delegation:check")
    public ApiResponse<AuthorizationDecision> check(@RequestParam Long agentId, @RequestParam String tenantId,
                                                     @Valid @RequestBody AuthorizationCheck req) {
        return ApiResponse.ok(service.check(agentId, tenantId, req));
    }
}
