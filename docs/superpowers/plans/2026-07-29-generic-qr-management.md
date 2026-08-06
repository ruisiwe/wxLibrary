# 通用二维码管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增后台通用二维码管理，将有效二维码动态展示在已登录用户的“我的”页面，并把通用二维码及 VIP 客服微信二维码统一改为 `RuoYiConfig.getWechatProfile()` 下的本地安全存储。

**Architecture:** 后端以 `wl_qr_config` 保存菜单文字、引导文字、相对图片路径、排序和状态；管理端通过独立 CRUD 与 multipart 图片接口维护。小程序只读取启用记录，跳转到统一详情页并通过受控图片接口读取文件；VIP 客服二维码沿用现有配置表，但图片从 COS 改为独立本地子目录。

**Tech Stack:** Java 8、Spring Boot、MyBatis、RuoYi-Vue/Element UI、微信小程序原生框架、JUnit 5、Node.js `node:test`。

---

## 文件结构

- 新建 `docs/2026-07-29-generic-qr-management.sql`：新表和后台菜单权限 SQL。
- 新建 `WlQrConfig`、`WlQrConfigMapper`、`WlQrConfigMapper.xml`、`QrConfigService`：通用二维码配置的领域、持久化与业务规则。
- 新建 `QrImageStorageService`：两个本地二维码目录的格式校验、安全写入、受控读取和清理。
- 新建 `LibraryQrConfigController`：后台列表、详情、增删改、上传、清空和图片预览。
- 新建 `WxQrConfigController`：登录用户的有效列表、详情和受控图片读取。
- 修改 `VipPageConfigService` 与现有 VIP 页面接口：把客服微信二维码改为本地路径和受控访问 URL。
- 新建 `ruoyi-ui/src/api/library/qrConfig.js` 与 `ruoyi-ui/src/views/library/qr/index.vue`：后台维护页面。
- 新建 `miniprogram/services/qr.js` 和 `miniprogram/pages/qr-code/*`：小程序动态菜单与统一二维码展示页。
- 修改 `miniprogram/pages/profile/profile.*` 和 `miniprogram/app.json`：在会员兑换后动态插入有效二维码菜单。

### Task 1: 数据结构与 MyBatis 持久化

**Files:**
- Create: `docs/2026-07-29-generic-qr-management.sql`
- Modify: `sql/wechat_library.sql`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlQrConfig.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlQrConfigMapper.java`
- Create: `ruoyi-wechat-library/src/main/resources/mapper/library/WlQrConfigMapper.xml`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/WlQrConfigMapperXmlTest.java`

- [ ] **Step 1: 写失败的 Mapper XML 契约测试**

  断言 XML 同时包含 `del_flag = '0'`、启用条件 `status = '0'`、`order by sort_order asc, id asc`，并且更新图片时使用 `id + oldImagePath` 乐观条件。

- [ ] **Step 2: 运行测试并确认失败**

  Run: `E:\JDK8\bin\java.exe -version`，再用 IntelliJ 自带 Maven 执行 `-pl ruoyi-wechat-library -am -Dtest=WlQrConfigMapperXmlTest -Dsurefire.failIfNoSpecifiedTests=false test`。
  Expected: FAIL，因为 mapper 与 XML 尚不存在。

- [ ] **Step 3: 实现领域对象、Mapper 和 SQL**

  `WlQrConfig` 字段固定为 `id/menuName/guideText/imagePath/sortOrder/status` 并继承 `BaseEntity`。Mapper 提供后台列表、按编号查询、启用列表、插入、更新基础字段、按旧路径更新图片、软删除。SQL 创建 `wl_qr_config`，图片只保存相对路径；写入菜单“二维码管理”及 `library:qr:query/add/edit/remove` 权限，不执行 SQL。

- [ ] **Step 4: 运行 Mapper 契约测试**

  Expected: PASS。

### Task 2: 本地二维码图片安全存储

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/WechatProfileStoragePaths.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/QrImageStorageService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/WechatProfileStoragePathsTest.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/QrImageStorageServiceTest.java`

- [ ] **Step 1: 写失败的路径和存储测试**

  覆盖 `qr-config`、`vip-customer-service` 两个根目录；JPEG/PNG/WebP 成功、超过 2MB 拒绝、扩展名伪装拒绝、随机文件名、相对路径读取、目录穿越与符号链接拒绝、静默删除仅限对应根目录。

- [ ] **Step 2: 运行测试并确认失败**

  Run: IntelliJ Maven `-pl ruoyi-wechat-library -am -Dtest=WechatProfileStoragePathsTest,QrImageStorageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。
  Expected: FAIL，因为新目录方法和服务尚不存在。

