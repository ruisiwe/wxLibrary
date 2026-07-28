# Library Page and Table Style Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle every WeChat library management list page individually so single-function pages have no redundant title or outer card and all business tables match the borderless RuoYi list style.

**Architecture:** Add opt-in `plain` and `embedded` presentation flags to the library-only `SimpleList` component, then enable them explicitly in each consumer. Update every directly-authored library table page in controlled groups, preserving multi-section headings and all existing data, permission, dialog, and pagination behavior.

**Tech Stack:** Vue 2, Element UI, RuoYi `right-toolbar`, Node.js static contract tests, `vue-template-compiler`.

---

## File Structure

### New tests

- `ruoyi-ui/tests/library-simple-list-style.test.js`
  - Verifies the opt-in plain list component contract and every explicit consumer.
- `ruoyi-ui/tests/library-page-table-style.test.js`
  - Verifies direct library tables, standard toolbars, retained section headings, and Vue template compilation.

### Modified common component

- `ruoyi-ui/src/views/library/common/SimpleList.vue`
  - Adds opt-in `plain` and `embedded` presentation without changing the default mode.

### Modified `SimpleList` consumers

- `ruoyi-ui/src/views/library/points/record/index.vue`
- `ruoyi-ui/src/views/library/points/rule/index.vue`
- `ruoyi-ui/src/views/library/vip/order/index.vue`
- `ruoyi-ui/src/views/library/vip/plan/index.vue`
- `ruoyi-ui/src/views/library/content/category/index.vue`
- `ruoyi-ui/src/views/library/vip/code/index.vue`
- `ruoyi-ui/src/views/library/access/courseCode/index.vue`

### Modified direct table pages

- `ruoyi-ui/src/views/library/user/index.vue`
- `ruoyi-ui/src/views/library/vip/entitlement/index.vue`
- `ruoyi-ui/src/views/library/vip/refund/index.vue`
- `ruoyi-ui/src/views/library/content/banner/index.vue`
- `ruoyi-ui/src/views/library/content/document/index.vue`
- `ruoyi-ui/src/views/library/content/video/index.vue`
- `ruoyi-ui/src/views/library/content/course/index.vue`
- `ruoyi-ui/src/views/library/agreement/index.vue`
- `ruoyi-ui/src/views/library/vip/benefit/index.vue`

No backend, API, database, SQL, environment, or mini-program file is part of this plan.

---

### Task 1: Add the Opt-In Plain `SimpleList` Presentation

**Files:**
- Create: `ruoyi-ui/tests/library-simple-list-style.test.js`
- Modify: `ruoyi-ui/src/views/library/common/SimpleList.vue`
- Modify: the seven `SimpleList` consumer files listed above

- [ ] **Step 1: Add the failing `SimpleList` style contract**

Create `ruoyi-ui/tests/library-simple-list-style.test.js`:

```javascript
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const componentPath = 'src/views/library/common/SimpleList.vue'
const plainPages = [
  'src/views/library/points/record/index.vue',
  'src/views/library/points/rule/index.vue',
  'src/views/library/vip/order/index.vue',
  'src/views/library/vip/plan/index.vue',
  'src/views/library/content/category/index.vue',
  'src/views/library/vip/code/index.vue',
  'src/views/library/access/courseCode/index.vue'
]
const embeddedPages = new Set([
  'src/views/library/vip/code/index.vue',
  'src/views/library/access/courseCode/index.vue'
])

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function compile(file, source) {
  const component = compiler.parseComponent(source)
  const result = compiler.compile(component.template.content)
  assert.deepStrictEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

function simpleListTag(source) {
  const match = source.match(/<simple-list\b[\s\S]*?\/>/i)
  assert(match, '页面必须包含 simple-list')
  return match[0]
}

const component = read(componentPath)
assert(component.includes("plain: { type: Boolean, default: false }"), 'SimpleList 必须提供显式 plain 开关')
assert(component.includes("embedded: { type: Boolean, default: false }"), 'SimpleList 必须提供显式 embedded 开关')
assert(component.includes(":is=\"plain ? 'div' : 'el-card'\""), 'plain 模式必须去掉最外层卡片')
assert(component.includes(':border="!plain"'), 'plain 模式必须关闭表格外框')
assert(component.includes(':stripe="!plain"'), 'plain 模式必须关闭斑马纹')
assert(component.includes('v-if="!plain" slot="header"'), 'plain 模式不得显示页面功能标题')
assert(component.includes('<right-toolbar :search="false" @queryTable="loadData"'), 'plain 模式必须使用右侧刷新工具栏')
compile(componentPath, component)

plainPages.forEach(file => {
  const source = read(file)
  const tag = simpleListTag(source)
  assert(/\splain(?:\s|\/|>)/.test(tag), `${file} 必须显式启用 plain`)
  if (embeddedPages.has(file)) {
    assert(/\sembedded(?:\s|\/|>)/.test(tag), `${file} 必须显式启用 embedded`)
  }
  compile(file, source)
})

console.log('资料库 SimpleList 逐页样式契约测试通过')
```

