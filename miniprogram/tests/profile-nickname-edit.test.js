const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const root = path.resolve(__dirname, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

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
      if (modulePath === '../../services/qr') return { list: () => Promise.resolve([]) }
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
    toasts
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
