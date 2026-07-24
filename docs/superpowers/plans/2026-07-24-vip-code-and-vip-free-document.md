# 会员码与会员免费文档 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 增加会员码生成/兑换功能，并让会员免费文档对 VIP 用户免费下载、非 VIP 用户继续积分兑换。

**Architecture:** 后端新增独立 `wl_vip_code` 表、实体、Mapper、Service 和管理端/小程序接口；会员码兑换复用现有 `VipEntitlementService` 开通/续期。文档新增 `access_type` 字段，文档访问服务根据用户 VIP 状态决定是否扣积分。

**Tech Stack:** Java 8、Spring Boot、MyBatis、RuoYi Vue、微信小程序原生页面、Jest 风格小程序单元测试。

---

### Task 1: 后端测试先行

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentAccessServiceTest.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipCodeServiceTest.java`

- [ ] **Step 1: Write failing tests**

覆盖：VIP 用户兑换会员免费文档时写入 0 积分解锁、不调用扣积分；非 VIP 用户兑换会员免费文档时继续扣积分；会员码兑换成功调用 VIP 权益开通并标记会员码使用。

- [ ] **Step 2: Run focused tests to verify RED**

Run: `mvn -pl ruoyi-wechat-library -am -Dtest=DocumentAccessServiceTest,VipCodeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL，原因是 `accessType`、`VipCodeService` 或 `WlVipCode` 尚未实现。

### Task 2: 后端实现

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlVipCode.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlVipCodeMapper.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipCodeService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipEntitlementService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlDocument.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DocumentSummaryDto.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DocumentUploadCommitRequest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentUploadService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentAccessService.java`
- Create: `ruoyi-wechat-library/src/main/resources/mapper/library/WlVipCodeMapper.xml`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentMapper.xml`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentUnlockMapper.xml`

- [ ] **Step 1: Implement minimal backend code**

会员码状态使用 `UNUSED/USED/DISABLED`，过期通过 `expires_time` 判断。文档访问方式使用 `POINT/VIP_FREE`。

- [ ] **Step 2: Run focused tests to verify GREEN**

Run: `mvn -pl ruoyi-wechat-library -am -Dtest=DocumentAccessServiceTest,VipCodeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS。

### Task 3: 接口、SQL、前端入口

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipCodeController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxVipCodeController.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipCodeGenerateRequest.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipCodeBatchResult.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipCodeRedeemRequest.java`
- Create: `docs/2026-07-24-vip-code-and-vip-free-document.sql`
- Modify: `ruoyi-ui/src/api/library/vip.js`
- Modify: `ruoyi-ui/src/views/library/content/document/index.vue`
- Modify: `miniprogram/services/vip.js`
- Modify: `miniprogram/pages/redeem-course/redeem-course.js`
- Modify: `miniprogram/pages/redeem-course/redeem-course.wxml`
- Modify: `miniprogram/pages/redeem-course/redeem-course.wxss`
- Modify: `miniprogram/pages/profile/profile.wxml`
- Modify: `miniprogram/pages/document-detail/document-detail.js`
- Modify: `miniprogram/pages/document-detail/document-detail.wxml`

- [ ] **Step 1: Add interfaces and UI bindings**

后台文档表单增加“访问方式”；小程序“兑换课程码”改“会员兑换”；个人中心移除“我的课程”，把意见反馈和联系客服放入菜单列表。

- [ ] **Step 2: Run targeted frontend/miniprogram tests**

Run: `npm test -- --runInBand miniprogram/tests/document-access.test.js miniprogram/tests/navigation.test.js`

Expected: PASS, or run the project’s equivalent targeted test command if package scripts differ.

### Task 4: Final verification

**Files:**
- Review all changed files

- [ ] **Step 1: Run relevant backend tests**

Run with Java 8 and IntelliJ Maven where available:
`mvn -pl ruoyi-wechat-library,ruoyi-admin -am -Dtest=DocumentAccessServiceTest,VipCodeServiceTest,LibraryMapperXmlContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 2: Review git diff**

Run: `git diff --check` and `git status --short`

- [ ] **Step 3: Commit only this task’s files**

Commit message: `feat: add vip code redemption and vip free documents`
