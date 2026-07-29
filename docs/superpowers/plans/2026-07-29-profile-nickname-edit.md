# 小程序“我的”页面修改昵称 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在小程序“我的”页面昵称右侧增加“编辑图标 + 修改昵称”按钮，通过原生输入弹窗修改昵称并同步页面与本地会话。

**Architecture:** 页面复用现有 `auth.updateNickname` 和后端 `/wx/profile` 接口，不新增后端能力。页面负责弹窗输入、空值与重复提交控制；接口成功后以返回资料刷新页面，并只更新本地会话用户的昵称字段。

**Tech Stack:** 微信原生小程序、TDesign MiniProgram、Node.js `node:test`、VM 页面逻辑测试。

---

### Task 1：用测试锁定昵称按钮和编辑流程

**Files:**
- Create: `miniprogram/tests/profile-nickname-edit.test.js`
- Test: `miniprogram/pages/profile/profile.json`
- Test: `miniprogram/pages/profile/profile.wxml`
- Test: `miniprogram/pages/profile/profile.wxss`
- Test: `miniprogram/pages/profile/profile.js`

- [ ] **Step 1：创建页面结构与逻辑测试**

新增 `profile-nickname-edit.test.js`：

```javascript
const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const root = path.resolve(__dirname, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

function loadProfilePage(options = {}) {
  let definition
  let modalOptions
  const toasts = []
  const nicknameCalls = []
  const sessionSaves = []
  const cachedUser = { id: 7, nickname: '旧昵称', avatarPath: '202607/a.jpg' }
  const updatedProfile = {
    id: 7,
    nickname: '新昵称',
    avatarUrl: '/wx/public/avatar/202607/a.jpg',
    pointBalance: 10,
    vipActive: false
  }
  const auth = {
    silentLogin: () => Promise.resolve({ user: {} }),
    firstLogin: () => Promise.resolve({ user: {} }),
    updateNickname: nickname => {
      nicknameCalls.push(nickname)
      return options.updateNickname
        ? options.updateNickname(nickname)
        : Promise.resolve(updatedProfile)
    }
  }
  const session = {
    getToken: () => 'token',
    getUser: () => cachedUser,
    save: (token, user) => sessionSaves.push({ token, user }),
    clear: () => {}
  }
  const wx = {
    showModal: value => {
      modalOptions = value
    },
    showToast: value => {
      toasts.push(value)
    },
    navigateTo: () => {},
    login: () => {}
  }
  const sourcePath = path.join(root, 'pages/profile/profile.js')
  vm.runInNewContext(read('pages/profile/profile.js'), {
    Page(value) {
      definition = value
    },
    require(modulePath) {
      if (modulePath === '../../services/auth') return auth
      if (modulePath === '../../store/session') return session
      if (modulePath === '../../services/request') return { request: () => Promise.resolve([]) }
      if (modulePath === '../../services/vip') return { profile: () => Promise.resolve(updatedProfile) }
      throw new Error(`未处理模块：${modulePath}`)
    },
    wx,
    Promise
  }, { filename: sourcePath })

  return {
    definition,
    getModalOptions: () => modalOptions,
    nicknameCalls,
    sessionSaves,
    toasts,
    updatedProfile
  }
}

function createInstance(definition, nickname = '旧昵称') {
  return {
    data: {
      ...definition.data,
      profile: {
        id: 7,
        nickname,
        avatarUrl: '/wx/public/avatar/202607/a.jpg',
        pointBalance: 10,
        vipActive: false
      }
    },
    setData(nextData) {
      Object.assign(this.data, nextData)
    }
  }
}

test('昵称右侧显示编辑图标和修改昵称小字', () => {
  const config = JSON.parse(read('pages/profile/profile.json'))
  const markup = read('pages/profile/profile.wxml')
  const styles = read('pages/profile/profile.wxss')

  assert.equal(config.usingComponents['t-icon'], 'tdesign-miniprogram/icon/icon')
  assert.match(markup, /class="nickname-row"/)
  assert.match(markup, /name="edit-1"/)
  assert.match(markup, /修改昵称/)
  assert.match(markup, /bindtap="editNickname"/)
  assert.match(styles, /\.nickname-edit/)
})

test('点击修改昵称弹出带当前昵称的可编辑窗口', () => {
  const harness = loadProfilePage()
  const instance = createInstance(harness.definition)

  harness.definition.editNickname.call(instance)

  const modal = harness.getModalOptions()
  assert.equal(modal.title, '修改昵称')
  assert.equal(modal.editable, true)
  assert.equal(modal.content, '旧昵称')
  assert.equal(modal.placeholderText, '请输入昵称')
})

test('取消、空昵称和相同昵称不调用更新接口', async () => {
  const harness = loadProfilePage()
  const instance = createInstance(harness.definition)

  harness.definition.editNickname.call(instance)
  harness.getModalOptions().success({ confirm: false })
  await harness.definition.saveNickname.call(instance, '   ')
  await harness.definition.saveNickname.call(instance, ' 旧昵称 ')

  assert.deepEqual(harness.nicknameCalls, [])
  assert.equal(harness.toasts[0].title, '昵称不能为空')
})

test('保存成功后更新页面昵称和本地会话', async () => {
  const harness = loadProfilePage()
  const instance = createInstance(harness.definition)

  await harness.definition.saveNickname.call(instance, ' 新昵称 ')

  assert.deepEqual(harness.nicknameCalls, ['新昵称'])
  assert.equal(instance.data.profile.nickname, '新昵称')
  assert.equal(instance.data.nicknameSaving, false)
  assert.equal(harness.sessionSaves[0].token, 'token')
  assert.equal(harness.sessionSaves[0].user.nickname, '新昵称')
  assert.equal(harness.toasts[0].title, '昵称修改成功')
})

test('保存期间阻止重复提交并展示接口错误', async () => {
  let rejectUpdate
  const pending = new Promise((resolve, reject) => {
    rejectUpdate = reject
  })
  const harness = loadProfilePage({ updateNickname: () => pending })
  const instance = createInstance(harness.definition)

  const first = harness.definition.saveNickname.call(instance, '新昵称')
  const second = harness.definition.saveNickname.call(instance, '另一个昵称')
  assert.deepEqual(harness.nicknameCalls, ['新昵称'])
  assert.equal(second, undefined)

  rejectUpdate(new Error('昵称不能包含HTML标签或控制字符'))
  await first

  assert.equal(instance.data.nicknameSaving, false)
  assert.equal(harness.toasts[0].title, '昵称不能包含HTML标签或控制字符')
})
```

