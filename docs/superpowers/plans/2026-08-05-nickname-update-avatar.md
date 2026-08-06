# 修改昵称后头像保持有效 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复小程序用户修改昵称后个人资料头像地址丢失的问题。

**Architecture:** 保持后端响应和页面更新流程不变，只在 `auth.updateNickname()` 的资料响应转换边界补充 `avatarPath` 到 `avatarUrl` 的映射，使其与 `vip.profile()` 的现有格式一致。使用真实加载 `auth.js` 的 Node 测试锁定该契约。

**Tech Stack:** 微信小程序 CommonJS、Node.js 内置 `node:test`、`vm`

---

### Task 1：用服务层测试复现头像地址丢失

**Files:**
- Modify: `miniprogram/tests/profile-nickname-edit.test.js`

- [ ] **Step 1：增加真实加载 auth.js 的测试工具**

在现有测试文件中增加 `loadAuthService`，通过 `vm.runInNewContext` 加载真实 `services/auth.js`，只替换请求、会话和日期依赖：

```javascript
function loadAuthService(profileResponse) {
  const module = { exports: {} }
  const calls = []
  vm.runInNewContext(read('services/auth.js'), {
    module,
    exports: module.exports,
    require(modulePath) {
      if (modulePath === './request') return {
        request: options => {
          calls.push(options)
          return Promise.resolve(profileResponse)
        },
        apiBaseUrl: () => 'https://api.example.com',
        unwrapResponse: value => value
      }
      if (modulePath === '../store/session') return { save: () => {} }
      if (modulePath === '../utils/date') return { formatDate: value => value }
      throw new Error(`未处理模块：${modulePath}`)
    },
    Promise,
    wx: {}
  })
  return { auth: module.exports, calls }
}
```

- [ ] **Step 2：增加修改昵称后生成头像展示地址的失败测试**

```javascript
test('修改昵称响应将头像路径转换为页面头像地址', async () => {
  const harness = loadAuthService({
    nickname: '新昵称',
    avatarPath: '202607/a.jpg',
    vipExpireTime: null
  })

  const profile = await harness.auth.updateNickname('新昵称')

  assert.equal(harness.calls[0].url, '/wx/profile')
  assert.equal(profile.avatarPath, '202607/a.jpg')
  assert.equal(profile.avatarUrl,
    'https://api.example.com/wx/public/avatar/202607/a.jpg')
})
```

- [ ] **Step 3：运行测试确认 RED**

Run：

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/profile-nickname-edit.test.js
```

Expected：新测试失败，`profile.avatarUrl` 实际值为 `undefined`。

### Task 2：在昵称更新响应边界补全头像 URL

**Files:**
- Modify: `miniprogram/services/auth.js`

- [ ] **Step 1：写入最小实现**

将 `updateNickname` 的响应映射改为：

```javascript
function updateNickname(nickname) {
  return request({ url: '/wx/profile', method: 'PUT', data: { nickname } })
    .then(profile => ({
      ...profile,
      vipExpireTime: formatDate(profile.vipExpireTime),
      avatarUrl: profile.avatarPath
        ? `${apiBaseUrl()}/wx/public/avatar/${profile.avatarPath}`
        : ''
    }))
}
```

- [ ] **Step 2：运行聚焦测试确认 GREEN**

重复 Task 1 的测试命令。

Expected：`profile-nickname-edit.test.js` 全部 PASS。

### Task 3：相关回归与差异复核

**Files:**
- Verify: `miniprogram/services/auth.js`
- Verify: `miniprogram/tests/profile-nickname-edit.test.js`

- [ ] **Step 1：运行昵称和日期相关回归测试**

Run：

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/profile-nickname-edit.test.js miniprogram/tests/date-display-format.test.js miniprogram/tests/silent-login.test.js
```

Expected：全部 PASS。

- [ ] **Step 2：检查差异**

Run：

```powershell
git diff --check -- miniprogram/services/auth.js miniprogram/tests/profile-nickname-edit.test.js
git status --short -- miniprogram/services/auth.js miniprogram/tests/profile-nickname-edit.test.js
```

Expected：无空白错误；仅 `auth.js` 新增头像映射，测试文件保留已有二维码测试兼容改动并新增头像回归测试。

- [ ] **Step 3：交付**

不创建分支，不暂存或提交，不修改后端，不执行发布或部署。
