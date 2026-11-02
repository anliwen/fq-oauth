# GitHub 第三方登录

GitHub 只负责证明第三方身份，FQ-OAuth 的本地用户和角色才是授权来源。

## 登录接口

客户端先通过 GitHub 官方 OAuth 流程获得 access token，再调用：

```http
POST /api/v1/auth/federated/github
Content-Type: application/json

{"accessToken":"<github-access-token>"}
```

FQ-OAuth 服务端会调用 GitHub `/user` 和 `/user/emails`，只接受已验证的主邮箱，然后用邮箱匹配 `t_users.email`：

- 匹配到且本地用户为 active：复用该 `user_id` 登录，返回 FQ-OAuth token、用户、角色和本地权限。
- 没有匹配到：返回“该 GitHub 邮箱未登记，请联系管理员”，不会自动创建用户，也不会授予默认角色。
- 本地用户被禁用：拒绝登录。

本地创建的用户和 GitHub 登录用户最终使用同一张 `t_users` 表、同一套 `t_user_roles` 角色关系和权限计算逻辑。

## 配置

可通过环境变量覆盖 GitHub API 地址：

```bash
FQ_GITHUB_API_BASE=https://api.github.com
```

生产环境的 GitHub client id、client secret 和 callback 应由接入应用或密钥管理服务保存，不能把 secret 放到前端。
