-- Run once on existing databases to enable public OAuth clients for PKCE-based integrations.
ALTER TABLE `t_oauth_clients`
  MODIFY COLUMN `client_secret_hash` char(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  ADD COLUMN `client_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CONFIDENTIAL' AFTER `client_secret_hash`;