- [ ] **Step 2: Run the contract and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  ruoyi-ui/tests/library-simple-list-style.test.js
```

Expected: FAIL because `SimpleList` does not define `plain` and the pages do not enable it.

- [ ] **Step 3: Implement the opt-in component layout**

In `SimpleList.vue`, replace the outer card opening and header with:

```vue
<div :class="{ 'app-container': !embedded }">
  <component :is="plain ? 'div' : 'el-card'" shadow="never">
    <div v-if="!plain" slot="header" class="toolbar">
      <span>{{ title }}</span>
      <div>
        <el-button v-if="creator" v-hasPermi="[permissions.add]" type="primary" size="mini" icon="el-icon-plus" @click="openDialog()">新增</el-button>
        <el-button size="mini" icon="el-icon-refresh" @click="loadData">刷新</el-button>
      </div>
    </div>
    <el-row v-else :gutter="10" class="mb8">
      <el-col v-if="creator" :span="1.5">
        <el-button
          v-hasPermi="[permissions.add]"
          type="primary"
          plain
          size="mini"
          icon="el-icon-plus"
          @click="openDialog()"
        >新增</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="loadData" />
    </el-row>
```

Change the table opening to:

```vue
<el-table
  v-loading="loading"
  :data="rows"
  :border="!plain"
  :stripe="!plain"
>
```

Close the dynamic component with:

```vue
</component>
```

Add these props without changing the existing props:

```javascript
plain: { type: Boolean, default: false },
embedded: { type: Boolean, default: false },
```

- [ ] **Step 4: Enable `plain` page by page**

Add the boolean `plain` attribute to each of the seven `simple-list` tags.

For direct component pages, use this shape:

```vue
<template>
  <simple-list
    plain
    title="积分流水"
    :loader="listPointRecords"
    :columns="columns"
  />
</template>
```

For pages already wrapped in `app-container`, use both flags:

```vue
<simple-list
  plain
  embedded
  class="mt16"
  title="会员码记录（仅显示掩码）"
  :loader="listVipCodes"
  :columns="columns"
/>
```

Apply the same `plain embedded` shape to the course-code page.

In the dirty `vip/plan/index.vue`, add only the `plain` attribute to the existing opening tag. Do not reformat or replace the user's other changes.

- [ ] **Step 5: Run the contract and verify GREEN**

Run the Step 2 command.

Expected: `资料库 SimpleList 逐页样式契约测试通过`.

- [ ] **Step 6: Commit the opt-in list presentation**

Stage only the test, component, and seven consumer pages:

```powershell
git add -- `
  ruoyi-ui/tests/library-simple-list-style.test.js `
  ruoyi-ui/src/views/library/common/SimpleList.vue `
  ruoyi-ui/src/views/library/points/record/index.vue `
  ruoyi-ui/src/views/library/points/rule/index.vue `
  ruoyi-ui/src/views/library/vip/order/index.vue `
  ruoyi-ui/src/views/library/vip/plan/index.vue `
  ruoyi-ui/src/views/library/content/category/index.vue `
  ruoyi-ui/src/views/library/vip/code/index.vue `
  ruoyi-ui/src/views/library/access/courseCode/index.vue
git commit -m "style: add opt-in plain library lists"
```

