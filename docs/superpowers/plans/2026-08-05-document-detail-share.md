# 文档详情页分享 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在文档详情页开启右上角好友转发和朋友圈分享，并让分享内容回到当前文档详情页。

**Architecture:** 只修改小程序详情页和新增静态契约测试。页面使用现有 `data.id`、`data.document.title` 和 `data.document.coverUrl` 组装微信原生分享返回值，不增加后端请求、数据库字段或权限逻辑。

**Tech Stack:** 微信小程序 Page API、Node.js 内置 `node:test`、Node.js `vm`。

---

### Task 1: 为页面分享行为写失败测试

**Files:**
- Create: `miniprogram/tests/document-share.test.js`
- Reference: `miniprogram/pages/document-detail/document-detail.js`

- [ ] **Step 1: Write the failing test**

创建测试加载详情页 `Page` 定义，验证页面加载时配置 `shareAppMessage` 和 `shareTimeline`，好友分享返回 `/pages/document-detail/document-detail?id=42`，朋友圈分享返回 `id=42`，标题和封面来自当前文档，缺少文档时标题使用兜底值。

测试使用 `vm.runInNewContext` 注入 `Page`、`wx` 和现有模块的最小 stub，不调用真实网络请求：

```javascript
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function loadPage() {
  const source = fs.readFileSync(
    path.resolve(__dirname, '../pages/document-detail/document-detail.js'),
    'utf8'
  );
  let definition;
  const shareMenus = [];
  vm.runInNewContext(source, {
    Page: value => { definition = value; },
    wx: { showShareMenu: options => shareMenus.push(options) },
    require: () => ({})
  });
  return { definition, shareMenus };
}

function pageContext(data) {
  return {
    data,
    setData(values) { this.data = { ...this.data, ...values }; },
    load() {}
  };
}

test('文档详情页右上角同时开启好友和朋友圈分享', () => {
  const { definition, shareMenus } = loadPage();
  const page = pageContext({ id: '', document: null });

  definition.onLoad.call(page, { id: '42' });

  assert.deepEqual(JSON.parse(JSON.stringify(shareMenus)), [
    { menus: ['shareAppMessage', 'shareTimeline'] }
  ]);
  assert.equal(typeof definition.onShareAppMessage, 'function');
  assert.equal(typeof definition.onShareTimeline, 'function');
});

test('文档详情页分享当前文档并在缺少文档时使用兜底标题', () => {
  const { definition } = loadPage();
  const page = pageContext({
    id: '42',
    document: { title: '测试文档', coverUrl: 'https://cdn.example.test/cover.jpg' }
  });

  assert.deepEqual(JSON.parse(JSON.stringify(definition.onShareAppMessage.call(page))), {
    title: '测试文档',
    path: '/pages/document-detail/document-detail?id=42',
    imageUrl: 'https://cdn.example.test/cover.jpg'
  });
  assert.deepEqual(JSON.parse(JSON.stringify(definition.onShareTimeline.call(page))), {
    title: '测试文档',
    query: 'id=42',
    imageUrl: 'https://cdn.example.test/cover.jpg'
  });

  page.data.document = null;
  assert.equal(definition.onShareAppMessage.call(page).title, '文档详情');
  assert.equal(definition.onShareTimeline.call(page).title, '文档详情');
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run from `miniprogram` using the bundled Node runtime:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test tests/document-share.test.js
```

Expected: FAIL because the current detail page has no share menu configuration or share callbacks.

### Task 2: 实现详情页右上角分享

**Files:**
- Modify: `miniprogram/pages/document-detail/document-detail.js:29-34`
- Test: `miniprogram/tests/document-share.test.js`

- [ ] **Step 1: Add the share menu and callbacks**

在 `onLoad` 开头增加：

```javascript
wx.showShareMenu({
  menus: ['shareAppMessage', 'shareTimeline']
});
```

在详情页对象中增加：

```javascript
onShareAppMessage() {
  const document = this.data.document || {};
  return {
    title: document.title || '文档详情',
    path: `/pages/document-detail/document-detail?id=${this.data.id}`,
    imageUrl: document.coverUrl || ''
  };
},
onShareTimeline() {
  const document = this.data.document || {};
  return {
    title: document.title || '文档详情',
    query: `id=${this.data.id}`,
    imageUrl: document.coverUrl || ''
  };
},
```

不把 token、openid、解锁状态或原始文件 URL 放入分享路径和参数。

- [ ] **Step 2: Run the focused test to verify it passes**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test tests/document-share.test.js
```

Expected: PASS for both sharing tests.

- [ ] **Step 3: Run the related mini-program tests**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test tests/document-access.test.js tests/banner-navigation.test.js tests/document-share.test.js
```

Expected: all selected tests pass with no new warnings.

- [ ] **Step 4: Check the diff and commit only feature files**

```powershell
git diff --check -- miniprogram/pages/document-detail/document-detail.js miniprogram/tests/document-share.test.js docs/superpowers/specs/2026-08-05-document-detail-share-design.md docs/superpowers/plans/2026-08-05-document-detail-share.md
git add -- miniprogram/pages/document-detail/document-detail.js miniprogram/tests/document-share.test.js docs/superpowers/specs/2026-08-05-document-detail-share-design.md docs/superpowers/plans/2026-08-05-document-detail-share.md
git commit -m "feat: 开启文档详情页分享"
```

Do not stage the unrelated existing dirty files in this worktree.
