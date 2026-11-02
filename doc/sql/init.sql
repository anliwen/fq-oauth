-- =============================================================================
-- fq-oauth initial database: platforms, departments, users, roles and permissions.
--
-- t_users and t_user_roles are identical to alphadesk. t_roles is alphadesk's plus one column,
-- platform_id: every role belongs to exactly one platform, and a role code is unique within its platform.
-- t_platforms is new: the systems that connect to this centre for unified login and permission management.
-- t_permissions / t_role_permissions are new: a permission tree per platform (up to 5 levels; nodes are directory / menu / button) and the
-- permissions granted to each role. A permission CODE is what the frontend checks to show a page or button and what
-- the backend annotation checks to let a request through.
-- t_departments is alphadesk's: an organisation tree, and every user may belong to one department (t_users.dept_id).
-- One column exists but is unused and stays at its default: t_roles.data_scope = 'SELF'
--
-- Run once on an empty database. There are no DROP statements; if old tables exist, drop them first:
--   t_agent_delegations, t_agents, t_role_permissions, t_permissions, t_user_roles, t_users, t_roles, t_departments, t_platforms,
--   and the retired t_role_menus, t_menus, t_audit_logs
--
-- Accounts (Argon2id hashes):
--   admin               password: Fq@Admin2026   <- change it after the first login
--   the 9 demo users    password: Demo123456     (development only, delete before production)
-- The `superadmin` role of the `fq-oauth` platform always holds every permission of that platform (no grant rows needed).
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================== tables ==============================

-- ---------------------------- t_platforms ----------------------------
CREATE TABLE IF NOT EXISTS `t_platforms` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '平台编码，创建后不可修改，删除后也不可复用',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '平台名称',
  `desc` varchar(255) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '平台描述',
  `is_system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置（本中心自身）',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_platforms_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接入平台表';

-- ---------------------------- t_departments ----------------------------
CREATE TABLE IF NOT EXISTS `t_departments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `parent_id` bigint DEFAULT NULL COMMENT '上级部门ID（自关联）',
  `leader` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '负责人',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_departments_name` (`name`),
  KEY `fk_t_departments_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

-- ---------------------------- t_roles ----------------------------
CREATE TABLE IF NOT EXISTS `t_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `platform_id` bigint NOT NULL COMMENT '所属平台ID，一个角色只属于一个平台，创建后不可更换',
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码（同一平台内唯一）',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `desc` varchar(255) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '角色描述',
  `data_scope` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围 ALL/DEPT_AND_CHILD/DEPT/SELF',
  `is_system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统内置',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_roles_platform_code` (`platform_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- ---------------------------- t_users ----------------------------
CREATE TABLE IF NOT EXISTS `t_users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `username` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `email` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱',
  `full_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '姓名',
  `employee_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工号',
  `title` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职务',
  `hashed_password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码哈希',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'active' COMMENT '状态 active/disabled/locked',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `token_version` int NOT NULL DEFAULT '1' COMMENT '密码版本号，修改密码后自增使旧token失效',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_users_username` (`username`),
  UNIQUE KEY `uq_t_users_email` (`email`),
  UNIQUE KEY `uq_t_users_employee_no` (`employee_no`),
  KEY `fk_t_users_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ---------------------------- t_agents ----------------------------
CREATE TABLE IF NOT EXISTS `t_agents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform_id` bigint NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `owner_user_id` bigint NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_agents_platform_code` (`platform_id`,`code`),
  KEY `idx_t_agents_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部Agent登记表';

-- ---------------------------- t_agent_delegations ----------------------------
CREATE TABLE IF NOT EXISTS `t_agent_delegations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `grantor_user_id` bigint NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `resource` varchar(128) NOT NULL,
  `actions` varchar(512) NOT NULL,
  `resource_scope` varchar(512) NOT NULL DEFAULT '*',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `expires_at` datetime NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_t_agent_delegations_agent` (`agent_id`,`status`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent委托授权';

-- ---------------------------- t_user_roles ----------------------------
CREATE TABLE IF NOT EXISTS `t_user_roles` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_t_user_roles_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-角色关联表';

-- ---------------------------- t_permissions ----------------------------
CREATE TABLE IF NOT EXISTS `t_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `platform_id` bigint NOT NULL COMMENT '所属平台ID',
  `parent_id` bigint DEFAULT NULL COMMENT '上级节点ID，NULL 表示根节点',
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限编码（同一平台内唯一），前端据此显示页面/按钮，后端据此放行接口',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型 directory/menu/button',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置（代码里引用了该编码，不可删除）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_permissions_platform_code` (`platform_id`,`code`),
  KEY `fk_t_permissions_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权限树表（目录/菜单/按钮）';

