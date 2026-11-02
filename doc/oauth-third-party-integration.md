# FQ OAuth 第三方直接接口接入指南

本文只介绍第三方通过账号密码调用接口登录的接入方式。整个流程由第三方后端调用 FQ OAuth，不需要浏览器跳转或回调处理。

## 1. 接入流程

```text
管理员登记客户端
    -> 第三方后端调用登录接口
    -> FQ OAuth 返回 access_token、refresh_token 和用户信息
    -> 第三方创建自己的登录会话
    -> 业务期间查询 Token 状态或续约
    -> 用户退出时调用登出接口并清除本地会话
```

第三方只需要实现以下四个接口：

| 接口 | 用途 |
| --- | --- |
| `POST /oauth/direct/token` | 使用账号密码登录并获取 Token |
| `POST /oauth/direct/introspect` | 查询 Access Token 是否有效及用户信息 |
| `POST /oauth/direct/refresh` | 使用 Refresh Token 换取新的 Token |
| `POST /oauth/direct/logout` | 撤销 Token 并退出登录 |

下面的接口地址均以 FQ OAuth 服务地址为前缀，例如：

```text
https://oauth.example.com/oauth/direct/token
```

## 2. 管理员登记客户端

管理员进入 FQ OAuth 的“服务客户端”页面，创建一个用于第三方系统的客户端，填写：

| 配置项 | 直接接口接入要求 |
| --- | --- |
| 服务平台 | 选择已登记的第三方平台 |
| 服务名称 | 填写第三方系统名称 |
| 主体类型 | 选择 `APPLICATION`（终端应用） |
| 客户端类型 | 推荐选择 `CONFIDENTIAL`（机密客户端） |
| 权限范围 | 填写该系统需要的 scope，例如 `openid profile` |
| 客户端状态 | 必须为启用 |

当前客户端登记表要求回调地址字段不能为空，但直接接口运行时不会使用该地址，也不会触发回调。请按平台校验规则填写该字段，不能填写通配符；生产环境使用 HTTPS。

不要选择 `SERVICE`。`SERVICE` 是 MCP Server 等服务自身调用身份，不能代替终端用户进行账号密码登录。

创建成功后平台会生成：

```text
client_id       客户端公开标识，表示哪个系统在调用
client_secret   客户端密钥，用于证明调用方是该系统
```

`client_secret` 只在创建时完整显示一次。管理员必须将它交给第三方后端，通过环境变量或密钥管理服务注入：

```text
FQ_OAUTH_BASE_URL=https://oauth.example.com
FQ_OAUTH_CLIENT_ID=平台生成的客户端 ID
FQ_OAUTH_CLIENT_SECRET=平台生成的客户端密钥
```

禁止将 `client_secret` 写入前端代码、浏览器 LocalStorage、移动端安装包、日志或 Git。

## 3. 账号密码登录

第三方自己的登录页收集账号和密码后，由第三方后端调用：

```http
POST /oauth/direct/token
Content-Type: application/json
```

请求体：

```json
{
  "clientId": "YOUR_APPLICATION_CLIENT_ID",
  "clientSecret": "YOUR_APPLICATION_CLIENT_SECRET",
  "username": "anliwen",
  "password": "用户密码"
}
```

curl 示例：

```bash
curl -X POST "$FQ_OAUTH_BASE_URL/oauth/direct/token" \
  -H 'Content-Type: application/json' \
  -H 'Cache-Control: no-store' \
  -d '{
    "clientId": "YOUR_APPLICATION_CLIENT_ID",
    "clientSecret": "YOUR_APPLICATION_CLIENT_SECRET",
    "username": "anliwen",
    "password": "用户密码"
  }'
```

成功响应：

```json
{
  "access_token": "ACCESS_TOKEN",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "REFRESH_TOKEN",
  "refresh_expires_in": 2592000,
  "scope": "openid profile",
  "user": {
    "id": 11,
    "username": "anliwen",
    "fullName": "安利文",
    "email": "anliwen@example.com"
  },
  "roles": ["client"],
  "permissions": ["client:home", "credential:view"],
  "client_id": "YOUR_APPLICATION_CLIENT_ID"
}
```

字段说明：

- `access_token`：访问令牌，有效期 2 小时。
- `refresh_token`：续约令牌，有效期 30 天，只能由第三方后端保存。
- `user`：用户基本信息。
- `roles`：用户所属角色编码。
- `permissions`：用户拥有的权限编码。
- `scope`：客户端登记并允许使用的权限范围。

登录成功后，第三方应创建自己的 Session 或 Cookie。建议前端只持有第三方自己的会话标识，不直接持有 `client_secret` 和 `refresh_token`。

用户名不存在、密码错误、用户被禁用或客户端认证失败时返回 `401`。错误响应示例：

