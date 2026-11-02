-- Complete permission catalog for the fq-oauth platform.
-- Codes are stable identifiers shared by frontend route/menu checks and backend annotations.
-- Run after init.sql. The statements are idempotent because the platform/code pair is unique.

SET @platform_id := (SELECT id FROM t_platforms WHERE code = 'fq-oauth' AND is_deleted = 0 LIMIT 1);

-- Root directories
INSERT IGNORE INTO t_permissions(platform_id,parent_id,code,name,type,sort,is_system)
VALUES
  (@platform_id,NULL,'system','系统管理','directory',10,1),
  (@platform_id,NULL,'client-portal','客户端门户','directory',20,1),
  (@platform_id,NULL,'identity','身份与组织','directory',30,1),
  (@platform_id,NULL,'authorization','授权与凭证','directory',40,1),
  (@platform_id,NULL,'audit','审计','directory',50,1);

-- Menus
INSERT IGNORE INTO t_permissions(platform_id,parent_id,code,name,type,sort,is_system)
SELECT @platform_id,p.id,x.code,x.name,'menu',x.sort,1
FROM t_permissions p
JOIN (
  SELECT 'system' parent_code,'dashboard:view' code,'数据总览' name,1 sort
  UNION ALL SELECT 'system','platform:view','平台管理',2
  UNION ALL SELECT 'system','dept:view','部门管理',3
  UNION ALL SELECT 'system','user:view','用户管理',4
  UNION ALL SELECT 'system','role:view','角色管理',5
  UNION ALL SELECT 'system','permission:view','权限配置',6
  UNION ALL SELECT 'client-portal','client:home','客户端首页',1
  UNION ALL SELECT 'client-portal','client:product','产品介绍',2
  UNION ALL SELECT 'client-portal','client:guide','接入指南',3
  UNION ALL SELECT 'client-portal','client:cases','场景案例',4
  UNION ALL SELECT 'client-portal','credential:view','我的 API Key',5
  UNION ALL SELECT 'authorization','agent:view','Agent 管理',1
  UNION ALL SELECT 'authorization','delegation:view','委托授权',2
  UNION ALL SELECT 'authorization','oauth-client:view','服务客户端',3
  UNION ALL SELECT 'authorization','api-key-review:view','API Key 审核',4
  UNION ALL SELECT 'authorization','api-key-management:view','API Key 管理',5
  UNION ALL SELECT 'audit','audit:view','审计日志',1
) x ON x.parent_code=p.code
WHERE p.platform_id=@platform_id;

-- Management and API permissions. Parent menu must exist before buttons are inserted.
INSERT IGNORE INTO t_permissions(platform_id,parent_id,code,name,type,sort,is_system)
SELECT @platform_id,p.id,x.code,x.name,'button',x.sort,1
FROM t_permissions p
JOIN (
  SELECT 'dashboard:view' parent_code,'dashboard:read' code,'查看数据总览' name,1 sort
  UNION ALL SELECT 'platform:view','platform:manage','维护平台',1
  UNION ALL SELECT 'dept:view','dept:manage','维护部门',1
  UNION ALL SELECT 'user:view','user:manage','维护用户',1
  UNION ALL SELECT 'role:view','role:manage','维护角色',1
  UNION ALL SELECT 'role:view','role:grant','分配角色权限',2
  UNION ALL SELECT 'permission:view','permission:manage','维护权限树',1
  UNION ALL SELECT 'agent:view','agent:manage','维护 Agent',1
  UNION ALL SELECT 'delegation:view','delegation:manage','创建/撤销/恢复委托',1
  UNION ALL SELECT 'delegation:view','delegation:check','校验委托',2
  UNION ALL SELECT 'oauth-client:view','oauth-client:manage','维护服务客户端',1
  UNION ALL SELECT 'api-key-review:view','api-key:review','审核 API Key',1
  UNION ALL SELECT 'api-key-review:view','api-key:admin-revoke','管理员禁用 API Key',2
  UNION ALL SELECT 'api-key-management:view','api-key:management','查看 API Key 管理统计',1
  UNION ALL SELECT 'credential:view','credential:create','申请 API Key',1
  UNION ALL SELECT 'credential:view','credential:issue','生成 API Key',2
  UNION ALL SELECT 'credential:view','credential:revoke','撤销 API Key',3
  UNION ALL SELECT 'credential:view','credential:rotate','轮换 API Key',4
  UNION ALL SELECT 'credential:view','token:view','查看授权令牌',5
  UNION ALL SELECT 'credential:view','token:revoke','撤销授权令牌',6
  UNION ALL SELECT 'audit:view','audit:read','查看审计日志',1
) x ON x.parent_code=p.code
WHERE p.platform_id=@platform_id;

-- client role is intentionally narrow. The superadmin role is handled as an all-permissions role in code.
INSERT IGNORE INTO t_roles(platform_id,code,name,`desc`,is_system)
VALUES(@platform_id,'client','客户端用户','可访问客户端门户并申请、管理自己的 API Key',1);

