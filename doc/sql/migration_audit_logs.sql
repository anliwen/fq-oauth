-- Apply once to databases created before the audit log page was introduced.
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
