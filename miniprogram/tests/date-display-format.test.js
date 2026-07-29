const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const { formatDate } = require('../utils/date')

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8')
}

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
