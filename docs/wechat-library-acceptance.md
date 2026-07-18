# 微信文库验收清单

## 说明

- `PASS`：已有自动化测试、静态契约或代码证据支持。
- `FAIL`：验证发现不符合要求，必须修复后重测。
- `PENDING`：必须依赖微信真机、测试商户、私有 COS 测试对象或实际角色账户，本次未使用生产环境代替。
- 自动化结果与真机结果必须分别记录；真机执行人需补充日期、环境、测试单号或截图编号，禁止记录密钥、令牌和私有 URL。

## 验收矩阵

| 编号 | 验收项 | 自动化结果 | 真机结果 | 当前证据/操作 |
| --- | --- | --- | --- | --- |
| A01 | 匿名访问首页、分类、文档和课程元数据，不强制登录且不返回私有对象键 | PASS | PENDING | `WxPublicContentController`、`WxPublicCourseController`；小程序首页请求标记为匿名；Controller 测试校验课程无对象键 |
| A02 | 首次登录必须提交头像、昵称、用户隐私协议和网站声明 | PASS | PENDING | multipart/JSON 登录测试、协议服务测试、`login-sheet` 的 `chooseAvatar` 与双协议校验 |
| A03 | 匿名用户可看元数据，但试读边界要求登录；未兑换不能获取完整 PDF | PASS | PENDING | `document-access.test.js`、文档访问服务/Controller 测试 |
| A04 | 同一用户并发兑换同一文档只扣一次积分并返回同一授权 | PASS | N/A | 文档解锁唯一键、事务锁和并发/幂等单元测试 |
| A05 | 分享原文件时由微信接收人选择器决定接收人，不传固定接收人 | PASS | PENDING | `buildShareOptions` 仅返回 `filePath`；源码契约测试排除 `toUser/openid` |
| A06 | 激励视频完整观看才发积分，每日最多 5 次 | PASS | PENDING | 积分服务每日次数约束和小程序本地 5 次提示；需真机验证广告关闭/完整观看 |
| A07 | 月卡 30 天、年卡 365 天；续费从当前到期日或当前时间中较晚者顺延 | PASS | N/A | VIP 套餐校验与权益服务日期测试 |
| A08 | 后台补偿开通/续期赠送积分恒为 0 | PASS | N/A | 权益服务来源类型与补偿测试、管理端补偿表单固定 0 |
| A09 | 重复支付成功通知只确认一次订单、发放一次权益和赠送积分 | PASS | PENDING | `VipOrderServiceTest.duplicatePaymentNotificationGrantsOnce`；需测试商户重复回调验证 |
| A10 | 退款受理中不撤销权益，只有最终成功通知才撤销对应支付权益 | PASS | PENDING | 退款状态机单元测试；需测试商户退款通知验证 |
| A11 | 退款追回积分后余额不得低于 0 | PASS | N/A | 积分余额数据库约束与退款服务测试 |
| A12 | 积分不足以追回时记录未追回积分审计值 | PASS | N/A | 退款结果/退款记录测试与管理端退款预估展示 |
| A13 | 一个课程码只能被一名用户使用，重复提交对原用户幂等 | PASS | N/A | 课程码摘要唯一性、行锁、使用状态和兑换测试 |
| A14 | 课程码课程兑换后为永久访问，不受 VIP 到期影响 | PASS | PENDING | `VideoPlaybackServiceTest` 与 `course-access.test.js`；真机验证播放器入口 |
| A15 | VIP 课程只在会员有效期内可播放，到期立即拒绝 | PASS | PENDING | `VideoPlaybackServiceTest` 和资料 `vipActive`；真机验证到期 UI/播放错误 |
| A16 | 私有 COS 文档/视频 URL 为短时签名且过期后失效，客户端不缓存播放 URL | PASS | PENDING | 文件/视频授权 TTL 测试；播放器每次进入重新授权且不写本地存储 |
| A17 | 停用用户的现有令牌立即阻断且不刷新 TTL；停用内容不可继续访问 | PASS | PENDING | 微信鉴权拦截器、令牌 TTL 与内容状态测试；需真机验证当前会话提示 |
| A18 | 管理员菜单和按钮受 `library:*` 权限控制，退款/补偿/课程码权限独立 | PASS | PENDING | 菜单/API 契约脚本、Controller `@PreAuthorize`/权限注解；需使用不同角色账户验收 |

## 真机专项记录

| 项目 | 结果 | 测试环境/单号 | 日期与执行人 | 备注 |
| --- | --- | --- | --- | --- |
| `wx.requestPayment` 取消 | PENDING |  |  | 不创建本地 VIP |
| `wx.requestPayment` 成功后后台状态轮询 | PENDING |  |  | 使用非生产测试订单 |
| `wx.openDocument` 试读与完整 PDF | PENDING |  |  | 完整 PDF 必须先兑换 |
| `wx.shareFileMessage` 接收人选择 | PENDING |  |  | 不指定固定接收人 |
| 激励视频完整/中途关闭 | PENDING |  |  | 每日第 6 次必须拒绝 |
| 视频断点续播与短时 URL 重新获取 | PENDING |  |  | 不记录实际私有 URL |
| 支付回调重复投递 | PENDING |  |  | 权益和赠送积分各一次 |
| 退款受理与最终成功回调 | PENDING |  |  | 最终成功前不撤销权益 |

## 本次自动化验证记录

执行日期：2026-07-18。以下结果来自当前工作树，未连接生产商户、私有 COS 或真实用户数据。

| 验证项 | 结果 | 证据摘要 |
| --- | --- | --- |
| Java 全量测试 | PASS | Java 8、IntelliJ 自带 Maven 执行 `clean test`；微信文库模块 126 项、管理端 33 项，共 159 项无失败；头像符号链接能力相关 3 项在当前 Windows 环境按条件跳过 |
| Java 8 全量打包 | PASS | Java 8 执行 `mvn -DskipTests package`，8 个 Reactor 模块全部成功 |
| 小程序契约测试 | PASS | Node.js 24 运行 `node --test miniprogram/tests/*.test.js`，22 项全部通过 |
| 管理端菜单/API 契约 | PASS | `node ruoyi-ui/scripts/verify-library-routes.js` 通过 |
| 管理端生产构建 | PASS | `npm run build:prod` 编译成功；仅有 RuoYi 既有资源包体积告警，临时 `dist` 与依赖目录联接已删除 |
| Git 空白/产物/凭据检查 | PASS | `git diff --check` 通过；构建目录和依赖目录均被忽略且未跟踪，敏感扩展名和环境配置规则已纳入 `.gitignore` |
