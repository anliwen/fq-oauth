ALTER TABLE `t_oauth_clients`
  ADD COLUMN `client_kind` varchar(16) NOT NULL DEFAULT 'APPLICATION' AFTER `client_type`,
  ADD COLUMN `resource` varchar(128) DEFAULT NULL AFTER `client_kind`;
UPDATE `t_oauth_clients` SET `client_kind` = 'SERVICE', `resource` = COALESCE(`resource`, `name`) WHERE `can_introspect_api_keys` = 1;
CREATE INDEX `idx_t_oauth_clients_kind_resource` ON `t_oauth_clients` (`client_kind`, `resource`);
