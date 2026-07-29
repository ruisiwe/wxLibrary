# 会员码生成套餐选择 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后台批量生成会员码弹窗的套餐编号数字输入改为仅展示启用会员套餐的下拉选择。

**Architecture:** 前端复用现有 `listVipPlans` 接口，在每次打开生成弹窗时按需加载启用套餐，并继续以 `planId` 调用现有会员码生成接口。后端接口、数据库和会员码记录表保持不变。

**Tech Stack:** Vue 2、Element UI、RuoYi 前端请求封装、Node.js、`vue-template-compiler`

---

### Task 1：会员码生成弹窗使用启用套餐下拉框

**Files:**
- Create: `ruoyi-ui/tests/vip-code-plan-select.test.js`
- Modify: `ruoyi-ui/src/views/library/vip/code/index.vue`

- [x] **Step 1：编写失败的页面契约测试**

创建 `ruoyi-ui/tests/vip-code-plan-select.test.js`：

```js
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/library/vip/code/index.vue'),
  'utf8'
)

assert(page.includes('label="会员套餐"'), '会员码生成弹窗应显示会员套餐字段')
assert(page.includes('<el-select'), '会员码生成弹窗应使用下拉框选择套餐')
assert(
  !page.includes('<el-input-number v-model="form.planId"'),
  '会员码生成弹窗不应继续手工输入套餐编号'
)
assert(page.includes('listVipPlans'), '会员码生成弹窗应复用会员套餐列表接口')
assert(
  page.includes("listVipPlans({ status: '0', pageNum: 1, pageSize: 100 })"),
  '会员码生成弹窗应只查询启用套餐'
)
assert(
  page.includes('form: { planId: null, count: 10, expiresTime: null }'),
  '会员码生成弹窗不应默认选中固定套餐编号'
)
assert(
  page.includes("this.$modal.msgError('请选择会员套餐')"),
  '未选择套餐时应显示简体中文提示'
)
assert(page.includes('planOptionLabel(plan)'), '套餐选项应显示完整套餐摘要')
assert(page.includes('generateVipCodes(this.form)'), '生成请求应继续提交原有planId字段')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `会员码生成页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('会员码生成套餐选择契约测试通过')
```

- [x] **Step 2：运行测试并确认旧实现失败**

Run:

```powershell
node ruoyi-ui/tests/vip-code-plan-select.test.js
```

Expected: FAIL，错误信息包含“会员码生成弹窗应显示会员套餐字段”。

- [x] **Step 3：实现启用套餐下拉选择**

在 `ruoyi-ui/src/views/library/vip/code/index.vue` 中将套餐表单项改为：

```vue
<el-form-item label="会员套餐" required>
  <el-select
    v-model="form.planId"
    :loading="planLoading"
    placeholder="请选择会员套餐"
    class="plan-select"
  >
    <el-option
      v-for="plan in plans"
      :key="plan.id"
      :label="planOptionLabel(plan)"
      :value="plan.id"
    />
  </el-select>
</el-form-item>
```

扩展套餐接口导入：

```js
import { listVipCodes, generateVipCodes, listVipPlans } from '@/api/library/vip'
```

将组件状态改为默认不选择套餐，并增加套餐列表和加载状态：

```js
planLoading: false,
plans: [],
form: { planId: null, count: 10, expiresTime: null },
```

将 `openGenerate` 改为每次打开都清空选择并重新加载启用套餐：

```js
openGenerate() {
  this.codes = []
  this.plans = []
  this.form = { planId: null, count: 10, expiresTime: null }
  this.visible = true
  this.planLoading = true
  listVipPlans({ status: '0', pageNum: 1, pageSize: 100 }).then(response => {
    this.plans = response.rows || []
  }).finally(() => {
    this.planLoading = false
  })
},
```

增加套餐价格和选项摘要格式化方法：

```js
formatPlanPrice(value) {
  return (Number(value || 0) / 100).toFixed(2)
},
planOptionLabel(plan) {
  return `${plan.planName}（${plan.validDays}天，¥${this.formatPlanPrice(plan.priceCent)}，赠送${plan.giftPoints || 0}积分）`
},
```

在调用生成接口前校验套餐：

```js
generate() {
  if (!this.form.planId) return this.$modal.msgError('请选择会员套餐')
  generateVipCodes(this.form).then(response => {
    this.codes = (response.data && response.data.plaintextCodes) || []
  })
}
```

将 scoped 样式整理为：

```css
.mt16 {
  margin-top: 16px;
}

.plan-select {
  width: 100%;
}
```

- [x] **Step 4：复跑页面契约测试**

Run:

```powershell
node ruoyi-ui/tests/vip-code-plan-select.test.js
```

Expected: PASS，输出“会员码生成套餐选择契约测试通过”。

- [x] **Step 5：运行相关既有前端契约测试**

Run:

```powershell
node ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
node ruoyi-ui/tests/vip-plan-valid-days.test.js
```

Expected: 两个测试均 PASS，分别输出对应的“契约测试通过”提示。

- [x] **Step 6：检查变更范围和格式**

Run:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` 无错误；本任务只新增测试并修改会员码页面，其他已有工作区改动保持不变。

- [ ] **Step 7：提交本次功能**

```powershell
git add -- ruoyi-ui/tests/vip-code-plan-select.test.js ruoyi-ui/src/views/library/vip/code/index.vue docs/superpowers/plans/2026-07-29-vip-code-plan-select.md
git commit -m "feat: select plan when generating vip codes"
```
