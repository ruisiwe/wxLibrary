# 日期展示格式统一 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将小程序和后台 `library` 业务页面的日期时间文本统一显示为 `yyyy-MM-dd`，同时保留后端、表单和业务判断使用的完整时间值。

**Architecture:** 小程序新增无副作用的公共 `formatDate` 工具，并在会员资料、昵称更新、兑换结果和积分流水展示边界调用。后台管理端复用若依已全局注入的 `parseTime`，在各日期表格列和详情文本中显式传入 `{y}-{m}-{d}`；日期时间选择器及原始数据不截断。

**Tech Stack:** 微信小程序 CommonJS、Node.js `node:test`、Vue 2、Element UI、若依 `parseTime`

---

## 文件结构

- Create: `miniprogram/utils/date.js`
  - 只负责把可识别日期值转换成 `yyyy-MM-dd` 展示文本。
- Create: `miniprogram/tests/date-display-format.test.js`
  - 验证格式化边界值和小程序各展示入口的接入契约。
- Modify: `miniprogram/services/vip.js`
  - 统一格式化会员资料接口返回的 `vipExpireTime`。
- Modify: `miniprogram/services/auth.js`
  - 统一格式化修改昵称接口返回的 `vipExpireTime`，防止保存昵称后页面重新出现 ISO 时间。
- Modify: `miniprogram/services/point.js`
  - 为积分记录增加只读的 `createDate`，保留 `createTime` 供任务统计。
- Modify: `miniprogram/pages/points/points.wxml`
  - 展示 `createDate`。
- Modify: `miniprogram/pages/redeem-course/redeem-course.js`
  - 格式化兑换成功弹窗中的会员到期日期。
- Create: `ruoyi-ui/tests/library-date-display-format.test.js`
  - 检查后台业务页面均显式使用年月日格式，且 Vue 模板可编译。
- Modify: `ruoyi-ui/tests/wx-user-detail-vip-operation.test.js`
  - 将最后登录时间的旧“精确到秒”契约更新为只显示年月日。
- Modify: `ruoyi-ui/src/views/library/user/index.vue`
  - 格式化用户列表、详情中的会员到期和最后登录日期。
- Modify: `ruoyi-ui/src/views/library/user/detail.vue`
  - 格式化独立用户详情页日期。
- Modify: `ruoyi-ui/src/views/library/vip/entitlement/index.vue`
  - 格式化权益台账的原、新到期日期，并复用统一年月日格式生成用户选项文案。
- Modify: `ruoyi-ui/src/views/library/agreement/index.vue`
  - 格式化协议列表生效日期。
- Modify: `ruoyi-ui/src/views/library/content/banner/index.vue`
  - 格式化宣传图片列表的开始、结束日期。

### Task 1: 小程序公共日期工具

**Files:**
- Create: `miniprogram/utils/date.js`
- Create: `miniprogram/tests/date-display-format.test.js`

- [ ] **Step 1: 编写公共方法的失败测试**

创建 `miniprogram/tests/date-display-format.test.js`：

