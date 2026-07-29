# 小程序首页轮播图跳转修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让首页轮播图根据接口返回的 `documentId` 跳转到对应文档详情页。

**Architecture:** 跳转规则保留在小程序 `promotion-strip` 组件中，组件校验关联文档编号后构造详情页 URL。后端接口、首页布局和轮播数据结构保持不变。

**Tech Stack:** 微信小程序 CommonJS、Node.js `node:test`

---

### Task 1: 建立轮播图点击契约

**Files:**
- Create: `miniprogram/tests/banner-navigation.test.js`
- Modify: `miniprogram/components/promotion-strip/index.js`

- [ ] **Step 1: 编写失败测试**

创建 `miniprogram/tests/banner-navigation.test.js`，使用 `vm` 加载真实组件脚本并捕获组件定义：

```js
const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

function loadComponent() {
  const source = fs.readFileSync(
    path.resolve(__dirname, '../components/promotion-strip/index.js'),
    'utf8'
  )
  let definition
  const navigations = []
  vm.runInNewContext(source, {
    Component: value => { definition = value },
    wx: { navigateTo: options => navigations.push(options) }
  })
  return { definition, navigations }
}

test('点击轮播图跳转到关联文档详情页', () => {
  const { definition, navigations } = loadComponent()
  definition.methods.open({
    currentTarget: { dataset: { item: { documentId: 12 } } }
  })
  assert.deepEqual(navigations, [
    { url: '/pages/document-detail/document-detail?id=12' }
  ])
})

test('关联文档编号无效时不发起跳转', () => {
  const invalidValues = [undefined, null, '', 0, -1, 'abc']
  invalidValues.forEach(documentId => {
    const { definition, navigations } = loadComponent()
    definition.methods.open({
      currentTarget: { dataset: { item: { documentId } } }
    })
    assert.equal(navigations.length, 0)
  })
})
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/banner-navigation.test.js
```

Expected: 有效 `documentId` 用例 FAIL，因为现有实现只读取 `linkPath`。

- [ ] **Step 3: 实现最小跳转逻辑**

将 `miniprogram/components/promotion-strip/index.js` 的 `open` 方法改为：

```js
open(event) {
  const item = event.currentTarget.dataset.item;
  const documentId = Number(item && item.documentId);
  if (!Number.isInteger(documentId) || documentId <= 0) return;
  wx.navigateTo({ url: `/pages/document-detail/document-detail?id=${documentId}` });
}
```

- [ ] **Step 4: 运行测试并确认通过**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/banner-navigation.test.js
```

Expected: 2 tests PASS。

### Task 2: 相关回归验证与提交

**Files:**
- Test: `miniprogram/tests/banner-navigation.test.js`
- Test: `miniprogram/tests/document-access.test.js`
- Test: `miniprogram/tests/navigation.test.js`
- Modify: `miniprogram/components/promotion-strip/index.js`
- Create: `docs/superpowers/plans/2026-07-29-miniprogram-banner-navigation-fix.md`

- [ ] **Step 1: 运行首页、文档和导航测试**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/banner-navigation.test.js miniprogram/tests/document-access.test.js miniprogram/tests/navigation.test.js
```

Expected: 相关测试全部 PASS。

- [ ] **Step 2: 检查差异范围**

Run:

```powershell
git diff --check -- miniprogram/components/promotion-strip/index.js miniprogram/tests/banner-navigation.test.js docs/superpowers/plans/2026-07-29-miniprogram-banner-navigation-fix.md
git status --short
```

确认首页 `index.wxml`、`index.wxss` 及其他用户改动没有被本任务修改或暂存。

- [ ] **Step 3: 单独提交修复**

Run:

```powershell
git add -- miniprogram/components/promotion-strip/index.js miniprogram/tests/banner-navigation.test.js docs/superpowers/plans/2026-07-29-miniprogram-banner-navigation-fix.md
git commit -m "fix: 修复首页轮播图文档跳转"
```

只提交以上三个文件，不包含当前工作区中的其他改动。
