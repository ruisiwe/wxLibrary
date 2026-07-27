const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8')
}

test('VIP 权益介绍通过独立接口展示在套餐列表下方', () => {
  const service = read('services/vip.js')
  const logic = read('pages/vip-plans/vip-plans.js')
  const markup = read('pages/vip-plans/vip-plans.wxml')

  assert.match(service, /url:\s*['"]\/wx\/vip\/page-config['"]/)
  assert.match(service, /module\.exports\s*=\s*\{[^}]*pageConfig/)
  assert.match(markup, /VIP 权益/)
  assert.match(markup, /wx:for="\{\{pageConfig\.benefits\}\}"/)
  assert.match(markup, /wx:key="\*this"/)
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

test('页面配置失败可独立重试且不清空套餐和会员信息', () => {
  const logic = read('pages/vip-plans/vip-plans.js')
  const markup = read('pages/vip-plans/vip-plans.wxml')
  const start = logic.indexOf('  loadPageConfig() {')
  const end = logic.indexOf('buy(event)')
  const loader = logic.slice(start, end)

  assert.ok(start >= 0 && end > start)
  assert.match(loader, /configError/)
  assert.match(loader, /VIP 权益介绍加载失败，请重试/)
  assert.doesNotMatch(loader, /plans\s*:/)
  assert.doesNotMatch(loader, /profile\s*:/)
  assert.match(markup, /configError/)
  assert.match(markup, /bindtap="loadPageConfig"/)
})