- [ ] **Step 3: 实现存储服务**

  `WechatProfileStoragePaths` 新增 `qrConfigRoot()` 和 `vipCustomerServiceRoot()`。`QrImageStorageService` 校验空文件、2MB 上限、实际图片解码、JPEG/PNG/WebP 类型；以 UUID 文件名原子写入；数据库返回根目录内相对路径；读取和删除前规范化并验证路径仍在对应根目录内。

- [ ] **Step 4: 运行存储测试**

  Expected: PASS。

### Task 3: 通用二维码业务与接口

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/QrConfigView.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/QrConfigService.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/QrConfigServiceTest.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryQrConfigController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxQrConfigController.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryQrConfigControllerTest.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/WxQrConfigControllerTest.java`

- [ ] **Step 1: 写失败的服务和控制器测试**

  覆盖字段必填和长度、排序非负、状态仅 `0/1`；启用列表不暴露磁盘路径；详情仅允许启用记录；上传替换“先存新文件、数据库乐观更新失败删新文件、成功后删旧文件”；清空和软删除“先提交数据库再删旧文件”；后台权限注解；小程序接口受 `Wx-Token` 保护。

- [ ] **Step 2: 运行测试并确认失败**

  Run: IntelliJ Maven 分别执行 `QrConfigServiceTest`、`LibraryQrConfigControllerTest`、`WxQrConfigControllerTest`。
  Expected: FAIL，因为服务和控制器尚不存在。

- [ ] **Step 3: 实现业务服务**

  后台 CRUD 返回完整配置但不返回绝对路径；小程序视图只返回 `id/menuName/guideText/imageConfigured/imageUrl`。图片 URL 固定为 `/wx/qr-configs/{id}/image`，服务读取数据库中的相对路径后交给存储服务。上传、清空、删除使用事务模板保证数据库状态先成功，再清理旧文件。

- [ ] **Step 4: 实现后台和小程序接口**

  后台路径 `/library/qr-config`，权限使用 `library:qr:query/add/edit/remove`，中文 API 注释、中文错误。小程序路径 `/wx/qr-configs`，列表、详情和图片均经过现有微信 token 拦截器；图片按扩展名返回正确媒体类型。

- [ ] **Step 5: 运行服务和控制器测试**

  Expected: PASS。

### Task 4: VIP 客服微信二维码改为本地存储

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipPageConfigService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipPageConfigServiceTest.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxVipPageController.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/WxVipPageControllerTest.java`
- Modify: `ruoyi-ui/src/views/library/vip/benefit/index.vue`
- Modify: `miniprogram/services/vip.js`

- [ ] **Step 1: 将现有 COS 测试改为本地存储失败测试**

  验证新上传写入 `vip-customer-service`，配置表仍使用现有 `customer_service_image_key` 列保存相对路径；替换/清空遵循安全清理顺序；旧 COS key 无法解析时视为未配置而不报错；公开图片接口不暴露绝对路径。

- [ ] **Step 2: 运行测试并确认失败**

  Run: IntelliJ Maven 执行 `VipPageConfigServiceTest,WxVipPageControllerTest`。
  Expected: FAIL，因为服务仍依赖 COS。

- [ ] **Step 3: 替换为本地存储实现**

  移除 `VipPageConfigService` 对 COS 与临时图片处理器的依赖，改用 `QrImageStorageService`。有本地文件时返回 `/wx/public/vip-page-config/customer-service-image`；新增公开受控读取接口。旧 COS key 不迁移，由后台重新上传。

- [ ] **Step 4: 适配后台和小程序 URL**

  管理端预览为相对 URL 时加 `VUE_APP_BASE_API`；小程序 `vip.pageConfig()` 为相对 URL 拼接 `apiBaseUrl()`。

- [ ] **Step 5: 运行 VIP 配置测试**

  Expected: PASS。

### Task 5: 后台二维码管理页面

**Files:**
- Create: `ruoyi-ui/src/api/library/qrConfig.js`
- Create: `ruoyi-ui/src/views/library/qr/index.vue`
- Create: `ruoyi-ui/tests/qr-config-management.test.js`
- Modify: `ruoyi-ui/package.json`

