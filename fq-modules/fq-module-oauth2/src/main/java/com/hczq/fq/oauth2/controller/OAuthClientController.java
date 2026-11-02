package com.hczq.fq.oauth2.controller;
import com.hczq.fq.common.api.ApiResponse;
import com.hczq.fq.oauth2.dto.*;
import com.hczq.fq.oauth2.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import java.util.List;
import java.util.Map;
import com.hczq.fq.common.api.PageResult;
@RestController @RequestMapping("/api/v1/oauth-clients") @SaCheckPermission("oauth-client:view")
public class OAuthClientController {
 private final OAuthService service; public OAuthClientController(OAuthService service){this.service=service;}
 @GetMapping public ApiResponse<PageResult<OAuthClientRead>> list(@RequestParam(defaultValue="1") int page, @RequestParam(name="page_size", defaultValue="10") int pageSize, @RequestParam(required=false) String keyword, @RequestParam(name="client_kind", required=false) String clientKind, @RequestParam(required=false) String status){int p=Math.max(page,1), size=Math.min(Math.max(pageSize,1),100); return ApiResponse.ok(service.listClients(p,size,keyword,clientKind,status));}
 @PostMapping @SaCheckPermission("oauth-client:create") public ApiResponse<OAuthClientRead> create(@Valid @RequestBody OAuthClientCreate req){return ApiResponse.ok(service.createClient(req));}
 @PutMapping("/{clientId}") @SaCheckPermission("oauth-client:update") public ApiResponse<OAuthClientRead> update(@PathVariable String clientId, @Valid @RequestBody OAuthClientUpdate req){return ApiResponse.ok(service.updateClient(clientId, req));}
 @PutMapping("/{clientId}/status") @SaCheckPermission("oauth-client:update") public ApiResponse<Void> status(@PathVariable String clientId, @RequestBody Map<String,String> req){service.updateClientStatus(clientId, req.get("status")); return ApiResponse.message("客户端状态已更新");}
 @DeleteMapping("/{clientId}") @SaCheckPermission("oauth-client:delete") public ApiResponse<Void> delete(@PathVariable String clientId){service.deleteClient(clientId); return ApiResponse.message("客户端已删除");}
}
