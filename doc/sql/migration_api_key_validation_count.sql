ALTER TABLE t_user_api_keys
  ADD COLUMN validation_count bigint NOT NULL DEFAULT 0 COMMENT '成功校验次数' AFTER last_used_at;
