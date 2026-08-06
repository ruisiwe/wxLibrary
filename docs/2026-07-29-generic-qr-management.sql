-- 通用二维码管理。
-- 本文件仅供管理员审核后手工执行，程序不会自动执行数据库迁移。

CREATE TABLE IF NOT EXISTS `wl_qr_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_name` varchar(50) NOT NULL,
  `guide_text` varchar(200) NOT NULL DEFAULT '',
  `image_path` varchar(512) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` char(1) NOT NULL DEFAULT '0',
  `create_by` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qr_config_status_sort` (`status`, `sort_order`),
  CONSTRAINT `chk_qr_config_sort` CHECK (`sort_order` >= 0),
  CONSTRAINT `chk_qr_config_status` CHECK (`status` IN ('0','1'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用二维码配置';

INSERT INTO sys_menu
SELECT '2051','二维码管理','2000','6','qrConfig','library/qr/index','','',
  1,0,'C','0','0','library:qr:query','qrcode','admin',SYSDATE(),'',NULL,'通用二维码管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2051);

INSERT INTO sys_menu
SELECT '2116','查询二维码','2051','1','','','','',
  1,0,'F','0','0','library:qr:query','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2116);

INSERT INTO sys_menu
SELECT '2117','新增二维码','2051','2','','','','',
  1,0,'F','0','0','library:qr:add','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2117);

INSERT INTO sys_menu
SELECT '2118','修改二维码','2051','3','','','','',
  1,0,'F','0','0','library:qr:edit','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2118);

INSERT INTO sys_menu
SELECT '2119','删除二维码','2051','4','','','','',
  1,0,'F','0','0','library:qr:remove','#','admin',SYSDATE(),'',NULL,''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2119);
