# 小程序暂时移除会员套餐区域 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从小程序 VIP 套餐页暂时移除套餐展示和支付运行逻辑，只保留会员信息、VIP 权益和开通咨询。

**Architecture:** `vip-plans` 页面不再获取套餐或发起支付，只通过 `vip.profile()` 加载会员信息，并继续独立加载 `vip.pageConfig()`。公共 VIP 服务、后台套餐管理和后端套餐支付接口保持不变，便于后续恢复。

**Tech Stack:** 微信小程序原生 WXML/JavaScript/WXSS、Node.js Test Runner

---

### Task 1：更新用户端套餐隐藏契约

**Files:**
- Modify: `miniprogram/tests/vip-center.test.js`
- Modify: `miniprogram/tests/vip-benefit-introduction.test.js`

- [x] **Step 1：将现有测试改为暂不展示套餐的契约**

在 `miniprogram/tests/vip-center.test.js` 中将“VIP 套餐和支付逻辑位于子页面”测试替换为：

```js
test('VIP 子页面暂不展示套餐和支付入口', () => {
  assert.ok(app.pages.includes('pages/vip-plans/vip-plans'));
  const service = read('services/vip.js');
  const logic = read('pages/vip-plans/vip-plans.js');
  const markup = read('pages/vip-plans/vip-plans.wxml');

  assert.match(logic, /vip\.profile/);
  assert.doesNotMatch(logic, /vip\.plans/);
  assert.doesNotMatch(logic, /vip\.createOrder/);
  assert.doesNotMatch(logic, /vip\.queryOrder/);
  assert.doesNotMatch(logic, /wx\.requestPayment/);
  assert.doesNotMatch(markup, /wx:for="\{\{plans\}\}"/);
  assert.doesNotMatch(markup, /购买\/续费/);
  assert.doesNotMatch(markup, /paymentState/);
  assert.match(service, /const plans =/);
  assert.match(service, /const createOrder =/);
});
```

在 `miniprogram/tests/vip-benefit-introduction.test.js` 中将第一个测试标题改为：

```js
test('VIP 权益介绍在移除套餐后继续独立展示', () => {
```

并在该测试中增加：

```js
assert.doesNotMatch(markup, /wx:for="\{\{plans\}\}"/)
```

将第三个测试标题改为：

```js
test('页面配置失败可独立重试且不清空会员信息', () => {
```

将 `loadPageConfig` 测试片段改为从该方法截取到文件末尾：

```js
const loader = logic.slice(start)
assert.ok(start >= 0)
assert.doesNotMatch(logic, /plans\s*:/)
```

- [x] **Step 2：运行测试并确认旧页面失败**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/vip-center.test.js miniprogram/tests/vip-benefit-introduction.test.js
```

Expected: FAIL，失败信息表明 `vip-plans` 页面仍包含 `vip.plans` 或套餐循环。

### Task 2：从 VIP 页面移除套餐和支付流程

**Files:**
- Modify: `miniprogram/pages/vip-plans/vip-plans.wxml`
- Modify: `miniprogram/pages/vip-plans/vip-plans.js`
- Modify: `miniprogram/pages/vip-plans/vip-plans.wxss`

- [x] **Step 1：移除套餐和支付 WXML**

从 `miniprogram/pages/vip-plans/vip-plans.wxml` 删除以下两块内容：

```xml
<view wx:if="{{paymentState}}" class="payment">{{paymentState}}</view>
<view class="plans">
  <view wx:for="{{plans}}" wx:key="id" class="plan">
    <view>
      <view class="plan__name">{{item.planName}}</view>
      <view class="plan__meta">{{item.validDays}} 天 · 赠送 {{item.giftPoints || 0}} 积分</view>
    </view>
    <view class="plan__right">
      <view>¥{{item.priceCent / 100}}</view>
      <button size="mini" data-id="{{item.id}}" bindtap="buy">购买/续费</button>
    </view>
  </view>
</view>
```

- [x] **Step 2：页面只加载会员信息**

在 `miniprogram/pages/vip-plans/vip-plans.js` 的 `data` 中删除：

```js
plans: [],
paymentState: '',
```

将 `load()` 改为：

```js
load() {
  this.setData({ loading: true, error: '' });
  vip.profile()
    .then(profile => this.setData({ profile: profile || {}, loading: false }))
    .catch(error => this.setData({
      loading: false,
      error: error.message || '会员信息加载失败，请重试'
    }));
},
```

删除 `buy(event)` 和 `pollOrder(attempt)` 方法及其全部支付逻辑。

- [x] **Step 3：清理套餐专用样式**

从 `miniprogram/pages/vip-plans/vip-plans.wxss` 删除：

```css
.payment{margin-top:20rpx;padding:20rpx;text-align:center;background:#eff6ff;color:#1d4ed8;border-radius:16rpx}
.plan{display:flex;justify-content:space-between;margin-top:24rpx;padding:30rpx;background:#fff;border-radius:22rpx}
.plan__name{font-size:32rpx;font-weight:600}
.plan__meta{margin-top:14rpx;color:#64748b;font-size:24rpx}
.plan__right{text-align:right;color:#f59e0b;font-weight:600}
.plan__right button{margin-top:14rpx;background:#2457d6;color:#fff}
```

- [x] **Step 4：复跑两个聚焦测试**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/vip-center.test.js miniprogram/tests/vip-benefit-introduction.test.js
```

Expected: 本任务相关 7 项测试通过；既有“积分任务进度合并到任务列表并提示每日上限”文案断言仍失败。

- [x] **Step 5：运行小程序完整测试**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/*.test.js
```

Expected: 本次运行共 45 项，43 项通过；已知基线失败为分类精选图标数量断言和 VIP 任务进度旧文案断言，本任务不修改这两处业务。

- [x] **Step 6：检查变更范围和格式**

Run:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` 无错误；本任务只修改 `vip-plans` 页面、两个对应测试和本实施计划，其他已有工作区改动保持不变。

- [ ] **Step 7：提交本次修改**

```powershell
git add -- miniprogram/pages/vip-plans/vip-plans.wxml miniprogram/pages/vip-plans/vip-plans.js miniprogram/pages/vip-plans/vip-plans.wxss miniprogram/tests/vip-center.test.js miniprogram/tests/vip-benefit-introduction.test.js docs/superpowers/plans/2026-07-29-hide-vip-plans.md
git commit -m "feat: hide vip plans from mini program"
```
