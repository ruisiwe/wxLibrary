# 文档分类下拉选择 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后台新增和修改文档表单中的分类编号输入框改为无需搜索的分类名称下拉框。

**Architecture:** 文档模块提供独立的分类选项接口，权限与文档新增、修改操作绑定，避免依赖分类管理权限。接口默认只返回启用分类；修改文档时可额外带回当前已停用分类用于正确回显，但停用项不可重新选择。前端继续提交原有 `categoryId`，不修改数据库和文档保存协议。

**Tech Stack:** Java 8、Spring MVC、MyBatis、Vue 2、Element UI、Node.js 契约测试、JUnit 5/Mockito

---

### Task 1: 文档分类选项接口

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDocumentController.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java`
- Test: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryDocumentControllerTest.java`

- [x] **Step 1: 写失败测试**

测试分类选项只包含启用分类；传入当前停用分类编号时将其追加用于回显。控制器测试接口响应和 `library:document:add,library:document:edit` 权限声明。

- [x] **Step 2: 运行测试确认失败**

Run: IntelliJ Maven 执行 `DocumentServiceTest` 与 `LibraryDocumentControllerTest`

Expected: FAIL，缺少文档分类选项服务和控制器接口。

- [x] **Step 3: 实现最小接口**

在 `DocumentService` 中查询启用分类，并在需要时追加当前分类；在 `LibraryDocumentController` 暴露 `GET /library/document/category-options`。

- [x] **Step 4: 运行测试确认通过**

Run: IntelliJ Maven 执行 `DocumentServiceTest` 与 `LibraryDocumentControllerTest`

Expected: PASS。

### Task 2: 文档表单下拉选择

**Files:**
- Modify: `ruoyi-ui/src/api/library/content.js`
- Modify: `ruoyi-ui/src/views/library/content/document/index.vue`
- Create: `ruoyi-ui/tests/document-category-select.test.js`

- [x] **Step 1: 写失败契约测试**

断言表单使用无搜索功能的 `el-select`，选项显示分类名称并提交分类编号，同时不再出现分类编号数字输入框。

- [x] **Step 2: 运行测试确认失败**

Run: `node ruoyi-ui/tests/document-category-select.test.js`

Expected: FAIL，分类选择器和选项接口尚不存在。

- [x] **Step 3: 实现下拉加载和校验**

弹窗打开时加载分类选项；新增仅显示启用分类，修改时传入当前分类编号用于回显；停用项禁用。错误提示改为“请选择文档分类”。

- [x] **Step 4: 运行测试确认通过**

Run: `node ruoyi-ui/tests/document-category-select.test.js`

Expected: PASS。

### Task 3: 回归验证

**Files:**
- Verify: all files above

- [x] **Step 1: 运行文档上传前端契约测试**

Run: `node ruoyi-ui/tests/document-upload-flow.test.js`

Expected: PASS。

- [x] **Step 2: 检查差异**

Run: `git diff --check`

Expected: 无新增空白错误。
