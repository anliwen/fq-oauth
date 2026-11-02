# FQ OAuth 项目模块与技术说明

本文介绍当前仓库的主要模块、页面用途、技术栈和本地开发入口。项目同时包含传统 IAM 管理能力、面向外部服务的 OAuth 授权，以及 Agent 委托授权演示。文中会区分已实现能力和仅预留的模块。

## 项目定位

FQ OAuth 是一个集中式身份与授权管理服务。它为管理人员提供用户、组织、角色、接入平台和权限配置；为接入服务提供 API Key 审批管理及 OAuth 2.0 授权码流程；也包含 Agent 身份登记和委托检查的演示实现。

当前仓库是 Maven 多模块单体应用：业务模块独立组织，但由 `fq-server` 组装并以一个 Spring Boot 服务运行。前端通过 HTTP API 调用后端。`travel-agent` 是单独运行的 Python 示例，不属于 FQ OAuth 服务端。

## 技术栈

| 层次 | 技术 | 用途 |
| --- | --- | --- |
| 后端运行时 | Java 21、Spring Boot 3.5.6 | REST API 与应用启动 |
| 后端模块构建 | Maven 多模块 | 管理公共依赖、基础设施、业务模块和应用入口 |
| 关系数据库 | MySQL、MyBatis Spring Boot Starter 3.0.5、Spring JDBC | 大部分业务使用 MyBatis XML；OAuth 模块使用 `JdbcTemplate` 持久化 |
| 登录会话 | Sa-Token 1.46.0、Redis | 登录状态、服务端会话和登录失败计数 |
| 密码存储 | Spring Security Crypto、Argon2id | 用户密码哈希及校验 |
| 前端 | Vue 3.5.44、Vue Router 5.4.0、Vite 8.3.4 | 管理端和客户端门户单页应用；以 `frontend/package.json` 为准 |
| 页面与图表 | 自定义 Vue 组件、CSS、SVG、浏览器 Fetch API | 表单、弹窗、选择器、统计图与 HTTP 请求；当前未引入第三方 UI/图表组件库 |
| Agent 示例 | Python 3.13+、FastAPI、HTTPX、Uvicorn、uv | 接收演示任务，在工具执行前检查委托；真实工具使用天气/搜索服务 |

示例依赖还声明了 OpenAI SDK、Requests、Tavily Python 客户端；依赖声明不等于每条任务路径都在调用大模型。后端使用 Lombok 减少样板代码，Bean Validation 进行请求参数校验，Spring 事务管理数据库写入。

数据库初始化脚本位于 `doc/sql/init.sql`。已有数据库的增量 SQL 位于 `doc/sql/` 下 `migration_*.sql` 文件。部署前应先审阅 SQL 并按目标环境执行；应用配置入口是 `fq-server/src/main/resources/application.yaml`。请使用部署环境自己的密钥和连接配置，勿将生产凭据写入代码或文档。

## 仓库结构

| 路径 | 职责 |
| --- | --- |
| `fq-common` | API 响应、分页结果、异常和通用类型 |
| `fq-dependencies` | 提供给外部 Maven 项目使用的依赖版本 BOM |
| `fq-starters` | Web、MyBatis、Redis、安全、审计、限流、加密、多租户和消息等通用基础设施封装 |
| `fq-modules` | 业务模块集合，见下文 |
| `fq-sdk` | 接入方 SDK 的预留模块，目前仅有 Maven 声明，尚无客户端代码 |
| `fq-server` | Spring Boot 启动入口、配置，以及对业务模块的组装 |
| `frontend` | Vue 管理端与客户端门户 |
| `travel-agent` | 独立运行的 Python Agent 联调示例 |
| `doc/sql` | 数据库初始化和迁移脚本 |
| `doc/oauth-authorization-code.md` | 外部服务 OAuth 授权码接入流程 |

## 后端业务模块

下表以代码当前状态为准。Maven 中声明了模块不代表该模块已有可用业务实现；尚无 `src` 代码的模块标记为“预留”。

