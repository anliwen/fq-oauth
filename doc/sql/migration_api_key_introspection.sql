-- Run once on databases created before service-side API Key introspection was introduced.
-- Existing OAuth clients keep this capability disabled until an administrator re-registers/configures them.
ALTER TABLE `t_oauth_clients`
  ADD COLUMN `can_introspect_api_keys` tinyint(1) NOT NULL DEFAULT '0'
    COMMENT '是否允许服务端校验用户API Key' AFTER `client_type`;
