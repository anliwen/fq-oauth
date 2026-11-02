ALTER TABLE `t_user_api_keys`
  ADD COLUMN `audience` varchar(128) NOT NULL DEFAULT 'fintech-mcp-server' AFTER `apply_reason`,
  ADD COLUMN `rotated_at` datetime DEFAULT NULL AFTER `revoked_at`;
CREATE INDEX `idx_t_user_api_keys_audience` ON `t_user_api_keys` (`audience`,`status`);
