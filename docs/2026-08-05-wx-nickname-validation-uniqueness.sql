-- 微信用户昵称长度与唯一性约束。
-- 本文件仅供人工审核和执行，项目实施过程不会自动执行数据库迁移。
-- 必须先执行以下预检查询；存在结果时应人工处理数据，再执行 ALTER TABLE。

-- 预检超过 20 个字符的历史昵称。
SELECT `id`, `nickname`, CHAR_LENGTH(`nickname`) AS `nickname_length`
FROM `wl_wx_user`
WHERE CHAR_LENGTH(`nickname`) > 20;

-- 预检空昵称和程序禁止使用的保留名称。
SELECT `id`, `nickname`
FROM `wl_wx_user`
WHERE TRIM(`nickname`) = ''
   OR LOWER(TRIM(`nickname`)) IN ('null', 'undefined');

-- 按 nickname 当前排序规则预检重复昵称，结果包含大小写不敏感等排序规则语义。
SELECT `nickname`, COUNT(*) AS `duplicate_count`
FROM `wl_wx_user`
GROUP BY `nickname`
HAVING COUNT(*) > 1;

-- 所有预检均无结果后，再人工执行以下结构变更。
ALTER TABLE `wl_wx_user`
  MODIFY COLUMN `nickname` varchar(20) NOT NULL DEFAULT '' COMMENT '微信用户昵称';

ALTER TABLE `wl_wx_user`
  ADD UNIQUE KEY `uk_wx_user_nickname` (`nickname`);