- [ ] **Step 2：运行新测试并确认旧页面失败**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  --test miniprogram/tests/profile-nickname-edit.test.js
```

预期：页面未注册 `t-icon`，结构及 `editNickname` 方法断言失败。

### Task 2：实现昵称按钮与编辑交互

**Files:**
- Modify: `miniprogram/pages/profile/profile.json`
- Modify: `miniprogram/pages/profile/profile.wxml`
- Modify: `miniprogram/pages/profile/profile.wxss`
- Modify: `miniprogram/pages/profile/profile.js`

- [ ] **Step 1：注册 TDesign 图标**

将 `profile.json` 改为：

```json
{
  "navigationBarTitleText": "我的",
  "usingComponents": {
    "login-sheet": "/components/login-sheet/index",
    "t-icon": "tdesign-miniprogram/icon/icon"
  }
}
```

- [ ] **Step 2：增加昵称行和修改按钮**

将头像后的资料容器和昵称部分改为：

```xml
<view class="user__details">
  <view class="nickname-row">
    <view class="nickname">{{profile.nickname}}</view>
    <view
      class="nickname-edit"
      bindtap="editNickname"
      aria-role="button"
      aria-label="修改昵称"
    >
      <t-icon name="edit-1" size="24rpx" color="rgba(255,255,255,.72)" />
      <text>修改昵称</text>
    </view>
  </view>
  <view>{{profile.pointBalance}} 积分 · {{profile.vipActive ? 'VIP 至 '+profile.vipExpireTime : '普通用户'}}</view>
