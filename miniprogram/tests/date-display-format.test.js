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
