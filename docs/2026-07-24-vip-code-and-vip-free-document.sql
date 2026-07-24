-- 会员码与会员免费文档结构变更
-- 执行前请先确认当前库已包含 wl_vip_plan、wl_vip_entitlement、wl_document 表。

CREATE TABLE IF NOT EXISTS `wl_vip_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员码编号',
  `plan_id` bigint NOT NULL COMMENT '会员套餐编号',
  `code_digest` char(64) NOT NULL COMMENT '会员码SHA-256摘要',
  `code_mask` varchar(32) NOT NULL COMMENT '会员码掩码',
  `status` varchar(16) NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED未使用、USED已使用、DISABLED已禁用',
  `used_user_id` bigint DEFAULT NULL COMMENT '使用微信用户编号',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `expires_time` datetime DEFAULT NULL COMMENT '过期时间',
  `batch_no` varchar(64) NOT NULL COMMENT '生成批次号',
  `vip_entitlement_id` bigint DEFAULT NULL COMMENT '会员权益记录编号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vip_code_digest` (`code_digest`),
  KEY `idx_vip_code_plan` (`plan_id`),
  KEY `idx_vip_code_batch` (`batch_no`),
  KEY `idx_vip_code_user` (`used_user_id`),
  CONSTRAINT `chk_vip_code_status` CHECK (`status` IN ('UNUSED','USED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员兑换码';

ALTER TABLE `wl_document`
  ADD COLUMN `access_type` varchar(20) NOT NULL DEFAULT 'POINT' COMMENT '访问方式：POINT积分兑换，VIP_FREE会员免费'
  AFTER `point_price`;

ALTER TABLE `wl_document`
  ADD CONSTRAINT `chk_document_access_type`
  CHECK (`access_type` IN ('POINT','VIP_FREE'));

ALTER TABLE `wl_document_unlock`
  MODIFY COLUMN `point_record_id` bigint DEFAULT NULL COMMENT '积分流水编号，会员免费解锁为空';

ALTER TABLE `wl_vip_entitlement`
  DROP CHECK `chk_vip_entitlement_source_type`;

ALTER TABLE `wl_vip_entitlement`
  ADD CONSTRAINT `chk_vip_entitlement_source_type`
  CHECK (`source_type` IN ('PAYMENT','MANUAL','COMPENSATION','VIP_CODE'));