```js
const test = require('node:test')
const assert = require('node:assert/strict')

const { formatDate } = require('../utils/date')

test('日期展示只保留年月日且不发生时区偏移', () => {
  assert.equal(formatDate('2026-08-28T14:08:47.662+08:00'), '2026-08-28')
  assert.equal(formatDate('2026-08-28 14:08:47'), '2026-08-28')
  assert.equal(formatDate('2026/8/2 14:08:47'), '2026-08-02')
  assert.equal(formatDate('2026-08-28'), '2026-08-28')
})

test('日期展示安全处理空值和非法值', () => {
  assert.equal(formatDate(null), '')
  assert.equal(formatDate(undefined), '')
  assert.equal(formatDate(''), '')
  assert.equal(formatDate('not-a-date'), 'not-a-date')
})
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```powershell
node --test miniprogram/tests/date-display-format.test.js
```

Expected: FAIL，错误为 `Cannot find module '../utils/date'`。

- [ ] **Step 3: 实现最小公共日期方法**

创建 `miniprogram/utils/date.js`：

```js
function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDate(value) {
  if (value === null || value === undefined || value === '') return ''

  if (typeof value === 'string') {
    const source = value.trim()
    const match = source.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
    if (match) return `${match[1]}-${pad(match[2])}-${pad(match[3])}`
  }

  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

module.exports = { formatDate }
```

识别到字符串年月日时直接使用原始日期部分，避免 `+08:00` 等时区值被运行环境转换到前一天或后一天。

- [ ] **Step 4: 运行测试并确认通过**

Run:

```powershell
node --test miniprogram/tests/date-display-format.test.js
```

Expected: 2 tests PASS。

- [ ] **Step 5: 检查并提交公共方法**

Run:

```powershell
git diff --check -- miniprogram/utils/date.js miniprogram/tests/date-display-format.test.js
git add -- miniprogram/utils/date.js miniprogram/tests/date-display-format.test.js
git commit -m "feat: 增加小程序日期展示格式化"
```

仅暂存以上两个文件，不处理工作区现有的其他改动。

### Task 2: 接入小程序所有日期展示入口

**Files:**
- Modify: `miniprogram/tests/date-display-format.test.js`
- Modify: `miniprogram/services/vip.js`
- Modify: `miniprogram/services/auth.js`
- Modify: `miniprogram/services/point.js`
- Modify: `miniprogram/pages/points/points.wxml`
- Modify: `miniprogram/pages/redeem-course/redeem-course.js`

- [ ] **Step 1: 增加展示入口的失败契约测试**

在 `miniprogram/tests/date-display-format.test.js` 增加：

```js
const fs = require('node:fs')
const path = require('node:path')

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8')
}

test('会员资料和修改昵称结果统一格式化会员到期日期', () => {
  const vipService = read('services/vip.js')
  const authService = read('services/auth.js')

  assert.match(vipService, /vipExpireTime:\s*formatDate\(data\.vipExpireTime\)/)
  assert.match(authService, /vipExpireTime:\s*formatDate\(profile\.vipExpireTime\)/)
})

test('兑换结果和积分流水使用年月日展示字段', () => {
  const redeemPage = read('pages/redeem-course/redeem-course.js')
  const pointService = read('services/point.js')
  const pointMarkup = read('pages/points/points.wxml')

  assert.match(redeemPage, /formatDate\(result\.newExpireTime \|\| result\.endTime\)/)
  assert.match(pointService, /createDate:\s*formatDate\(item\.createTime\)/)
  assert.match(pointMarkup, /\{\{item\.createDate\}\}/)
  assert.doesNotMatch(pointMarkup, /\{\{item\.createTime\}\}/)
})
```

- [ ] **Step 2: 运行测试并确认展示契约失败**

Run:

```powershell
node --test miniprogram/tests/date-display-format.test.js
```

Expected: 公共方法测试 PASS，新增的两个展示入口测试 FAIL。

- [ ] **Step 3: 格式化会员资料和昵称更新结果**

在 `miniprogram/services/vip.js` 顶部引入：

```js
const { formatDate } = require('../utils/date')
```

将 `profile` 映射补充为：

```js
const profile = () => request({ url: '/wx/profile' }).then(data => ({
  ...data,
  vipExpireTime: formatDate(data.vipExpireTime),
  avatarUrl: data.avatarPath ? `${apiBaseUrl()}/wx/public/avatar/${data.avatarPath}` : ''
}))
```

在 `miniprogram/services/auth.js` 顶部引入同一工具，并将昵称更新改为：

```js
function updateNickname(nickname) {
  return request({ url: '/wx/profile', method: 'PUT', data: { nickname } })
    .then(profile => ({
      ...profile,
      vipExpireTime: formatDate(profile.vipExpireTime)
    }))
}
```

- [ ] **Step 4: 为积分记录增加日期展示字段**

在 `miniprogram/services/point.js` 引入公共方法，并将 `records` 改为：

```js
const { formatDate } = require('../utils/date')

const records = params => request({ url: '/wx/points/records', data: params })
  .then(result => ({
    ...result,
    items: (result.items || []).map(item => ({
      ...item,
      createDate: formatDate(item.createTime)
    }))
  }))
