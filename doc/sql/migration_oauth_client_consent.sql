ALTER TABLE `t_oauth_clients`
  ADD COLUMN `skip_consent` tinyint(1) NOT NULL DEFAULT '0' COMMENT '可信内部应用是否跳过用户同意页' AFTER `can_introspect_api_keys`;