-- ---------------------------- t_role_permissions ----------------------------
CREATE TABLE IF NOT EXISTS `t_role_permissions` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID（必须与角色属于同一平台，由服务层保证）',
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `fk_t_role_permissions_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色-权限关联表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================== data ==============================

-- platforms: this centre itself (built in), and alphadesk, whose business roles are seeded below
INSERT INTO `t_platforms` (`code`, `name`, `desc`, `is_system`) VALUES
  ('fq-oauth', '统一认证中心', '本中心自身，用户、角色和平台的管理权限都属于这个平台', 1),
  ('alphadesk', 'AlphaDesk', '投研平台，其业务角色（投资经理、研究员、风控专员）属于该平台', 0);

-- roles
INSERT INTO `t_roles` (`platform_id`, `code`, `name`, `desc`, `is_system`)
SELECT p.`id`, m.`code`, m.`name`, m.`desc`, m.`is_system`
FROM (
  SELECT 'fq-oauth' AS platform, 'superadmin' AS code, '超级管理员' AS name, '系统管理员，可管理平台、用户与角色' AS `desc`, 1 AS is_system
  UNION ALL
  SELECT 'alphadesk', 'pm', '投资经理', '记录并沉淀市场观点，维护知识库与技能，可管理组合类资源', 1
  UNION ALL
  SELECT 'alphadesk', 'analyst', '研究员', '记录投研信息、沉淀知识库，聚焦研究支持类功能', 1
  UNION ALL
  SELECT 'alphadesk', 'risk', '风控专员', '负责合规审查，只读访问投研资源与知识库', 0
) m
JOIN `t_platforms` p ON p.`code` = m.platform;

-- users (the first row is the administrator)
INSERT INTO `t_users` (`username`, `email`, `full_name`, `employee_no`, `title`, `hashed_password`, `status`, `is_active`) VALUES
  ('admin', 'admin@hczq.com', '系统管理员', 'ADMIN', '系统管理员', '$argon2id$v=19$m=65536,t=3,p=4$puNbgkeAfeqgiWRpCgYvXg$dfkr0gWffypdefmxDnPusGvoSfTLSj/oBKveGn2FhT8', 'active', 1),
  ('wangjiangfeng', 'wangjiangfeng@hczq.com', '王江丰', 'HC0001', '部门总经理', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('zhangwei', 'zhangwei@hczq.com', '张伟', 'HC0028', '投资总监', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('chenjing', 'chenjing@hczq.com', '陈静', 'HC0105', '研究总监', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('yueyang', 'yueyang@hczq.com', '岳阳', 'HC0132', '电子首席', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('liqiang', 'liqiang@hczq.com', '李强', 'HC0058', '投资总监', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('zhangyu', 'zhangyu@hczq.com', '张瑜', 'HC0160', '宏观首席', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('wangli', 'wangli@hczq.com', '王丽', 'HC0188', '风控经理', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1),
  ('zhaomin', 'zhaomin@hczq.com', '赵敏', 'HC0211', '研究员', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'disabled', 0),
  ('liuyang', 'liuyang@hczq.com', '刘洋', 'HC0235', '投资经理', '$argon2id$v=19$m=65536,t=3,p=4$Mw+fiFZGIlCAttBIwxDyxg$tBTg/tRdBpweTr3pndroSPEKk9XeNkFN+FizimY9Tio', 'active', 1);

-- user <-> role
INSERT INTO `t_user_roles` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM (
  SELECT 'admin' AS username, 'superadmin' AS code
  UNION ALL
  SELECT 'wangjiangfeng' AS username, 'superadmin' AS code
  UNION ALL
  SELECT 'zhangwei' AS username, 'pm' AS code
  UNION ALL
  SELECT 'chenjing' AS username, 'analyst' AS code
  UNION ALL
  SELECT 'yueyang' AS username, 'analyst' AS code
  UNION ALL
  SELECT 'liqiang' AS username, 'pm' AS code
  UNION ALL
  SELECT 'zhangyu' AS username, 'analyst' AS code
  UNION ALL
  SELECT 'wangli' AS username, 'risk' AS code
  UNION ALL
  SELECT 'zhaomin' AS username, 'analyst' AS code
  UNION ALL
  SELECT 'liuyang' AS username, 'pm' AS code
) m
JOIN `t_users` u ON u.`username` = m.username
JOIN `t_roles` r ON r.`code` = m.code AND r.`is_deleted` = 0;

-- ---------------------------- departments (demo, same tree as alphadesk) ----------------------------
INSERT INTO `t_departments` (`name`, `parent_id`, `leader`, `sort`) VALUES ('资产管理部', NULL, '王江丰', 1);

INSERT INTO `t_departments` (`name`, `parent_id`, `leader`, `sort`)
SELECT m.name, p.`id`, m.leader, m.sort
FROM (
  SELECT '权益投资组' AS name, '资产管理部' AS parent, '张伟' AS leader, 1 AS sort
  UNION ALL SELECT '固定收益组', '资产管理部', '李强', 2
  UNION ALL SELECT '研究组', '资产管理部', '陈静', 3
  UNION ALL SELECT '风险管理组', '资产管理部', '王丽', 4
) m
JOIN `t_departments` p ON p.`name` = m.parent;

INSERT INTO `t_departments` (`name`, `parent_id`, `leader`, `sort`)
SELECT m.name, p.`id`, m.leader, m.sort
FROM (
  SELECT '行业研究组' AS name, '研究组' AS parent, '岳阳' AS leader, 1 AS sort
  UNION ALL SELECT '宏观策略组', '研究组', '张瑜', 2
) m
JOIN `t_departments` p ON p.`name` = m.parent;

-- which department each demo user belongs to
UPDATE `t_users` u
JOIN (
  SELECT 'admin' AS username, '资产管理部' AS dept
  UNION ALL SELECT 'wangjiangfeng', '资产管理部'
  UNION ALL SELECT 'zhangwei', '权益投资组'
  UNION ALL SELECT 'chenjing', '研究组'
  UNION ALL SELECT 'yueyang', '行业研究组'
  UNION ALL SELECT 'liqiang', '固定收益组'
  UNION ALL SELECT 'zhangyu', '宏观策略组'
  UNION ALL SELECT 'wangli', '风险管理组'
  UNION ALL SELECT 'zhaomin', '行业研究组'
  UNION ALL SELECT 'liuyang', '权益投资组'
) m ON m.username = u.`username`
JOIN `t_departments` d ON d.`name` = m.dept
SET u.`dept_id` = d.`id`;

-- ---------------------------- permissions of this centre ----------------------------
-- Every code below is referenced by a @SaCheckPermission annotation or by the frontend, so these rows are built in.
-- Directories first, then menus, then buttons (each level needs its parent to exist).
INSERT INTO `t_permissions` (`platform_id`, `parent_id`, `code`, `name`, `type`, `sort`, `is_system`)
SELECT p.`id`, NULL, m.code, m.name, 'directory', m.sort, 1
FROM (
  SELECT 'system' AS code, '系统管理' AS name, 1 AS sort
) m
JOIN `t_platforms` p ON p.`code` = 'fq-oauth';

INSERT INTO `t_permissions` (`platform_id`, `parent_id`, `code`, `name`, `type`, `sort`, `is_system`)
SELECT p.`id`, d.`id`, m.code, m.name, 'menu', m.sort, 1
FROM (
  SELECT 'platform:view' AS code, '平台管理' AS name, 1 AS sort
  UNION ALL SELECT 'user:view', '用户管理', 2
  UNION ALL SELECT 'role:view', '角色管理', 3
  UNION ALL SELECT 'permission:view', '权限配置', 4
  UNION ALL SELECT 'dept:view', '部门管理', 5
) m
JOIN `t_platforms` p ON p.`code` = 'fq-oauth'
JOIN `t_permissions` d ON d.`platform_id` = p.`id` AND d.`code` = 'system';

INSERT INTO `t_permissions` (`platform_id`, `parent_id`, `code`, `name`, `type`, `sort`, `is_system`)
SELECT p.`id`, d.`id`, m.code, m.name, 'button', m.sort, 1
FROM (
  SELECT 'platform:manage' AS code, '维护平台' AS name, 'platform:view' AS parent_code, 1 AS sort
  UNION ALL SELECT 'user:manage', '维护用户', 'user:view', 1
  UNION ALL SELECT 'role:manage', '维护角色', 'role:view', 1
  UNION ALL SELECT 'role:grant', '分配权限', 'role:view', 2
  UNION ALL SELECT 'permission:manage', '维护权限树', 'permission:view', 1
  UNION ALL SELECT 'dept:manage', '维护部门', 'dept:view', 1
) m
JOIN `t_platforms` p ON p.`code` = 'fq-oauth'
JOIN `t_permissions` d ON d.`platform_id` = p.`id` AND d.`code` = m.parent_code;

-- ---------------------------- OAuth service authorization ----------------------------
CREATE TABLE IF NOT EXISTS `t_oauth_clients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform_id` bigint NOT NULL COMMENT '所属接入平台',
  `client_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `client_secret_hash` char(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `client_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CONFIDENTIAL',
  `client_kind` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'APPLICATION' COMMENT '主体类型：SERVICE 服务自身，APPLICATION 终端应用',
  `resource` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务主体绑定的资源标识',
  `can_introspect_api_keys` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否允许服务端校验用户API Key',
  `skip_consent` tinyint(1) NOT NULL DEFAULT '0' COMMENT '可信内部应用是否跳过用户同意页',
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `redirect_uris` text COLLATE utf8mb4_general_ci NOT NULL,
  `scopes` varchar(1024) COLLATE utf8mb4_general_ci NOT NULL,
  `created_by` bigint NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_oauth_clients_client_id` (`client_id`),
  KEY `idx_t_oauth_clients_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OAuth服务客户端';

CREATE TABLE IF NOT EXISTS `t_oauth_authorization_codes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code_hash` char(64) COLLATE utf8mb4_general_ci NOT NULL,
  `client_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `scope` varchar(1024) COLLATE utf8mb4_general_ci NOT NULL,
  `redirect_uri` varchar(2048) COLLATE utf8mb4_general_ci NOT NULL,
  `code_challenge` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `code_challenge_method` varchar(8) COLLATE utf8mb4_general_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `used` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_oauth_codes_hash` (`code_hash`),
  KEY `idx_t_oauth_codes_client_user` (`client_id`,`user_id`),
  KEY `idx_t_oauth_codes_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OAuth授权码';

CREATE TABLE IF NOT EXISTS `t_oauth_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_hash` char(64) COLLATE utf8mb4_general_ci NOT NULL,
  `token_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `client_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `scope` varchar(1024) COLLATE utf8mb4_general_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT '0',
  `parent_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_oauth_tokens_hash` (`token_hash`),
  KEY `idx_t_oauth_tokens_client_user` (`client_id`,`user_id`),
  KEY `idx_t_oauth_tokens_expiry` (`expires_at`),
  KEY `idx_t_oauth_tokens_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OAuth访问与刷新令牌';

CREATE TABLE IF NOT EXISTS `t_user_api_keys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `key_hash` char(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `key_prefix` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `apply_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `audience` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'fintech-mcp-server',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used_at` datetime DEFAULT NULL,
  `validation_count` bigint NOT NULL DEFAULT '0' COMMENT '成功校验次数',
  `revoked_at` datetime DEFAULT NULL,
  `rotated_at` datetime DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `review_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_t_user_api_keys_hash` (`key_hash`),
  KEY `idx_t_user_api_keys_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户API Key';

-- ---------------------------- t_audit_logs ----------------------------
CREATE TABLE IF NOT EXISTS `t_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint DEFAULT NULL COMMENT '操作者用户ID，匿名操作为空',
  `action` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
  `method` varchar(10) COLLATE utf8mb4_general_ci NOT NULL,
  `path` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `target` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SUCCESS',
  `status_code` int NOT NULL DEFAULT '200',
  `ip` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `user_agent` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `detail` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_t_audit_logs_create_time` (`create_time`),
  KEY `idx_t_audit_logs_operator` (`operator_id`),
  KEY `idx_t_audit_logs_action_result` (`action`,`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统操作审计日志';
