# FQ OAuth 用户授权中心

集中管理用户、组织、平台与角色权限，并为外部服务提供 OAuth 授权码接入、API Key 申请审核及个人凭证管理。仓库还包含 Agent 身份登记、委托授权检查和独立 Python 联调示例。

后端采用 Java 21、Spring Boot、Sa-Token、MyBatis/Spring JDBC、MySQL 和 Redis；前端采用 Vue、Vue Router 和 Vite。后端按 Maven 多模块组织，由 `fq-server` 组装运行。

## 文档导航

- [模块用途、页面使用与技术栈](doc/project-guide.md)：介绍各业务模块、基础设施模块、已实现/预留能力、数据库表及本地启动方式。
- [OAuth 授权码接入](doc/oauth-authorization-code.md)：外部服务的浏览器授权、PKCE、回调和令牌管理流程。
- [Travel Agent 联调示例](travel-agent/README.md)：独立 Python Agent 的接入和工具授权检查演示。
- [数据库初始化脚本](doc/sql/init.sql)：新库的表结构与初始数据；已有库请先审阅 `doc/sql/` 下的迁移脚本。

当前是模块化单体应用，并非每个 Maven 模块都已实现完整业务。审计、配额、策略引擎、联邦登录等预留模块的具体边界见模块说明。生产部署前应替换演示账号和连接凭据，并验证接入方的令牌/密钥校验逻辑。


  1. 服务自身身份 + 机密客户端

     MCP Server

     推荐组合。服务可以安全保存密钥，也可以开启 API Key 校验。

  2. 服务自身身份 + 公共客户端

     无法安全保存密钥的服务

     不推荐作为普通 MCP Server 配置，通常只能依赖其他安全措施。

  3. 终端应用 + 机密客户端

     Dify/Coze 后端、企业 Web 系统、Agent 平台

     用户在平台中登录并授权，平台后端保存密钥并换取 Token。

  4. 终端应用 + 公共客户端

     浏览器 SPA、移动 App、桌面应用

     客户端代码可能被用户看到，不能保存密钥，因此必须使用 PKCE。
