# 管理端协议操作列修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为所有协议版本提供查看操作，并让已发布和已替代协议通过只读弹窗查看完整内容。

**Architecture:** 仅修改管理端协议页面，复用列表已返回的协议完整数据和现有表单弹窗。页面增加只读状态，查看时禁用全部表单控件并隐藏保存入口；草稿协议原有修改和发布流程保持不变。

**Tech Stack:** Vue 2、Element UI、Node.js、`vue-template-compiler`

---

### Task 1：协议操作列和只读查看模式

**Files:**
- Create: `ruoyi-ui/tests/agreement-operation-column.test.js`
- Modify: `ruoyi-ui/src/views/library/agreement/index.vue`

- [x] **Step 1：编写失败的页面契约测试**

创建 `ruoyi-ui/tests/agreement-operation-column.test.js`：

```js
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/library/agreement/index.vue'),
  'utf8'
)

assert(page.includes('@click="view(s.row)">查看</el-button>'),
  '所有协议行都应提供查看操作')
assert(page.includes("v-if=\"s.row.status === '0'\""),
  '修改和发布操作应继续仅对草稿显示')
assert(page.includes('readOnly: false'),
  '协议页面应维护只读查看状态')
assert(page.includes("readOnly ? '查看协议版本'"),
  '查看模式应显示查看协议版本标题')
assert(page.includes('view(row)'),
  '协议页面应提供查看方法')
assert(page.includes('this.readOnly = true'),
  '查看方法应启用只读模式')
assert(page.includes('this.readOnly = false'),
  '新增和修改时应退出只读模式')
assert((page.match(/:disabled="readOnly"/g) || []).length === 5,
  '查看模式应禁用全部五个协议表单控件')
assert(page.includes('v-if="readOnly" @click="visible = false">关闭</el-button>'),
  '查看模式应只提供关闭操作')
assert(page.includes('<template v-else>'),
  '非查看模式应保留取消和保存草稿操作')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `协议管理页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('协议操作列与只读查看契约测试通过')
```

- [x] **Step 2：运行测试并确认旧实现失败**

Run:

```powershell
node ruoyi-ui/tests/agreement-operation-column.test.js
```

Expected: FAIL，错误信息包含“所有协议行都应提供查看操作”。

- [x] **Step 3：实现查看操作和只读弹窗**

在 `ruoyi-ui/src/views/library/agreement/index.vue` 中将操作列改为：

```vue
<el-table-column label="操作" width="200">
  <template slot-scope="s">
    <el-button type="text" @click="view(s.row)">查看</el-button>
    <el-button
      v-if="s.row.status === '0'"
      v-hasPermi="['library:agreement:edit']"
      type="text"
      @click="open(s.row)"
    >修改</el-button>
    <el-button
      v-if="s.row.status === '0'"
      v-hasPermi="['library:agreement:publish']"
      type="text"
      @click="publish(s.row)"
    >发布</el-button>
  </template>
</el-table-column>
```

将弹窗标题和表单改为：

```vue
<el-dialog
  :title="readOnly ? '查看协议版本' : form.id ? '修改协议版本' : '新增协议版本'"
  :visible.sync="visible"
  width="760px"
>
  <el-form :model="form" label-width="100px">
    <el-form-item label="协议类型" required>
      <el-radio-group v-model="form.agreementType" :disabled="readOnly">
        <el-radio label="PRIVACY">用户隐私协议</el-radio>
        <el-radio label="STATEMENT">网站声明</el-radio>
        <el-radio label="FILE_DISCLAIMER">文件发送免责声明</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="版本" required>
      <el-input v-model="form.version" :disabled="readOnly"/>
    </el-form-item>
    <el-form-item label="标题" required>
      <el-input v-model="form.title" :disabled="readOnly"/>
    </el-form-item>
    <el-form-item label="协议内容" required>
      <el-input v-model="form.content" type="textarea" :rows="12" :disabled="readOnly"/>
    </el-form-item>
    <el-form-item label="生效时间" required>
      <el-date-picker
        v-model="form.effectiveTime"
        type="datetime"
        value-format="yyyy-MM-dd HH:mm:ss"
        :disabled="readOnly"
      />
    </el-form-item>
  </el-form>
  <span slot="footer">
    <el-button v-if="readOnly" @click="visible = false">关闭</el-button>
    <template v-else>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存草稿</el-button>
    </template>
  </span>
</el-dialog>
```

将页面状态增加为：

```js
data() {
  return {
    loading: false,
    rows: [],
    visible: false,
    readOnly: false,
    form: {}
  }
},
```

增加查看方法，并保证新增或修改时退出只读模式：

```js
view(row) {
  this.readOnly = true
  this.form = { ...row }
  this.visible = true
},
open(row) {
  this.readOnly = false
  this.form = row
    ? { ...row }
    : {
      agreementType: 'PRIVACY',
      version: '',
      title: '',
      content: '',
      effectiveTime: '',
      status: '0'
    }
  this.visible = true
},
```

- [x] **Step 4：复跑页面契约测试**

Run:

```powershell
node ruoyi-ui/tests/agreement-operation-column.test.js
```

Expected: PASS，输出“协议操作列与只读查看契约测试通过”。

- [x] **Step 5：运行现有协议页面样式测试**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' ruoyi-ui/tests/library-page-table-style.test.js
```

Expected: PASS，Node 测试报告显示 3 项测试全部通过。

- [x] **Step 6：检查变更范围和格式**

Run:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` 无错误；本任务只新增契约测试、修改协议页面并新增本实施计划，其他已有工作区改动保持不变。

- [ ] **Step 7：提交本次修复**

```powershell
git add -- ruoyi-ui/tests/agreement-operation-column.test.js ruoyi-ui/src/views/library/agreement/index.vue docs/superpowers/plans/2026-07-29-agreement-operation-column.md
git commit -m "fix: add agreement view operation"
```
