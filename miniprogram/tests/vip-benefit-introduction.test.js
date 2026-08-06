const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8')
}

test('VIP 权益介绍在移除套餐后继续独立展示', () => {
  const service = read('services/vip.js')
  const logic = read('pages/vip-plans/vip-plans.js')
  const markup = read('pages/vip-plans/vip-plans.wxml')

  assert.match(service, /url:\s*['"]\/wx\/vip\/page-config['"]/)
  assert.match(service, /module\.exports\s*=\s*\{[^}]*pageConfig/)
  assert.match(markup, /VIP 权益/)
  assert.match(markup, /wx:for="\{\{pageConfig\.benefits\}\}"/)
  assert.match(markup, /wx:key="\*this"/)
  assert.doesNotMatch(markup, /wx:for="\{\{plans\}\}"/)
  assert.match(logic, /loadPageConfig/)
  assert.match(logic, /vip\.pageConfig\(\)/)
})

test('客服微信图片未配置时隐藏客服区域', () => {
  const markup = read('pages/vip-plans/vip-plans.wxml')

  assert.match(markup, /wx:if="\{\{pageConfig\.customerServiceImageUrl\}\}"/)
  assert.match(markup, /mode="widthFix"/)
  assert.match(markup, /pageConfig\.customerServiceTip/)
  assert.match(markup, /开通 VIP 请添加客服微信/)
})

test('页面配置失败可独立重试且不清空会员信息', () => {
  const logic = read('pages/vip-plans/vip-plans.js')
  const markup = read('pages/vip-plans/vip-plans.wxml')
  const start = logic.indexOf('  loadPageConfig() {')
  const loader = logic.slice(start)

  assert.ok(start >= 0)
  assert.match(loader, /configError/)
  assert.match(loader, /VIP 权益介绍加载失败，请重试/)
  assert.doesNotMatch(logic, /plans\s*:/)
  assert.doesNotMatch(loader, /profile\s*:/)
  assert.match(markup, /configError/)
  assert.match(markup, /bindtap="loadPageConfig"/)
})

test('客服微信本地受控图片地址会拼接服务端根地址', () => {
  const service = read('services/vip.js')

  assert.match(service, /customerServiceImageUrl/)
  assert.match(service, /apiBaseUrl\(\)/)
  assert.match(service, /startsWith\(['"]\/['"]\)/)
})
