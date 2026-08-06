# 首页分类区与文档列表间隙调整 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将首页分类宫格与文档列表之间的底部间距由 `28rpx` 缩小为 `12rpx`，并用自动化测试固定该视觉契约。

**Architecture:** 复用现有 `document-access.test.js` 对首页 WXML/WXSS 的静态契约检查，不引入新的测试基础设施。实现仅修改首页 `category-grid` 选择器的底部外边距，不调整分类组件内部布局、文档列表布局或后端代码。

**Tech Stack:** 微信小程序 WXML/WXSS、Node.js 24、`node:test`

---

### Task 1: 用测试固定首页区块间距

**Files:**
- Modify: `miniprogram/tests/document-access.test.js`
- Test: `miniprogram/tests/document-access.test.js`

- [x] **Step 1: 在现有首页设计测试中加入失败断言**

在测试 `首页按设计草图展示小程序名称和专题推荐标题` 的末尾加入：

```js
  assert.match(style,
    /\.home category-grid\s*\{[^}]*margin:\s*0\s+0\s+12rpx[^}]*\}/s,
    '分类宫格与文档列表之间应保留 12rpx 间距');
```

- [x] **Step 2: 运行单个测试文件，确认新断言先失败**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/document-access.test.js
```

Expected: FAIL，错误信息包含 `分类宫格与文档列表之间应保留 12rpx 间距`，因为当前样式仍为 `margin: 0 0 28rpx`。

### Task 2: 实现最小样式修改

**Files:**
- Modify: `miniprogram/pages/index/index.wxss:89`
- Test: `miniprogram/tests/document-access.test.js`

- [x] **Step 1: 将分类宫格底部外边距改为 12rpx**

把现有规则改为：

```css
.home category-grid {
  display: block;
  margin: 0 0 12rpx
}
```

- [x] **Step 2: 重新运行单个测试文件，确认测试通过**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/document-access.test.js
```

Expected: PASS，文件内全部测试通过。

### Task 3: 回归检查与交付

**Files:**
- Verify: `miniprogram/pages/index/index.wxss`
- Verify: `miniprogram/tests/document-access.test.js`

- [x] **Step 1: 运行首页导航相关回归测试**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/navigation.test.js miniprogram/tests/document-access.test.js
```

Expected: PASS，两个测试文件内全部测试通过。

- [x] **Step 2: 检查补丁格式与任务文件差异**

Run:

```powershell
git diff --check -- miniprogram/pages/index/index.wxss miniprogram/tests/document-access.test.js
git diff -- miniprogram/pages/index/index.wxss miniprogram/tests/document-access.test.js
git status --short -- miniprogram/pages/index/index.wxss miniprogram/tests/document-access.test.js docs/superpowers/specs/2026-08-05-home-category-document-gap-design.md docs/superpowers/plans/2026-08-05-home-category-document-gap.md
```

Expected: `git diff --check` 无输出；差异仅包含原有未提交内容、本次 `28rpx` 到 `12rpx` 的修改和对应测试断言。保持所有文件未暂存，不创建提交。
