# MCP Server 服务身份

MCP Server 使用独立的 OAuth client 表示服务自身身份，不等同于终端用户。

- `client_kind=SERVICE`：服务主体，例如 `fintech-mcp-server`。
- `client_type=CONFIDENTIAL`：服务端保存 client secret。
- `resource=fintech-mcp-server`：服务身份绑定的资源标识。
- `can_introspect_api_keys=true`：仅授予该服务调用 API Key introspection 的能力。

调用 `/oauth/introspect` 时，fq-oauth 同时校验 Basic client 凭证、`client_kind`、服务资源和请求中的 `resource`。普通 `APPLICATION` client 即使拥有 secret，也不能伪装成 MCP Server 验证用户凭证。

执行迁移 `migration_oauth_service_principal.sql` 后，在“服务客户端”中创建：

```json
{
  "name": "fintech-mcp-server",
  "clientType": "CONFIDENTIAL",
  "clientKind": "SERVICE",
  "resource": "fintech-mcp-server",
  "canIntrospectApiKeys": true,
  "redirectUris": ["https://mcp.example.com/oauth/callback"],
  "scopes": ["mcp"]
}
```

MCP 运行时只注入这个服务 client 的 `FQ_OAUTH_CLIENT_ID` 和 `FQ_OAUTH_CLIENT_SECRET`；用户 API Key 或 Bearer token 只代表调用者，不改变 MCP Server 的服务身份。