- [ ] **Step 1: 写失败的页面契约测试**

  断言页面包含名称、引导文字、图片、排序、状态列；新增/编辑；启停；上传/替换；清空；删除；图片格式和 2MB 前端校验；无图片文案；列表外层不出现卡片边框。

- [ ] **Step 2: 运行测试并确认失败**

  Run: `node ruoyi-ui/tests/qr-config-management.test.js`。
  Expected: FAIL，因为页面尚不存在。

- [ ] **Step 3: 实现 API 与页面**

  API 映射 `/library/qr-config` 的 CRUD、multipart 上传和清空。页面沿用项目现有紧凑筛选栏、工具栏和无外框表格风格；一个弹窗维护文字/排序/状态，列表行直接维护图片动作；上传仅接受 JPEG/PNG/WebP 且不超过 2MB。

- [ ] **Step 4: 运行页面契约测试**

  Expected: PASS。

### Task 6: 小程序动态二维码菜单和统一详情页

**Files:**
- Create: `miniprogram/services/qr.js`
- Modify: `miniprogram/pages/profile/profile.js`
- Modify: `miniprogram/pages/profile/profile.wxml`
- Create: `miniprogram/pages/qr-code/qr-code.js`
- Create: `miniprogram/pages/qr-code/qr-code.json`
- Create: `miniprogram/pages/qr-code/qr-code.wxml`
- Create: `miniprogram/pages/qr-code/qr-code.wxss`
- Modify: `miniprogram/app.json`
- Create: `miniprogram/tests/qr-menu.test.js`

- [ ] **Step 1: 写失败的小程序契约测试**

  断言“我的”页面仅在已有 `profile` 的登录分支遍历 `qrMenus`，位置位于“会员兑换”和“用户隐私协议”之间；按 id 跳转统一页；接口失败不清空用户资料；详情页含名称、引导文字、预览/长按图片和“二维码暂未配置”空态；`app.json` 注册页面。

- [ ] **Step 2: 运行测试并确认失败**

  Run: `node --test miniprogram/tests/qr-menu.test.js`。
  Expected: FAIL，因为服务和页面尚不存在。

- [ ] **Step 3: 实现登录用户菜单加载**

  `qr.list()` 使用受保护请求。`profile.load()` 分别加载会员资料和二维码菜单，二维码请求失败只把 `qrMenus` 置空，不改变 `profile`；WXML 在指定位置遍历有效数据，每条跳转 `/pages/qr-code/qr-code?id=<id>`。

- [ ] **Step 4: 实现统一详情页**

  页面按 id 请求详情；若已配置图片，使用带 `Wx-Token` 的 `wx.downloadFile` 下载临时文件后显示，并支持 `wx.previewImage`；否则显示“二维码暂未配置”。缺少/非法 id、接口失败均显示中文提示。

- [ ] **Step 5: 运行小程序契约测试**

  Expected: PASS。

### Task 7: 集成验证与 SQL 交付

**Files:**
- Modify: `docs/superpowers/plans/2026-07-29-generic-qr-management.md`

- [ ] **Step 1: 运行后端相关测试**

  使用 IntelliJ 自带 Maven，Java 8，执行本次新增和修改的存储、服务、Mapper、控制器测试；Expected: 全部 PASS。

- [ ] **Step 2: 运行前端和小程序测试**

  Run: `node ruoyi-ui/tests/qr-config-management.test.js`、`npm test`（`miniprogram`）；Expected: 本次新增测试 PASS，若全量存在既有失败则记录并确认与本次改动无关。

- [ ] **Step 3: 构建校验**

  Run: IntelliJ Maven `-pl ruoyi-admin -am -DskipTests package`，再在 `ruoyi-ui` 执行 `npm run build:prod`。不提交 `ruoyi-ui/dist` 或其他构建产物。

- [ ] **Step 4: 静态检查**

  Run: `git diff --check`，检查 `git status --short`，确认没有读取或修改 `.env`、`application.yml`、`application-druid.yml`，没有覆盖已有脏工作区改动。

- [ ] **Step 5: 交付**

  在最终回复中说明改动、验证结果、未执行数据库迁移，并完整粘贴 `docs/2026-07-29-generic-qr-management.sql` 内容供用户复制执行。
