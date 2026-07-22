# Mini Program VIP Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bottom video-course tab with a VIP center that combines membership status, current points and point tasks while moving plan purchase into a child page.

**Architecture:** Keep `pages/vip/vip` as the tab page, move its existing plan/payment behavior into `pages/vip-plans/vip-plans`, and turn `pages/points/points` into the ledger child page. Reuse the existing VIP and point services; do not change backend APIs or database structures.

**Tech Stack:** WeChat native mini program, CommonJS services, TDesign custom tab bar, Node.js `node:test` source-contract tests.

---

### Task 1: Lock the navigation and page split with failing tests

**Files:**
- Modify: `miniprogram/tests/navigation.test.js`
- Create: `miniprogram/tests/vip-center.test.js`

- [ ] **Step 1: Change navigation expectations**

Expect the first three pages and tabs to be:

```javascript
const expectedPages = ['pages/index/index', 'pages/vip/vip', 'pages/profile/profile']
const expectedTabs = [
  { pagePath: 'pages/index/index', text: '首页' },
  { pagePath: 'pages/vip/vip', text: 'VIP' },
  { pagePath: 'pages/profile/profile', text: '我的' }
]
```

- [ ] **Step 2: Add VIP page-boundary tests**

Assert that the VIP tab markup contains current points and all three tasks, does not contain a plan loop, and navigates to `/pages/vip-plans/vip-plans`; assert the plan child page owns `vip.plans`, order creation and payment; assert profile markup no longer links to VIP or point tasks; assert the points page contains ledger records but no task buttons.

- [ ] **Step 3: Run tests and confirm RED**

Run `npm test` from `miniprogram`. Expected: navigation and VIP-center tests fail because the current tab is still video courses and the page split does not exist.

### Task 2: Create the VIP plan child page and update navigation

**Files:**
- Modify: `miniprogram/app.json`
- Modify: `miniprogram/custom-tab-bar/index.js`
- Create: `miniprogram/pages/vip-plans/vip-plans.js`
- Create: `miniprogram/pages/vip-plans/vip-plans.wxml`
- Create: `miniprogram/pages/vip-plans/vip-plans.wxss`
- Create: `miniprogram/pages/vip-plans/vip-plans.json`

- [ ] **Step 1: Replace the second tab**

Set the second tab path to `pages/vip/vip`, text to `VIP`, and custom-tab icon to `diamond`. Keep page and custom-tab configuration identical.

- [ ] **Step 2: Move the existing plan/payment behavior**

Copy the current VIP plan list, order creation, `wx.requestPayment`, order polling and safe Chinese payment states into `vip-plans`. On `onShow`, reload plans/profile so a completed payment refreshes membership state.

- [ ] **Step 3: Register the child page**

Add `pages/vip-plans/vip-plans` after the VIP tab page in `app.json`; keep course pages registered as non-tab pages so existing course links are not broken.

### Task 3: Build the VIP center and ledger child page

**Files:**
- Modify: `miniprogram/pages/vip/vip.js`
- Modify: `miniprogram/pages/vip/vip.wxml`
- Modify: `miniprogram/pages/vip/vip.wxss`
- Modify: `miniprogram/pages/vip/vip.json`
- Modify: `miniprogram/pages/points/points.js`
- Modify: `miniprogram/pages/points/points.wxml`
- Modify: `miniprogram/pages/points/points.wxss`
- Modify: `miniprogram/pages/points/points.json`
- Modify: `miniprogram/pages/profile/profile.wxml`

- [ ] **Step 1: Load VIP center data**

On `onShow`, update the tab highlight. If no token exists, show a login guide that switches to the profile tab. Otherwise load `vip.profile()`, `points.balance()` and `points.rules()` together; derive the advertised video daily limit from the `AD_REWARD` rule and cap it at 5.

- [ ] **Step 2: Move point tasks into VIP**

Move daily sign-in, rewarded-video progress/cache and share invitation behavior from the points page to VIP. Successful tasks refresh membership/point data; task failures show safe Chinese toast text without clearing already loaded data.

- [ ] **Step 3: Add child-page navigation**

The membership card button opens `/pages/vip-plans/vip-plans`; the points card opens `/pages/points/points`. The VIP tab itself must not call `vip.plans()` and must not show package prices.

- [ ] **Step 4: Reduce the points page to a ledger**

Load balance and paged point records only. Show current balance plus the ledger, with no sign-in, ad or share buttons.

- [ ] **Step 5: Remove duplicated profile entries**

Remove “VIP 会员” and “积分任务与流水” menu rows from profile while preserving document, favorite, course, agreement and feedback entries.

### Task 4: Verify and commit

**Files:** All files above plus this plan.

- [ ] **Step 1: Run the full mini-program test suite**

Run `npm test` from `miniprogram`. Expected: all tests pass.

- [ ] **Step 2: Check configuration and source diff**

Run scoped `git diff --check`, ensure all Chinese text is readable, and confirm no generated `miniprogram_npm`, environment file or unrelated working-tree change is included.

- [ ] **Step 3: Commit only the VIP navigation feature**

Commit only the plan, mini-program app/tab configuration, VIP/plan/points/profile pages and focused tests with:

```text
feat: make VIP the second mini program tab
```

Do not push.