| 模块 | 用途 | 当前状态 |
| --- | --- | --- |
| `fq-module-platform` | 登记接入 FQ 用户中心的平台，作为平台权限配置的边界 | 已实现，管理端“平台管理” |
| `fq-module-user` | 用户账号、角色和生命周期管理 | 已实现，管理端“用户管理”“角色管理” |
| `fq-module-org` | 部门组织树 | 已实现，管理端“部门管理” |
| `fq-module-authn` | 用户名密码登录、登出、当前用户信息、改密、登录会话及失败限流 | 已实现，使用 Sa-Token 和 Redis |
| `fq-module-authz` | 平台权限树、角色权限授予和接口权限校验 | 已实现，管理端“权限配置” |
| `fq-module-oauth2` | 外部服务客户端登记、浏览器授权同意、授权码换 Token、API Key 申请审核及用户凭证管理 | 已实现；详见 OAuth 授权码文档 |
| `fq-module-agent` | 登记 Agent 标识及其所属平台/租户关系 | 已实现，管理端“Agent 管理” |
| `fq-module-delegation` | 为 Agent 配置用户委托范围、过期时间，并对资源/动作请求返回授权决策 | 已实现，管理端“委托授权”；示例流程见 `travel-agent/README.md` |
| `fq-module-tenant` | 多租户领域能力 | 预留；当前存在模块声明和基础设施 starter，业务实现尚未落地 |
| `fq-module-federation` | 对接外部身份提供方，如 SAML、LDAP 或社交登录 | 预留，尚无业务实现 |
| `fq-module-mcp` | MCP Server/工具注册和工具级访问治理 | 预留，尚无业务实现；当前授权码服务面向外部服务客户端，不等同于内置 MCP Server |
| `fq-module-consent` | 高风险操作的人工确认与授权审批 | 预留，尚无独立业务实现；现有 OAuth 用户同意页面属于 `fq-module-oauth2` |
| `fq-module-audit` | 业务审计日志与风险事件查询 | 预留，尚无业务实现 |
| `fq-module-quota` | 调用配额、速率和用量控制 | 预留，尚无业务实现 |
| `fq-module-policy` | 细粒度策略决策引擎（如 ABAC/ReBAC） | 预留，尚无业务实现 |
1、易信、oa
2、alaph 用户接入
3、多端登录、多服务区别对待

### 基础设施模块

`fq-starters` 是 Maven 聚合模块，不独立启动。当前部分 starter 只集中引入依赖，并不包含对应名称所暗示的完整框架实现。

| 模块 | 使用方式与当前边界 |
| --- | --- |
| `fq-starter-web` | 引入 Spring MVC、Bean Validation；包含全局异常处理，统一业务错误、参数校验错误等响应 |
| `fq-starter-mybatis` | 引入 Spring JDBC 和 MyBatis Starter；各业务模块维护 Mapper 接口和 XML；当前没有通用租户拦截器实现 |
| `fq-starter-redis` | 引入 Spring Data Redis；实际会话及登录失败计数由认证模块使用 |
| `fq-starter-security` | 共享安全配置的预留位置；当前登录拦截器在 `fq-module-authn`，角色/权限提供器在 `fq-module-authz` |
| `fq-starter-audit` | 审计注解与切面的预留位置，当前无实现 |
| `fq-starter-ratelimit` | 引入 Redis starter，为通用限流预留位置；当前没有通用限流器，登录失败限流位于认证模块 |
| `fq-starter-crypto` | 密钥管理、加密和签名能力的预留位置；现有用户密码哈希位于 `fq-module-user`，OAuth 凭证哈希位于 `fq-module-oauth2` |
| `fq-starter-tenant` | 租户上下文的预留位置，当前无实现；业务对象中的 `tenantId` 不代表已有全局租户隔离 |
| `fq-starter-mq` | 消息和领域事件的预留位置，尚未选定消息中间件或实现消息收发 |

### 概念边界

