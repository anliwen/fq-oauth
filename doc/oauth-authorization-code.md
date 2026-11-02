# OAuth 授权码接入

本流程用于 Workbuddy 等 MCP 服务通过浏览器申请用户授权。服务端保存访问令牌；浏览器只在 FQ OAuth 登录/同意页面交互，不接触客户端密钥。

## 管理端配置

在「服务客户端」创建客户端，登记 MCP 服务名称、精确回调地址和允许的 scope。

- 公共客户端：适用于无法安全保存密钥的原生应用或纯浏览器客户端，必须使用 PKCE S256。
- 机密客户端：适用于有后端的 MCP 服务，密钥只保存在服务端；授权码仍使用 PKCE。

现有数据库执行一次 `doc/sql/migration_oauth_public_clients.sql`；新库使用 `doc/sql/init.sql`。

## 浏览器授权

MCP 服务端为每次授权生成不可预测的 `state` 和 PKCE `code_verifier`，用 SHA-256 计算 Base64URL（去掉 `=`）得到 `code_challenge`。将 `state`、`code_verifier` 与当前服务端会话绑定，不能只依赖浏览器提交值。

将浏览器重定向至以下 URL。参数必须 URL 编码，`redirect_uri` 必须与管理端登记值完全一致：

```text
GET https://<fq-oauth-host>/oauth/authorize
  ?response_type=code
  &client_id=<client_id>
  &redirect_uri=<registered_callback>
  &scope=mcp
  &state=<one-time-random-value>
  &code_challenge=<base64url-sha256-verifier>
  &code_challenge_method=S256
```

用户未登录时先登录；登录成功后返回授权同意页。用户同意后，FQ OAuth 将浏览器重定向回登记的回调地址，并附带短时、一次性的 `code` 和原样 `state`。拒绝时返回 `error=access_denied` 和 `state`。回调必须先校验 state，再处理 code/error。

## 服务端换取令牌

MCP 服务端收到回调后使用表单编码请求：

```http
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&client_id=<client_id>&code=<code>&redirect_uri=<same-registered-callback>&code_verifier=<original-verifier>
```

机密客户端还需通过 `Authorization: Basic base64(client_id:client_secret)` 认证，或在表单中提交 `client_secret`。公共客户端不发送密钥。成功响应包含 `access_token`、`token_type`、`expires_in`、`refresh_token` 和 `scope`。授权码在 10 分钟后失效且只能兑换一次；访问令牌有效 2 小时，刷新令牌有效 30 天并在刷新时轮换。

```http
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&client_id=<client_id>&refresh_token=<refresh_token>
```

机密客户端刷新时同样需要客户端认证。刷新令牌只能由原客户端使用。令牌撤销和 introspection 分别使用 `/oauth/revoke`、`/oauth/introspect`，并要求客户端认证。

## FQ OAuth 端点

- 授权：`GET /oauth/authorize`
- 同意上下文：`GET /api/v1/oauth/authorize`（仅供已登录同意页使用）
- 换取/刷新令牌：`POST /oauth/token`
- 撤销：`POST /oauth/revoke`
- 查询令牌状态：`POST /oauth/introspect`
- 服务发现：`GET /.well-known/oauth-authorization-server`

## 服务端 API Key 内省

可信的 MCP/API 服务可以使用同一个内省端点校验用户 API Key。创建 OAuth 客户端时选择“机密客户端”，并显式启用“允许校验用户 API Key”。公共客户端不能获得此能力。

服务端必须使用 HTTP Basic 传递 `client_id:client_secret`，并把用户提交的完整 API Key 放在请求体中：

```http
POST /oauth/introspect
Authorization: Basic base64(<client_id>:<client_secret>)
Content-Type: application/x-www-form-urlencoded
Cache-Control: no-store

token=fqk_xxx&token_type_hint=api_key
```

有效响应示例：

```json
{
  "active": true,
  "sub": "用户ID",
  "credential_id": "API Key记录ID",
  "token_type": "api_key"
}
```

API Key 不存在、未审核、未生成、已撤销、所属用户被禁用或格式明显无效时统一返回 `{"active":false}`。校验成功会节流更新 `last_used_at`，不会返回 API Key 原文。服务端应将无效凭证返回 401，将有效但未通过工具资源策略的请求返回 403；FQ OAuth 不会仅凭 API Key 自动允许所有工具。

该内省接口只返回身份和凭证状态，不返回工具权限。MCP 服务仍需按自己的资源、动作、对象范围进行授权，或者进一步调用委托/策略服务。生产环境必须使用 HTTPS、禁止记录完整 Key，并在 FQ OAuth 不可用时失败关闭。

生产部署时应通过 HTTPS 提供所有端点和回调。开发环境只允许 `localhost`/`127.0.0.1` 使用 HTTP 回调。