INSERT IGNORE INTO t_role_permissions(role_id,permission_id)
SELECT r.id,p.id
FROM t_roles r
JOIN t_permissions p ON p.platform_id=@platform_id
WHERE r.platform_id=@platform_id AND r.code='client'
  AND p.code IN ('client:home','client:product','client:guide','client:cases','credential:view',
                 'credential:create','credential:issue','credential:revoke','credential:rotate',
                 'token:view','token:revoke');

-- Exact API-operation permissions. A menu's `*:view` code controls whether its page is visible;
-- each write endpoint is separately guarded by one of these child codes.
INSERT IGNORE INTO t_permissions(platform_id,parent_id,code,name,type,sort,is_system)
SELECT @platform_id,p.id,x.code,x.name,'button',x.sort,1
FROM t_permissions p
JOIN (
  SELECT 'platform:view' parent_code,'platform:create' code,'新增平台' name,10 sort UNION ALL
  SELECT 'platform:view','platform:update','修改平台',20 UNION ALL SELECT 'platform:view','platform:delete','删除平台',30 UNION ALL
  SELECT 'dept:view','dept:create','新增部门',10 UNION ALL SELECT 'dept:view','dept:update','修改部门',20 UNION ALL SELECT 'dept:view','dept:delete','删除部门',30 UNION ALL
  SELECT 'user:view','user:create','新增用户',10 UNION ALL SELECT 'user:view','user:update','修改用户、状态或密码',20 UNION ALL SELECT 'user:view','user:delete','删除用户',30 UNION ALL
  SELECT 'role:view','role:create','新增角色',10 UNION ALL SELECT 'role:view','role:update','修改角色',20 UNION ALL SELECT 'role:view','role:delete','删除角色',30 UNION ALL
  SELECT 'permission:view','permission:create','新增权限',10 UNION ALL SELECT 'permission:view','permission:update','修改权限',20 UNION ALL SELECT 'permission:view','permission:delete','删除权限',30 UNION ALL
  SELECT 'agent:view','agent:create','登记 Agent',10 UNION ALL SELECT 'agent:view','agent:update','暂停 Agent',20 UNION ALL SELECT 'agent:view','agent:delete','撤销 Agent',30 UNION ALL
  SELECT 'delegation:view','delegation:create','创建委托',10 UNION ALL SELECT 'delegation:view','delegation:update','恢复委托',20 UNION ALL SELECT 'delegation:view','delegation:delete','撤销委托',30 UNION ALL
  SELECT 'oauth-client:view','oauth-client:create','新增服务客户端',10 UNION ALL SELECT 'oauth-client:view','oauth-client:update','修改或禁用服务客户端',20 UNION ALL SELECT 'oauth-client:view','oauth-client:delete','删除服务客户端',30
) x ON x.parent_code=p.code
WHERE p.platform_id=@platform_id;

-- Backward-compatible grant migration: any role that had the former aggregate `*:manage`
-- permission receives its matching create/update/delete operation permissions.
INSERT IGNORE INTO t_role_permissions(role_id,permission_id)
SELECT legacy_grant.role_id, exact_permission.id
FROM t_role_permissions legacy_grant
JOIN t_permissions legacy_permission ON legacy_permission.id=legacy_grant.permission_id AND legacy_permission.platform_id=@platform_id
JOIN (
  SELECT 'platform:manage' legacy_code,'platform:create' exact_code UNION ALL SELECT 'platform:manage','platform:update' UNION ALL SELECT 'platform:manage','platform:delete' UNION ALL
  SELECT 'dept:manage','dept:create' UNION ALL SELECT 'dept:manage','dept:update' UNION ALL SELECT 'dept:manage','dept:delete' UNION ALL
  SELECT 'user:manage','user:create' UNION ALL SELECT 'user:manage','user:update' UNION ALL SELECT 'user:manage','user:delete' UNION ALL
  SELECT 'role:manage','role:create' UNION ALL SELECT 'role:manage','role:update' UNION ALL SELECT 'role:manage','role:delete' UNION ALL
  SELECT 'permission:manage','permission:create' UNION ALL SELECT 'permission:manage','permission:update' UNION ALL SELECT 'permission:manage','permission:delete' UNION ALL
  SELECT 'agent:manage','agent:create' UNION ALL SELECT 'agent:manage','agent:update' UNION ALL SELECT 'agent:manage','agent:delete' UNION ALL
  SELECT 'delegation:manage','delegation:create' UNION ALL SELECT 'delegation:manage','delegation:update' UNION ALL SELECT 'delegation:manage','delegation:delete' UNION ALL
  SELECT 'oauth-client:manage','oauth-client:create' UNION ALL SELECT 'oauth-client:manage','oauth-client:update' UNION ALL SELECT 'oauth-client:manage','oauth-client:delete'
) mapping ON mapping.legacy_code=legacy_permission.code
JOIN t_permissions exact_permission ON exact_permission.platform_id=@platform_id AND exact_permission.code=mapping.exact_code;
