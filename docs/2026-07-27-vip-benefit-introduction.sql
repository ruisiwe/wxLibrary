-- VIP 权益介绍与客服微信配置。
-- 仅供已有数据库升级使用，请由管理员审核后手工执行。

CREATE TABLE IF NOT EXISTS `wl_vip_benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `benefit_text` varchar(100) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` char(1) NOT NULL DEFAULT '0',
  `create_by` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_vip_benefit_status_sort` (`status`, `sort_order`),
  CONSTRAINT `chk_vip_benefit_sort` CHECK (`sort_order` >= 0),
  CONSTRAINT `chk_vip_benefit_status` CHECK (`status` IN ('0','1'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP权益介绍';

CREATE TABLE IF NOT EXISTS `wl_vip_page_config` (
  `id` bigint NOT NULL,
  `customer_service_image_key` varchar(512) DEFAULT NULL,
  `customer_service_tip` varchar(100) NOT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP页面配置';

INSERT INTO `wl_vip_benefit` (`benefit_text`, `sort_order`, `status`,
  `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`)
SELECT '赠送积分', 10, '0', 'system', NOW(), '', NOW(), '0'
WHERE NOT EXISTS (
  SELECT 1 FROM `wl_vip_benefit` WHERE `benefit_text` = '赠送积分' AND `del_flag` = '0'
);

INSERT INTO `wl_vip_benefit` (`benefit_text`, `sort_order`, `status`,
  `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`)
SELECT 'VIP 文档免费下载', 20, '0', 'system', NOW(), '', NOW(), '0'
WHERE NOT EXISTS (
  SELECT 1 FROM `wl_vip_benefit` WHERE `benefit_text` = 'VIP 文档免费下载' AND `del_flag` = '0'
);

INSERT INTO `wl_vip_benefit` (`benefit_text`, `sort_order`, `status`,
  `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`)
SELECT 'VIP 专属课件', 30, '0', 'system', NOW(), '', NOW(), '0'
WHERE NOT EXISTS (
  SELECT 1 FROM `wl_vip_benefit` WHERE `benefit_text` = 'VIP 专属课件' AND `del_flag` = '0'
);

INSERT INTO `wl_vip_page_config` (`id`, `customer_service_image_key`, `customer_service_tip`,
  `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`)
VALUES (1, NULL, '开通 VIP 请添加客服微信', 'system', NOW(), '', NOW(), '0')
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);

INSERT INTO sys_menu
SELECT '2034','VIP 权益介绍','2003','5','benefit','library/vip/benefit/index','','',
  1,0,'C','0','0','library:vip:benefit:list','star','admin',SYSDATE(),'',NULL,'VIP 权益介绍'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2034);

INSERT INTO sys_menu
SELECT '2111','新增VIP权益','2034','1','','','','',
  1,0,'F','0','0','library:vip:benefit:add','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2111);

INSERT INTO sys_menu
SELECT '2112','修改VIP权益','2034','2','','','','',
  1,0,'F','0','0','library:vip:benefit:edit','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2112);

INSERT INTO sys_menu
SELECT '2113','删除VIP权益','2034','3','','','','',
  1,0,'F','0','0','library:vip:benefit:remove','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2113);

INSERT INTO sys_menu
SELECT '2114','查询VIP客服配置','2034','4','','','','',
  1,0,'F','0','0','library:vip:page-config:query','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2114);

INSERT INTO sys_menu
SELECT '2115','修改VIP客服配置','2034','5','','','','',
  1,0,'F','0','0','library:vip:page-config:edit','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2115);
