-- Bind OAuth clients to a registered platform. Backfill existing rows before making this NOT NULL.
ALTER TABLE `t_oauth_clients` ADD COLUMN `platform_id` bigint NULL COMMENT '所属接入平台' AFTER `id`;
CREATE INDEX `idx_t_oauth_clients_platform` ON `t_oauth_clients` (`platform_id`);
-- Example backfill: UPDATE t_oauth_clients SET platform_id = <matching t_platforms.id> WHERE platform_id IS NULL;
-- Then enforce: ALTER TABLE t_oauth_clients MODIFY COLUMN platform_id bigint NOT NULL;
