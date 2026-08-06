# 文档列表缩略图正方形完整展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 所有文档列表中的缩略图统一以正方形完整展示，不再裁剪图片内容。

**Architecture:** 首页、分类、搜索、收藏和我的文档共用 `document-row` 组件。通过修改该组件的图片模式与尺寸一次性覆盖全部列表，并使用静态契约测试锁定展示规则。

**Tech Stack:** 微信小程序 WXML/WXSS、Node.js `node:test`

---

### Task 1: 锁定并实现缩略图展示规则

**Files:**
- Create: `miniprogram/tests/document-thumbnail-display.test.js`
- Modify: `miniprogram/components/document-row/index.wxml`
- Modify: `miniprogram/components/document-row/index.wxss`

- [ ] **Step 1: 编写失败的展示契约测试**

```javascript
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('文档列表缩略图使用正方形完整展示', () => {
  const template = fs.readFileSync(
    path.resolve(__dirname, '../components/document-row/index.wxml'),
    'utf8'
  );
  const style = fs.readFileSync(
    path.resolve(__dirname, '../components/document-row/index.wxss'),
    'utf8'
  );

  assert.match(template, /class="row__cover"[^>]*mode="aspectFit"/);
  assert.match(style, /\.row__cover\{[^}]*width:144rpx[^}]*height:144rpx/);
  assert.doesNotMatch(template, /mode="aspectFill"/);
});
```

- [ ] **Step 2: 运行测试并确认它因当前裁剪规则失败**

Run:

```powershell
node --test tests/document-thumbnail-display.test.js
```

Working directory: `miniprogram`

Expected: FAIL，因为模板仍为 `aspectFill`，样式高度仍为 `184rpx`。

- [ ] **Step 3: 实现最小模板与样式修改**

将 `miniprogram/components/document-row/index.wxml` 中图片改为：

```xml
<image class="row__cover" src="{{document.coverUrl}}" mode="aspectFit" />
```

将 `miniprogram/components/document-row/index.wxss` 中缩略图规则改为：

```css
.row__cover{width:144rpx;height:144rpx;border-radius:12rpx;background:#fff;flex:none}
```

- [ ] **Step 4: 运行单项测试并确认通过**

Run:

```powershell
node --test tests/document-thumbnail-display.test.js
```

Working directory: `miniprogram`

Expected: PASS，1 项测试通过。

- [ ] **Step 5: 运行全部小程序测试**

Run:

```powershell
npm test
```

Working directory: `miniprogram`

Expected: 全部测试通过，无失败项。

- [ ] **Step 6: 检查修改范围**

Run:

```powershell
git diff --check -- miniprogram/components/document-row/index.wxml miniprogram/components/document-row/index.wxss miniprogram/tests/document-thumbnail-display.test.js
git diff -- miniprogram/components/document-row/index.wxml miniprogram/components/document-row/index.wxss miniprogram/tests/document-thumbnail-display.test.js
```

Expected: 仅包含 `document-row` 的图片模式、正方形尺寸和对应测试；不修改其他业务文件。
