-- Apply once to databases created before API Key application review was introduced.
ALTER TABLE `t_user_api_keys`
  MODIFY COLUMN `key_hash` char(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  MODIFY COLUMN `key_prefix` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  MODIFY COLUMN `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  ADD COLUMN `apply_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL AFTER `status`,
  ADD COLUMN `reviewed_by` bigint DEFAULT NULL AFTER `revoked_at`,
  ADD COLUMN `reviewed_at` datetime DEFAULT NULL AFTER `reviewed_by`,
  ADD COLUMN `review_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL AFTER `reviewed_at`;