Before committing, inspect the staged `vip/plan/index.vue` diff and confirm it contains only the new `plain` attribute in addition to the user's already-existing unstaged work. If selective line staging cannot isolate the task from the user's changes, leave that file uncommitted and report it separately rather than staging unrelated lines.

---

### Task 2: Restyle the Standard Direct Table Pages

**Files:**
- Create: `ruoyi-ui/tests/library-page-table-style.test.js`
- Modify:
  - `ruoyi-ui/src/views/library/user/index.vue`
  - `ruoyi-ui/src/views/library/vip/entitlement/index.vue`
  - `ruoyi-ui/src/views/library/content/banner/index.vue`
  - `ruoyi-ui/src/views/library/content/document/index.vue`

- [ ] **Step 1: Add grouped failing contracts**

Create `ruoyi-ui/tests/library-page-table-style.test.js`:

```javascript
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const test = require('node:test')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function compile(file, source) {
  const component = compiler.parseComponent(source)
  const result = compiler.compile(component.template.content)
  assert.deepStrictEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

function assertBorderlessTables(file) {
  const source = read(file)
  const component = compiler.parseComponent(source)
  const tags = component.template.content.match(/<el-table\b[\s\S]*?>/g) || []
  assert(tags.length > 0, `${file} 必须包含业务表格`)
  tags.forEach(tag => {
    assert(!/\sborder(?:\s|=|>)/.test(tag), `${file} 表格不得启用 border`)
    assert(!/\sstripe(?:\s|=|>)/.test(tag), `${file} 表格不得启用 stripe`)
  })
  compile(file, source)
  return source
}

test('标准资料库列表页使用无边框表格和右侧工具栏', () => {
  const files = [
    'src/views/library/user/index.vue',
    'src/views/library/vip/entitlement/index.vue',
    'src/views/library/content/banner/index.vue',
    'src/views/library/content/document/index.vue'
  ]
  files.forEach(file => {
    const source = assertBorderlessTables(file)
    assert(source.includes('class="mb8"'), `${file} 必须使用标准操作行`)
    assert(source.includes('<right-toolbar'), `${file} 必须提供右侧刷新工具栏`)
  })
})

test('紧凑资料库列表页使用无边框表格和右侧工具栏', () => {
  const files = [
    'src/views/library/vip/refund/index.vue',
    'src/views/library/content/video/index.vue',
    'src/views/library/content/course/index.vue',
    'src/views/library/agreement/index.vue'
  ]
  files.forEach(file => {
    const source = assertBorderlessTables(file)
    assert(source.includes('class="mb8"'), `${file} 必须使用标准操作行`)
    assert(source.includes('<right-toolbar'), `${file} 必须提供右侧刷新工具栏`)
  })
})

test('VIP权益多配置区保留分区标题并移除表格边框', () => {
  const file = 'src/views/library/vip/benefit/index.vue'
  const source = assertBorderlessTables(file)
  assert(!/<h2[^>]*>VIP 权益介绍<\/h2>/.test(source), '多配置区页面不得保留重复总标题')
  assert(source.includes('<span>客服微信配置</span>'), '必须保留客服微信配置分区标题')
  assert(source.includes('<span>权益文字列表</span>'), '必须保留权益文字列表分区标题')
})
```

- [ ] **Step 2: Run the standard-page contract and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  --test --test-name-pattern="标准资料库列表页" `
  ruoyi-ui/tests/library-page-table-style.test.js
```

Expected: FAIL on existing `border`, `stripe`, or missing `right-toolbar`.

- [ ] **Step 3: Update the user and entitlement pages**

In `library/user/index.vue`, add immediately before the table:

```vue
<el-row :gutter="10" class="mb8">
  <right-toolbar :search="false" @queryTable="load" />
</el-row>
```

Change its table opening to:

```vue
<el-table v-loading="loading" :data="rows">
```

In `library/vip/entitlement/index.vue`, change the existing action row to `class="mb8"`, use `:span="1.5"` for both action columns, set both buttons to `plain size="mini"`, and add:

```vue
<right-toolbar :search="false" @queryTable="load" />
```

Change the entitlement table to:

```vue
<el-table :data="rows">
```

- [ ] **Step 4: Update the banner and document pages**