- `authn` 回答“你是谁”，负责用户登录；`authz` 回答“你能操作什么”，负责平台内角色权限。
- “平台”用于组织用户中心管理的业务系统和权限；“OAuth 客户端”用于登记发起授权码流程的应用及回调地址，两者不是同一个实体。
- `client` 是本中心的角色编码；它与 OAuth `client_id`、Agent `id`、租户 `tenantId` 不同，不能互换填写。
- 登录 Token 是 Sa-Token 会话凭证；OAuth 访问令牌、刷新令牌保存在 OAuth 数据表中；API Key 是另一种独立凭证，不能假定三者可互换。
- 当前 OAuth 模块没有实现完整 OIDC 身份协议（如 ID Token、UserInfo、JWKS），不要仅根据模块 POM 描述将其当作 OIDC Provider。

## 页面用途

### 管理员页面

管理员登录后默认进入“数据总览”（登录 URL 有原页面跳转参数时保留返回行为）。该页面汇总已有业务接口的数据，展示统计、申请趋势和近期记录，不是 CPU、内存等基础设施监控。部分数据（如服务客户端、Agent、委托）受当前账号的数据范围限制，不应解读为全租户全局统计。左侧菜单按管理权限或管理员角色显示。

| 页面 | 用途 |
| --- | --- |
| 数据总览 | 查看用户授权中心的关键统计，并进入待审核申请 |
| 平台管理 | 维护使用中心能力的接入平台 |
| 部门管理 | 维护组织部门 |
| 用户管理 | 管理用户资料和账号状态 |
| 角色管理 | 管理角色及其关联用户 |
| 权限配置 | 配置平台权限树，并为角色授予权限 |
| Agent 管理 | 登记和维护 Agent 身份 |
| 委托授权 | 配置 Agent 可代表用户访问的资源、动作和范围，并维护授权有效期/状态 |
| 服务客户端 | 登记 OAuth 客户端类型、回调地址和允许的 scope |
| API Key 申请审核 | 查看客户端提交的申请并批准或拒绝 |

### Client 用户页面

具有 `client` 角色的用户登录后进入客户端门户首页，只显示客户端门户及“我的 API Key”相关菜单，不显示管理员菜单。

| 页面 | 用途 |
| --- | --- |
| 首页、产品介绍、接入指南、场景案例 | 面向客户端用户的产品与接入说明页面 |
| 我的 API Key | 提交申请、查看审核状态、审核通过后生成密钥、撤销有效密钥；也可查看并撤销已授权服务的访问/刷新令牌 |

角色编码区分大小写：本中心管理员为 `superadmin`，客户端角色为 `client`。若同一用户同时具有二者，当前前端优先按 `client` 展示客户端门户。菜单隐藏不是安全边界，最终以服务端接口校验为准。

外部服务通过 OAuth 授权码接入时，用户在 FQ OAuth 登录并同意授权，服务回调中取得授权码，再由服务端兑换令牌。浏览器不应保存客户端密钥。具体参数、PKCE 和端点参见 [`oauth-authorization-code.md`](oauth-authorization-code.md)。

## 主要授权流程

### API Key 申请与签发

1. `client` 用户进入“我的 API Key”，填写名称和申请用途并提交，状态为 `PENDING`（待审核）。
2. 管理员进入“API Key 申请审核”，查看申请人和用途，批准或填写原因拒绝。通过后为 `APPROVED`，拒绝后为 `REJECTED`。
3. 用户回到个人页面，点击“生成密钥”。系统此时才签发完整 API Key，状态变为 `ACTIVE`。
4. 完整密钥仅在本次生成响应/弹窗中展示，必须当场保存；列表只返回前缀，数据库保存 SHA-256 哈希，不支持重新查看原文。
5. 用户可以撤销自己的有效密钥，状态变为 `REVOKED`。

上述流程是当前已实现的申请和凭证生命周期管理。外部 MCP/API 服务可使用 OAuth 内省端点检查 API Key 状态和所属用户；工具资源权限仍由服务端策略决定。该能力不会自动建立统一 MCP 网关。

### 外部服务授权码

1. 管理员在“服务客户端”登记客户端、精确回调地址和可请求 scope。
2. 外部服务生成 `state` 与 PKCE 参数，保存于服务端会话，并引导用户访问 FQ OAuth `/oauth/authorize`。
3. 用户登录并在同意页面确认授权；同意后 FQ OAuth 将短时授权码和原始 `state` 返回登记的回调地址。
4. 外部服务校验 `state`，在服务端调用 `/oauth/token` 兑换访问令牌。客户端密钥只用于服务端认证。
5. 用户可以在客户端凭证页面查看/撤销个人凭证；客户端应安全保管令牌并处理过期和撤销。

