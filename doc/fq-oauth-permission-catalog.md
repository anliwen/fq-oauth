# fq-oauth 权限目录

权限 `code` 是稳定标识符：前端路由/菜单使用它控制显示，后端接口使用同一个 code 通过 Sa-Token 放行。权限归属 `fq-oauth` 平台，`superadmin` 角色在后端自动拥有本平台全部权限。

| 页面/能力 | 前端路径或菜单 | 后端接口 | 权限 code |
|---|---|---|---|
| 数据总览 | `/dashboard` | `GET /api/v1/users`, `GET /api/v1/platforms`, `GET /api/v1/roles` 等统计查询 | `dashboard:view`, `dashboard:read` |
| 平台管理 | `/platforms` | `GET/POST/PUT/DELETE /api/v1/platforms` | `platform:view`, `platform:manage` |
| 部门管理 | `/depts` | `GET/POST/PUT/DELETE /api/v1/depts` | `dept:view`, `dept:manage` |
| 用户管理 | `/users` | `GET/POST/PUT/DELETE /api/v1/users`, `PUT /status`, `PUT /password/reset` | `user:view`, `user:manage` |
| 角色管理 | `/roles` | `GET/POST/PUT/DELETE /api/v1/roles` | `role:view`, `role:manage` |
| 角色授权 | `/roles` | `GET/PUT /api/v1/roles/{roleId}/permissions` | `role:grant` |
| 权限配置 | `/permissions` | `GET/POST/PUT/DELETE /api/v1/permissions` | `permission:view`, `permission:manage` |
| 客户端首页 | `/client/home` | 静态门户 | `client:home` |
| 产品介绍 | `/client/product` | 静态门户 | `client:product` |
| 接入指南 | `/client/guide` | 静态门户 | `client:guide` |
| 场景案例 | `/client/cases` | 静态门户 | `client:cases` |
| Agent 管理 | `/agents` | `GET /api/v1/agents` | `agent:view` |
| Agent 写操作 | `/agents` | `POST /api/v1/agents`, `POST /{id}/suspend`, `POST /{id}/revoke` | `agent:manage` |
| 委托查询 | `/delegations` | `GET /api/v1/delegations` | `delegation:view` |
| 委托写操作 | `/delegations` | `POST /api/v1/delegations`, `POST /{id}/revoke`, `POST /{id}/restore` | `delegation:manage` |
| 委托校验 | 内部授权检查 | `POST /api/v1/delegations/check` | `delegation:check` |
| 服务客户端 | `/oauth-clients` | `GET /api/v1/oauth-clients` | `oauth-client:view` |
| 服务客户端写操作 | `/oauth-clients` | `POST/PUT/DELETE /api/v1/oauth-clients`, `PUT /status` | `oauth-client:manage` |
| API Key 审核 | `/api-key-applications` | `GET /api/v1/api-key-applications` | `api-key-review:view` |
| API Key 审核写操作 | `/api-key-applications` | `POST /{id}/review`, `POST /{id}/revoke` | `api-key:review`, `api-key:admin-revoke` |
| API Key 管理 | `/api-key-management` | `GET /api/v1/api-key-applications` | `api-key-management:view` |
| 我的 API Key | `/my-credentials` | `GET/POST /api/v1/me/api-keys` | `credential:view`, `credential:create` |
| API Key 生命周期 | `/my-credentials` | `POST /api-keys/{id}/issue`, `/revoke`, `/rotate` | `credential:issue`, `credential:revoke`, `credential:rotate` |
| 授权令牌 | `/my-credentials` | `GET /api/v1/me/tokens`, `POST /tokens/{id}/revoke` | `token:view`, `token:revoke` |
| 审计日志 | `/audit-logs` | `GET /api/v1/audit-logs` | `audit:view`, `audit:read` |

执行 [migration_fq_oauth_permissions.sql](sql/migration_fq_oauth_permissions.sql) 后，权限树和 `client` 角色的基础授权会写入数据库。管理员可在“角色管理”和“权限配置”中继续调整非系统角色；系统权限的 code、类型和父级不应修改。