In `library/content/banner/index.vue`:

- add `v-show="showSearch"` to the query form;
- add `<right-toolbar :showSearch.sync="showSearch" @queryTable="load" />` inside the existing `mb8` row;
- add `showSearch: true` beside the existing loading/list state;
- remove the `border` and `stripe` attributes from the table.

The toolbar tail must be:

```vue
      <right-toolbar :showSearch.sync="showSearch" @queryTable="load" />
    </el-row>
```

In `library/content/document/index.vue`, change the existing add button to:

```vue
<el-col :span="1.5">
  <el-button
    v-if="$auth.hasPermiAnd(['library:document:add', 'library:document:upload'])"
    type="primary"
    plain
    size="mini"
    icon="el-icon-plus"
    @click="open()"
  >新增</el-button>
</el-col>
<right-toolbar :search="false" @queryTable="load" />
```

Change the document table opening to:

```vue
<el-table v-loading="loading" :data="rows">
```

- [ ] **Step 5: Run the standard-page contract and verify GREEN**

Run the Step 2 command.

Expected: one passing test and the other groups reported as skipped by the name pattern.

- [ ] **Step 6: Commit the standard pages**

```powershell
git add -- `
  ruoyi-ui/tests/library-page-table-style.test.js `
  ruoyi-ui/src/views/library/user/index.vue `
  ruoyi-ui/src/views/library/vip/entitlement/index.vue `
  ruoyi-ui/src/views/library/content/banner/index.vue `
  ruoyi-ui/src/views/library/content/document/index.vue
git commit -m "style: align library list page tables"
```

---

### Task 3: Restyle the Compact Direct Table Pages

**Files:**
- Modify:
  - `ruoyi-ui/src/views/library/vip/refund/index.vue`
  - `ruoyi-ui/src/views/library/content/video/index.vue`
  - `ruoyi-ui/src/views/library/content/course/index.vue`
  - `ruoyi-ui/src/views/library/agreement/index.vue`
- Test: `ruoyi-ui/tests/library-page-table-style.test.js`

- [ ] **Step 1: Run the compact-page contract and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  --test --test-name-pattern="紧凑资料库列表页" `
  ruoyi-ui/tests/library-page-table-style.test.js
```

Expected: FAIL because these four tables still use `border` and lack the standard operation row.

- [ ] **Step 2: Update refund and agreement pages**

At the top of each page container, create a standard action row.

Refund:

```vue
<el-row :gutter="10" class="mb8">
  <right-toolbar :search="false" @queryTable="load" />
</el-row>
<el-table :data="rows">
```

Agreement:

```vue
<el-row :gutter="10" class="mb8">
  <el-col :span="1.5">
    <el-button
      v-hasPermi="['library:agreement:add']"
      type="primary"
      plain
      size="mini"
      icon="el-icon-plus"
      @click="open()"
    >新增协议版本</el-button>
  </el-col>
  <right-toolbar :search="false" @queryTable="load" />
</el-row>
<el-table v-loading="loading" :data="rows">
```

Remove the old standalone agreement button and the table `mt16` class. Preserve all dialog and service code.

- [ ] **Step 3: Update course and video pages**

Course page operation row:

```vue
<el-row :gutter="10" class="mb8">
  <el-col :span="1.5">
    <el-button
      v-hasPermi="['library:course:add']"
      type="primary"
      plain
      size="mini"
      icon="el-icon-plus"
      @click="openDialog()"
    >新增课程</el-button>
  </el-col>
  <right-toolbar :search="false" @queryTable="load" />
</el-row>
<el-table v-loading="loading" :data="rows">
```

Remove the old standalone course button and the table `border` and `mt16`.

For the video page:

- keep the course selector query form;
- set the form to `:inline="true" size="small"`;
- leave the add-video button inside that form;
- add the following row between the form and table:

```vue
<el-row :gutter="10" class="mb8">
  <right-toolbar :search="false" @queryTable="loadVideos" />