### Agent 委托授权演示

1. 管理员创建平台和 Agent。
2. 使用 Agent 所属账号配置委托资源、动作和有效期（当前实现要求委托创建人是 Agent 所有者，单次委托最长 24 小时）。
3. `travel-agent` 收到用户请求，在执行工具前调用 FQ OAuth 委托检查接口。
4. 仅在检查允许时，示例才继续执行对应工具；撤销或超出委托范围时应拒绝。

当前 Python 示例通过用户访问令牌调用委托检查接口，主要用于本地验证授权规则，不是生产级 Agent 凭证交换方案。生产环境需进一步实现短期 Agent 凭证/令牌交换、密钥轮换、完整审计和服务间认证。

## 数据持久化

| 数据表 | 归属模块及用途 |
| --- | --- |
| `t_platforms` | 平台管理：接入平台信息 |
| `t_departments` | 组织管理：部门树 |
| `t_users`、`t_roles`、`t_user_roles` | 用户模块：账号、角色及用户角色关系 |
| `t_permissions`、`t_role_permissions` | 权限模块：平台权限树及角色权限关系 |
| `t_agents` | Agent 模块：Agent 登记、所属关系及状态 |
| `t_agent_delegations` | 委托模块：委托资源、动作、范围、有效期及撤销状态 |
| `t_oauth_clients` | OAuth 模块：客户端、密钥哈希、回调白名单、scope 及 API Key 内省能力开关 |
| `t_oauth_authorization_codes` | OAuth 模块：一次性授权码哈希及 PKCE 信息 |
| `t_oauth_tokens` | OAuth 模块：访问/刷新令牌哈希、有效期与撤销信息 |
| `t_user_api_keys` | OAuth 模块：API Key 申请、审核、密钥哈希和生命周期 |

初始化脚本使用 InnoDB、`utf8mb4` 和 `utf8mb4_general_ci`，不声明显式外键；关联关系由业务代码及 SQL 管理。登录会话使用 Redis，不属于 `t_oauth_tokens` 表。

## 开发与验证

需要安装 JDK 21、Maven、Node.js/npm，以及可用的 MySQL 和 Redis。Node.js 版本应满足项目安装的 Vite 要求。仓库当前没有 Maven Wrapper，使用本机 `mvn` 命令。

在仓库根目录构建后端（此命令跳过测试，不代表测试通过）：

```bash
mvn -DskipTests package
```

运行打包后的后端（先准备符合本机环境的 MySQL/Redis 配置）：

```bash
java -jar fq-server/target/fq-server-1.0.0-RELEASE.jar
```

构建前端：

```bash
cd frontend
npm install
npm run build
```

在 `frontend` 目录使用 `npm run dev` 启动开发服务器。

本地 Vite 默认端口为 `5173`，后端默认端口为 `8080`。Vite 开发代理将 `/api`、`/oauth` 和 `/.well-known` 转发到后端；如需覆盖目标，检查 `frontend/vite.config.js` 的 `FQ_API` 配置。

运行 Agent 演示前，先按 [`travel-agent/README.md`](../travel-agent/README.md) 在中心端创建平台、Agent 和委托，并配置示例中的 Agent ID。以 `travel-agent/src/main.py` 的实际配置为准：离线联调可将 `MOCK_TOOLS` 设为 `True`；设为 `False` 时会调用真实工具，需要网络及相应密钥。示例的硬编码配置仅用于本地演示，不应直接用于生产。

## 相关文档

- [OAuth 授权码接入](oauth-authorization-code.md)
- [OAuth 授权码及 API Key 内省接入](oauth-authorization-code.md)
- [数据库初始化](sql/init.sql)
- [OAuth 公共客户端迁移](sql/migration_oauth_public_clients.sql)
- [API Key 审批迁移](sql/migration_api_key_approval.sql)
- [Travel Agent 联调示例](../travel-agent/README.md)
