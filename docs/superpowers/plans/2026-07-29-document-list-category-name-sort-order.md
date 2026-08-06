# 文档列表分类名称和排序字段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 后台文档列表以分类名称替代分类编号，并增加文档排序列。

**Architecture:** 管理端文档列表查询使用左连接读取 `wl_category.name`，通过 `WlDocument.categoryName` 返回前端。表格直接展示 `categoryName` 和现有 `sortOrder`；文档保存字段、分类选择接口和数据库结构保持不变。

**Tech Stack:** Java 8、MyBatis、Vue 2、Element UI、JUnit 5、Node.js 契约测试

---

### Task 1: 写失败契约测试

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java`
- Modify: `ruoyi-ui/tests/document-category-select.test.js`

- [x] **Step 1: 断言管理列表 SQL 返回分类名称**

验证 `selectDocumentList` 使用 `left join wl_category`、选择 `c.name as category_name`，并继续按文档排序值排序。

- [x] **Step 2: 断言表格列**

验证文档表格包含 `categoryName/分类名称` 和 `sortOrder/排序`，不再显示 `categoryId/分类编号`。

- [x] **Step 3: 运行测试确认失败**

Run: IntelliJ Maven 执行 `LibraryMapperXmlContractTest`；运行 `node ruoyi-ui/tests/document-category-select.test.js`

Expected: FAIL，管理列表尚未关联分类名称，前端仍显示分类编号且缺少排序列。

### Task 2: 实现列表字段

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlDocument.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentMapper.xml`
- Modify: `ruoyi-ui/src/views/library/content/document/index.vue`

- [x] **Step 1: 增加分类名称只读属性**

为 `WlDocument` 增加 `categoryName` 及 getter/setter。

- [x] **Step 2: 修改管理列表 SQL**

为 `WlDocumentResult` 增加 `category_name` 映射，`selectDocumentList` 左连接分类表并返回分类名称；查询条件和排序字段使用文档表别名。

- [x] **Step 3: 修改表格列**

把分类编号列替换为分类名称列，在其后新增排序列。

- [x] **Step 4: 运行测试确认通过**

Run: IntelliJ Maven 执行 `LibraryMapperXmlContractTest`；运行 `node ruoyi-ui/tests/document-category-select.test.js`

Expected: PASS。

### Task 3: 回归验证

**Files:**
- Verify: all files above

- [x] **Step 1: 运行文档相关前端测试**

Run: `node ruoyi-ui/tests/document-upload-flow.test.js`

Expected: PASS。

- [x] **Step 2: 检查差异**

Run: `git diff --check`

Expected: 无新增空白错误。