</el-row>
```

- change the table to `<el-table v-loading="loading" :data="videos">`.

- [ ] **Step 4: Run the compact-page contract and verify GREEN**

Run the Step 1 command.

Expected: the compact-page test passes.

- [ ] **Step 5: Commit the compact pages**

```powershell
git add -- `
  ruoyi-ui/src/views/library/vip/refund/index.vue `
  ruoyi-ui/src/views/library/content/video/index.vue `
  ruoyi-ui/src/views/library/content/course/index.vue `
  ruoyi-ui/src/views/library/agreement/index.vue
git commit -m "style: align remaining library tables"
```

---

### Task 4: Preserve VIP Benefit Section Titles Without the Page Heading

**Files:**
- Modify: `ruoyi-ui/src/views/library/vip/benefit/index.vue`
- Test: `ruoyi-ui/tests/library-page-table-style.test.js`

- [ ] **Step 1: Run the multi-configuration contract and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  --test --test-name-pattern="VIP权益多配置区" `
  ruoyi-ui/tests/library-page-table-style.test.js
```

Expected: FAIL because the page still renders the repeated `VIP 权益介绍` heading and the benefit table has `border stripe`.

- [ ] **Step 2: Remove only the redundant total heading**

Delete:

```vue
<h2 class="page-title">VIP 权益介绍</h2>
```

Do not remove:

```vue
<span>客服微信配置</span>
<span>权益文字列表</span>
```

Remove the now-unused `.page-title` CSS rule, leaving the config-card and benefit-card spacing rules intact.

- [ ] **Step 3: Remove the benefit table border and stripe**

Change:

```vue
<el-table v-loading="benefitLoading" :data="benefits" border stripe>
```

to:

```vue
<el-table v-loading="benefitLoading" :data="benefits">
```

Do not change the two section cards or the `el-descriptions`/form behavior.

- [ ] **Step 4: Run the multi-configuration contract and verify GREEN**

Run the Step 1 command.

Expected: the multi-configuration test passes.

- [ ] **Step 5: Commit the VIP benefit page**

```powershell
git add -- ruoyi-ui/src/views/library/vip/benefit/index.vue
git commit -m "style: simplify VIP benefit page heading"
```

---

### Task 5: Full UI Verification and Repository Hygiene

**Files:**
- Verify only; do not stage build output or unrelated user work.

- [ ] **Step 1: Run both new style contracts**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  ruoyi-ui/tests/library-simple-list-style.test.js
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  --test ruoyi-ui/tests/library-page-table-style.test.js
```

Expected: both scripts pass and all three direct-page test groups are green.

- [ ] **Step 2: Run all management UI contract tests**

Run every `ruoyi-ui/tests/*.test.js` with the bundled Node runtime.

Expected:

- both new library style contracts pass;
- existing VIP and document tests pass;
- the known unrelated banner crop ratio test may continue to fail on `112:55` versus current `952:550`, and must be reported separately without changing it.

- [ ] **Step 3: Run the production build**

Run:

```powershell
npm run build:prod
```

Working directory: `ruoyi-ui`.

Expected: build succeeds. Existing asset-size warnings are acceptable; Vue compilation errors are not.

- [ ] **Step 4: Verify table and title boundaries**

Run:

```powershell
rg -n '<el-table[^>]*(border|stripe)|^[[:space:]]+(border|stripe)$' ruoyi-ui/src/views/library
rg -n '<h2[^>]*>VIP 权益介绍</h2>|slot="header" class="toolbar"' ruoyi-ui/src/views/library
```

Expected:

- no business list table enables static `border` or `stripe`;
- `SimpleList` may contain dynamic `:border="!plain"` and `:stripe="!plain"`;
- `SimpleList` retains its non-plain compatibility header behind `v-if="!plain"`;
- no page renders the redundant VIP total heading;
- dialog descriptions using `border` remain allowed.

- [ ] **Step 5: Verify repository hygiene**

```powershell
git diff --check
git status --short
git status --ignored --short ruoyi-ui/dist
git diff --cached --name-only
```

Confirm:

- `ruoyi-ui/dist` remains ignored and unstaged;
- no environment or sensitive configuration file was read or staged;
- no backend, SQL, deployment, migration, or data operation occurred;
- mini-program changes and `DocumentService.java` remain untouched;
- user changes in `vip/plan/index.vue` were not overwritten or accidentally included beyond the intended attribute.