```

在 `miniprogram/pages/points/points.wxml` 将：

```xml
<text class="time">{{item.createTime}}</text>
```

替换为：

```xml
<text class="time">{{item.createDate}}</text>
```

原始 `createTime` 保留在记录对象中，`miniprogram/pages/vip/vip.js` 继续使用它判断当天积分任务。

- [ ] **Step 5: 格式化兑换成功弹窗**

在 `miniprogram/pages/redeem-course/redeem-course.js` 顶部引入：

```js
const { formatDate } = require('../../utils/date')
```

将到期值改为：

```js
const expire = formatDate(result.newExpireTime || result.endTime)
```

- [ ] **Step 6: 运行小程序相关测试**

Run:

```powershell
node --test miniprogram/tests/date-display-format.test.js miniprogram/tests/profile-nickname-edit.test.js miniprogram/tests/vip-center.test.js miniprogram/tests/vip-payment.test.js
```

Expected: 日期格式、昵称修改、VIP 中心和兑换支付相关测试全部 PASS。若 `vip-center.test.js` 的已知旧页面契约仍失败，确认失败内容与日期格式无关并记录，不在本任务扩展修复。

- [ ] **Step 7: 检查并提交小程序接入**

Run:

```powershell
git diff --check -- miniprogram/tests/date-display-format.test.js miniprogram/services/vip.js miniprogram/services/auth.js miniprogram/services/point.js miniprogram/pages/points/points.wxml miniprogram/pages/redeem-course/redeem-course.js
git add -- miniprogram/tests/date-display-format.test.js miniprogram/services/vip.js miniprogram/services/auth.js miniprogram/services/point.js miniprogram/pages/points/points.wxml miniprogram/pages/redeem-course/redeem-course.js
git commit -m "fix: 统一小程序日期展示格式"
```

不得暂存 `miniprogram/pages/profile/profile.wxml` 等现有用户改动。

### Task 3: 建立后台业务日期展示契约

**Files:**
- Create: `ruoyi-ui/tests/library-date-display-format.test.js`
- Modify: `ruoyi-ui/tests/wx-user-detail-vip-operation.test.js`

- [ ] **Step 1: 编写后台日期展示失败测试**

创建 `ruoyi-ui/tests/library-date-display-format.test.js`：

```js
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function compile(file, source) {
  const component = compiler.parseComponent(source)
  const result = compiler.compile(component.template.content)
  assert.deepEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

test('微信用户与权益台账日期只显示年月日', () => {
  const user = read('src/views/library/user/index.vue')
  const detail = read('src/views/library/user/detail.vue')
  const entitlement = read('src/views/library/vip/entitlement/index.vue')

  assert.match(user, /parseTime\(scope\.row\.vipExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(user, /parseTime\(value,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(detail, /parseTime\(user\.vipExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(detail, /parseTime\(user\.lastLoginTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(entitlement, /parseTime\(scope\.row\.oldExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(entitlement, /parseTime\(scope\.row\.newExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)

  compile('src/views/library/user/index.vue', user)
  compile('src/views/library/user/detail.vue', detail)
  compile('src/views/library/vip/entitlement/index.vue', entitlement)
})

test('协议和宣传图片列表日期只显示年月日', () => {
  const agreement = read('src/views/library/agreement/index.vue')
  const banner = read('src/views/library/content/banner/index.vue')

  assert.match(agreement, /parseTime\(s\.row\.effectiveTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(banner, /parseTime\(scope\.row\.startTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(banner, /parseTime\(scope\.row\.endTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(agreement, /type="datetime"/)
  assert.match(banner, /type="datetime"/)

  compile('src/views/library/agreement/index.vue', agreement)
  compile('src/views/library/content/banner/index.vue', banner)
})
```

在 `ruoyi-ui/tests/wx-user-detail-vip-operation.test.js` 将旧断言：

```js
assert(page.includes("parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}')"),
  '最后登录时间应格式化到秒')
```

替换为：

```js
assert(page.includes("parseTime(value, '{y}-{m}-{d}')"),
  '会员到期和最后登录时间应只显示年月日')
```

- [ ] **Step 2: 运行后台契约测试并确认失败**

Run:

```powershell
node --test ruoyi-ui/tests/library-date-display-format.test.js
node ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
```

Expected: 新日期展示测试 FAIL，微信用户详情旧实现断言 FAIL。

### Task 4: 格式化后台业务页面

**Files:**
- Modify: `ruoyi-ui/src/views/library/user/index.vue`
- Modify: `ruoyi-ui/src/views/library/user/detail.vue`
- Modify: `ruoyi-ui/src/views/library/vip/entitlement/index.vue`
- Modify: `ruoyi-ui/src/views/library/agreement/index.vue`
- Modify: `ruoyi-ui/src/views/library/content/banner/index.vue`
- Test: `ruoyi-ui/tests/library-date-display-format.test.js`
- Test: `ruoyi-ui/tests/wx-user-detail-vip-operation.test.js`

- [ ] **Step 1: 格式化微信用户列表和弹窗详情**

在 `ruoyi-ui/src/views/library/user/index.vue` 将会员到期表格列改为：

```vue
<el-table-column label="会员到期时间" min-width="180">
  <template slot-scope="scope">
    {{ scope.row.vipExpireTime ? parseTime(scope.row.vipExpireTime, '{y}-{m}-{d}') : '未开通' }}
  </template>
</el-table-column>
```

将现有方法改为：

```js
formatDateTime(value) {
  return value ? this.parseTime(value, '{y}-{m}-{d}') : '-'
},
formatVipExpire(value) {
  return value ? this.formatDateTime(value) : '未开通'
}
```

`detailVipState` 继续使用未格式化的 `detail.vipExpireTime` 和 `Date.now()` 判断会员状态。

- [ ] **Step 2: 格式化独立微信用户详情页**

在 `ruoyi-ui/src/views/library/user/detail.vue` 将两个日期描述项改为：

```vue
<el-descriptions-item label="会员到期时间">
  {{ user.vipExpireTime ? parseTime(user.vipExpireTime, '{y}-{m}-{d}') : '未开通' }}
</el-descriptions-item>
<el-descriptions-item label="最后登录时间">
  {{ user.lastLoginTime ? parseTime(user.lastLoginTime, '{y}-{m}-{d}') : '-' }}
</el-descriptions-item>
```

- [ ] **Step 3: 格式化权益台账日期**

在 `ruoyi-ui/src/views/library/vip/entitlement/index.vue` 将原、新到期时间列改为：

```vue
<el-table-column label="原到期时间">
  <template slot-scope="scope">
    {{ scope.row.oldExpireTime ? parseTime(scope.row.oldExpireTime, '{y}-{m}-{d}') : '-' }}
  </template>
</el-table-column>
<el-table-column label="新到期时间">
  <template slot-scope="scope">
    {{ scope.row.newExpireTime ? parseTime(scope.row.newExpireTime, '{y}-{m}-{d}') : '-' }}
  </template>
</el-table-column>
```

将 `expireLabel` 中的手工 `Date` 拼接替换为：

```js
expireLabel(user) {
  if (!user.vipExpireTime) return '当前非VIP'
  const expireDate = this.parseTime(user.vipExpireTime, '{y}-{m}-{d}')
  return expireDate ? `VIP至 ${expireDate}` : 'VIP到期时间未知'
}
```

- [ ] **Step 4: 格式化协议和宣传图片列表**

在 `ruoyi-ui/src/views/library/agreement/index.vue` 将生效时间列改为：

```vue
<el-table-column label="生效时间" min-width="170">
  <template slot-scope="s">
    {{ s.row.effectiveTime ? parseTime(s.row.effectiveTime, '{y}-{m}-{d}') : '-' }}
  </template>
</el-table-column>
```

在 `ruoyi-ui/src/views/library/content/banner/index.vue` 将开始、结束时间列改为：

```vue
<el-table-column label="开始时间" width="165">
  <template slot-scope="scope">
    {{ scope.row.startTime ? parseTime(scope.row.startTime, '{y}-{m}-{d}') : '-' }}
  </template>
</el-table-column>
<el-table-column label="结束时间" width="165">
  <template slot-scope="scope">
    {{ scope.row.endTime ? parseTime(scope.row.endTime, '{y}-{m}-{d}') : '-' }}
  </template>
</el-table-column>
```

保留协议、宣传图片和兑换码页面的 `type="datetime"`、`value-format` 及表单原始值。

- [ ] **Step 5: 运行后台日期和关联页面测试**

Run:

```powershell
node --test ruoyi-ui/tests/library-date-display-format.test.js
node ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
node --test ruoyi-ui/tests/library-page-table-style.test.js ruoyi-ui/tests/library-simple-list-style.test.js ruoyi-ui/tests/vip-entitlement-batch-operation.test.js ruoyi-ui/tests/agreement-operation-column.test.js
```

Expected: 所有测试 PASS，Vue 模板编译无错误。

- [ ] **Step 6: 检查并提交后台改动**

Run:

```powershell
git diff --check -- ruoyi-ui/tests/library-date-display-format.test.js ruoyi-ui/tests/wx-user-detail-vip-operation.test.js ruoyi-ui/src/views/library/user/index.vue ruoyi-ui/src/views/library/user/detail.vue ruoyi-ui/src/views/library/vip/entitlement/index.vue ruoyi-ui/src/views/library/agreement/index.vue ruoyi-ui/src/views/library/content/banner/index.vue
git add -- ruoyi-ui/tests/library-date-display-format.test.js ruoyi-ui/tests/wx-user-detail-vip-operation.test.js ruoyi-ui/src/views/library/user/index.vue ruoyi-ui/src/views/library/user/detail.vue ruoyi-ui/src/views/library/vip/entitlement/index.vue ruoyi-ui/src/views/library/agreement/index.vue ruoyi-ui/src/views/library/content/banner/index.vue
git commit -m "fix: 统一后台业务日期展示格式"
```

不得暂存当前已修改的 `ruoyi-ui/src/views/library/vip/code/index.vue`、`ruoyi-ui/src/views/library/vip/plan/index.vue` 或其他无关文件。

### Task 5: 全量审计与最终验证

**Files:**
- Modify only if an遗漏 is found in: `miniprogram/**/*.wxml`, `miniprogram/**/*.js`, `ruoyi-ui/src/views/library/**/*.vue`
- Test: `miniprogram/tests/*.test.js`
- Test: `ruoyi-ui/tests/*.test.js`

- [ ] **Step 1: 搜索仍直接展示的日期字段**

Run:

```powershell
rg -n --glob '*.wxml' --glob '*.js' '(createTime|updateTime|expireTime|expiresTime|startTime|endTime|lastLoginTime|paidTime|usedTime|effectiveTime|publishTime|unlockTime|grantedTime|acceptedTime)' miniprogram
rg -n --glob '*.vue' '(prop="[^"]*(Time|Date)"|\{\{[^}]*(Time|Date)[^}]*\}\})' ruoyi-ui/src/views/library
```

Expected:

- 小程序实际日期文本均经过 `formatDate` 或展示字段。
- 后台 `library` 的日期表格列、详情文本均经过 `parseTime(..., '{y}-{m}-{d}')`。
- 日期选择器、日期比较代码和 `vip.js` 当天任务统计可继续出现完整时间字段。

- [ ] **Step 2: 运行小程序全量测试**

Run:

```powershell
Set-Location miniprogram
npm test
```

Expected: 本次新增和相关测试 PASS。项目现有已知的分类图标数量契约、VIP 任务旧文案契约若仍失败，失败内容应与本次日期改动一致地保持基线，不扩展修复。

- [ ] **Step 3: 运行后台业务测试和生产构建**

Run:

```powershell
Set-Location ruoyi-ui
node --test tests/*.test.js
npm run build:prod
```

Expected: 后台测试 PASS，生产构建成功。构建产生的 `ruoyi-ui/dist` 不暂存、不提交。

- [ ] **Step 4: 最终差异检查**

Run:

```powershell
Set-Location ..
git diff --check
git status --short
git diff -- miniprogram ruoyi-ui/src/views/library ruoyi-ui/tests
```

确认：

- 没有修改后端时间序列化、数据库或配置文件。
- 没有覆盖用户现有工作区改动。
- 所有日期文本只显示 `yyyy-MM-dd`。
- 日期时间表单和业务判断仍保留完整精度。
- 构建产物和环境文件未被暂存。