</view>
```

- [ ] **Step 3：增加页面样式**

保留现有样式并增加：

```css
.user__details{flex:1;min-width:0}
.nickname-row{display:flex;align-items:center;min-width:0;margin-bottom:14rpx}
.nickname{min-width:0;margin-bottom:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.nickname-edit{display:flex;flex-shrink:0;align-items:center;margin-left:16rpx;color:rgba(255,255,255,.72);font-size:22rpx;font-weight:400;line-height:32rpx}
.nickname-edit text{margin-left:6rpx}
```

- [ ] **Step 4：增加保存状态和弹窗方法**

在页面 `data` 中增加：

```javascript
nicknameSaving: false
```

在 `methods` 同级位置增加：

```javascript
editNickname() {
  if (this.data.nicknameSaving || !this.data.profile) return
  wx.showModal({
    title: '修改昵称',
    editable: true,
    content: this.data.profile.nickname || '',
    placeholderText: '请输入昵称',
    success: result => {
      if (result.confirm) this.saveNickname(result.content)
    }
  })
},
saveNickname(value) {
  const nickname = (value || '').trim()
  if (!nickname) {
    wx.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }
  if (nickname === this.data.profile.nickname || this.data.nicknameSaving) return

  this.setData({ nicknameSaving: true })
  return auth.updateNickname(nickname).then(profile => {
    const cachedUser = session.getUser() || {}
    session.save(session.getToken(), { ...cachedUser, nickname: profile.nickname })
    this.setData({ profile, nicknameSaving: false })
    wx.showToast({ title: '昵称修改成功', icon: 'success' })
  }).catch(error => {
    this.setData({ nicknameSaving: false })
    wx.showToast({
      title: error.message || '昵称修改失败，请稍后重试',
      icon: 'none'
    })
  })
},
```

- [ ] **Step 5：运行昵称编辑测试**

重复 Task 1 Step 2 命令，预期 5 个用例全部通过。

- [ ] **Step 6：运行完整小程序测试**

工作目录：`miniprogram`

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test tests/*.test.js
```

预期：现有测试和新测试全部通过。

- [ ] **Step 7：提交小程序改动**

```powershell
git add -- `
  miniprogram/pages/profile/profile.json `
  miniprogram/pages/profile/profile.wxml `
  miniprogram/pages/profile/profile.wxss `
  miniprogram/pages/profile/profile.js `
  miniprogram/tests/profile-nickname-edit.test.js
git commit -m "feat: add profile nickname editing"
```

### Task 3：最终验证与范围检查

**Files:**
- Verify: `miniprogram/pages/profile/profile.json`
- Verify: `miniprogram/pages/profile/profile.wxml`
- Verify: `miniprogram/pages/profile/profile.wxss`
- Verify: `miniprogram/pages/profile/profile.js`
- Verify: `miniprogram/tests/profile-nickname-edit.test.js`

- [ ] **Step 1：重新运行完整小程序测试**

运行 Task 2 Step 6 命令，读取测试总数和失败数。

- [ ] **Step 2：检查提交差异**

```powershell
git diff --check HEAD
git show --stat --oneline HEAD
git status --short
```

确认：

- 按钮位于昵称右侧，使用 `edit-1` 和“修改昵称”。
- 页面只调用现有昵称接口，没有新增后端改动。
- 成功后更新页面和会话，失败恢复保存状态。
- 没有修改敏感配置、数据库或构建产物。
- 既有 `index`、`search`、VIP 套餐价格及 `DocumentService` 未提交改动保持原样。

