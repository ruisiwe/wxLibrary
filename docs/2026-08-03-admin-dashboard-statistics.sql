-- 后台首页统计：微信用户每日活跃记录与文档发送成功记录。
-- 本文件仅用于人工审核和执行，项目实施过程不会自动执行数据库迁移。

ALTER TABLE `wl_document_unlock`
  ADD KEY `idx_document_unlock_dashboard` (`del_flag`, `unlock_time`, `spent_points`, `document_id`);

CREATE TABLE IF NOT EXISTS `wl_wx_user_daily_active` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '微信用户编号',
  `active_date` date NOT NULL COMMENT '活跃日期',
  `create_by` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL,
  `update_by` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wx_user_daily_active` (`user_id`, `active_date`),
  KEY `idx_wx_user_daily_active_date` (`active_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户每日活跃记录';

CREATE TABLE IF NOT EXISTS `wl_document_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '微信用户编号',
  `document_id` bigint NOT NULL COMMENT '文档编号',
  `request_id` varchar(64) NOT NULL COMMENT '客户端发送请求号',
  `send_time` datetime NOT NULL COMMENT '发送成功时间',
  `create_by` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL,
  `update_by` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_send_request` (`request_id`),
  KEY `idx_document_send_document_time` (`document_id`, `send_time`),
  KEY `idx_document_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档发送成功记录';