```json
{
  "error": "invalid_client",
  "error_description": "用户名或密码错误"
}
```

不要根据错误文本区分“用户不存在”和“密码错误”。

## 4. 查询 Token 状态

第三方后端需要确认 Token 是否仍有效时调用：

```http
POST /oauth/direct/introspect
Content-Type: application/json
```

请求体：

```json
{
  "clientId": "YOUR_APPLICATION_CLIENT_ID",
  "clientSecret": "YOUR_APPLICATION_CLIENT_SECRET",
  "token": "ACCESS_TOKEN"
}
```

成功且 Token 有效时返回：

```json
{
  "active": true,
  "client_id": "YOUR_APPLICATION_CLIENT_ID",
  "sub": "11",
  "scope": "openid profile",
  "exp": 1790000000,
  "user": {
    "id": 11,
    "username": "anliwen",
    "fullName": "安利文",
    "email": "anliwen@example.com"
  },
  "roles": ["client"],
  "permissions": ["client:home", "credential:view"]
}
```

Token 过期、撤销或不存在时返回：

```json
{
  "active": false
}
```

`introspect` 只允许第三方后端调用。不要把 `clientSecret` 放到浏览器中让前端直接查询。

## 5. Token 续约

Access Token 即将过期时，第三方后端调用：

```http
POST /oauth/direct/refresh
Content-Type: application/json
```

请求体：

```json
{
  "clientId": "YOUR_APPLICATION_CLIENT_ID",
  "clientSecret": "YOUR_APPLICATION_CLIENT_SECRET",
  "refreshToken": "REFRESH_TOKEN"
}
```

成功响应与登录响应结构一致，会返回新的 `access_token` 和新的 `refresh_token`。Refresh Token 会轮换，旧 Refresh Token 立即失效，因此必须原子替换本地保存的两个 Token：

```text
旧 access_token  -> 新 access_token
旧 refresh_token -> 新 refresh_token
```

不要并发使用同一个 Refresh Token。续约失败时删除本地 Token 和会话，要求用户重新登录。

## 6. 登出

用户退出时，第三方后端调用：

```http
POST /oauth/direct/logout
Content-Type: application/json
```

请求体：

```json
{
  "clientId": "YOUR_APPLICATION_CLIENT_ID",
  "clientSecret": "YOUR_APPLICATION_CLIENT_SECRET",
  "token": "ACCESS_TOKEN"
}
```

成功响应：

```json
{
  "revoked": true
}
```

接口会撤销 Access Token 及其关联的 Refresh Token。第三方随后还必须清除自己的 Session、Cookie 和服务端 Token 缓存。

## 7. Token 存储和校验

FQ OAuth 同时使用 MySQL 和 Redis：

- Redis 是 Token 的实时权威存储，缓存键只使用 Token 哈希，不保存 Token 原文；Token 校验、续约和撤销均以 Redis 状态为准。
- MySQL 的 `t_oauth_tokens` 只做 Token 留存、审计和管理查询，保存 Token 哈希、用户、客户端、scope、过期时间、撤销状态和 Access/Refresh 关联关系。
- 登录签发 Token 时同时写入 Redis 和 MySQL；Redis 写入失败不会返回可用 Token。
- Redis 缓存 TTL 等于 Token 剩余有效期，Token 撤销、登出或 Refresh Token 轮换时会立即清理对应用户的 Token 缓存。
- Redis 未命中时直接判定 Token 无效，不回源 MySQL；Redis 故障时需要恢复 Redis 后重新登录。

## 8. 第三方后端实现建议

1. 所有请求必须使用 HTTPS；开发环境才允许使用本机 HTTP。
2. `clientSecret`、Access Token、Refresh Token 不写日志，不提交 Git。
3. Token 请求和响应设置 `Cache-Control: no-store`，禁止代理缓存。
4. FQ OAuth 返回 `401` 时，不要继续使用原 Token；按登录或续约失败处理。
5. Refresh Token 只保存在第三方后端的加密存储中。
6. 第三方业务接口使用自己的 Session 保护前端，不要把 FQ OAuth 的客户端密钥下发给前端。

## 9. 最小接入伪代码

```text
登录：
  response = POST /oauth/direct/token(clientId, clientSecret, username, password)
  保存 response.access_token 和 response.refresh_token 到服务端
  创建第三方本地会话

请求业务：
  校验第三方本地会话
  必要时 POST /oauth/direct/introspect 查询 FQ OAuth Token

续约：
  POST /oauth/direct/refresh(clientId, clientSecret, refreshToken)
  原子替换新的 access_token 和 refresh_token

退出：
  POST /oauth/direct/logout(clientId, clientSecret, accessToken)
  删除第三方本地会话和 Token 缓存
```
