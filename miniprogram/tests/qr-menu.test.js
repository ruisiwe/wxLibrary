const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8')
}

test('我的页面在登录内容中遍历有效二维码并保持指定位置', () => {
  const logic = read('pages/profile/profile.js')
  const markup = read('pages/profile/profile.wxml')
  const profileBranch = markup.indexOf('<view wx:else>')
  const redeem = markup.indexOf('/pages/redeem-course/redeem-course')
  const qrLoop = markup.indexOf('wx:for="{{qrMenus}}"')
  const privacy = markup.indexOf('/pages/agreement/agreement?type=PRIVACY')

  assert.ok(profileBranch >= 0)
  assert.ok(redeem < qrLoop && qrLoop < privacy)
  assert.match(markup, /wx:key="id"/)
  assert.match(markup, /data-id="\{\{item\.id\}\}"/)
  assert.match(markup, /\{\{item\.menuName\}\}/)
  assert.match(logic, /qrMenus:\s*\[\]/)
  assert.match(logic, /qr\.list\(\)/)
  assert.match(logic, /openQr/)
  assert.match(logic, /\/pages\/qr-code\/qr-code\?id=/)
})

test('二维码菜单请求失败不会清空用户资料', () => {
  const logic = read('pages/profile/profile.js')
  const start = logic.indexOf('  loadQrMenus()')
  const loader = logic.slice(start, logic.indexOf('  openQr', start))

  assert.ok(start >= 0)
  assert.match(loader, /qrMenus/)
  assert.doesNotMatch(loader, /profile:\s*null/)
})

test('统一二维码页支持受控下载、预览和未配置空态', () => {
  const service = read('services/qr.js')
  const logic = read('pages/qr-code/qr-code.js')
  const markup = read('pages/qr-code/qr-code.wxml')
  const app = read('app.json')

  assert.match(service, /url:\s*['"]\/wx\/qr-configs['"]/)
  assert.match(service, /Wx-Token/)
  assert.match(service, /wx\.downloadFile/)
  assert.match(logic, /wx\.previewImage/)
  assert.match(markup, /show-menu-by-longpress/)
  assert.match(markup, /二维码暂未配置/)
  assert.match(markup, /guideText/)
  assert.match(app, /pages\/qr-code\/qr-code/)
})
