const test = require('node:test');
const assert = require('node:assert/strict');
const { buildHeaders, unwrapResponse } = require('../services/request');

test('受保护接口只发送独立 Wx-Token', () => {
  const headers = buildHeaders({ wxToken: 'abc', ruoyiToken: 'admin-token' });
  assert.equal(headers['Wx-Token'], 'abc');
  assert.equal(headers.Authorization, undefined);
});

test('匿名请求不会携带空令牌', () => {
  assert.deepEqual(buildHeaders({}), { 'Content-Type': 'application/json' });
});

test('接口响应只在成功码时返回数据', () => {
  assert.deepEqual(unwrapResponse({ code: 0, data: { id: 1 } }), { id: 1 });
  assert.throws(() => unwrapResponse({ code: 401, message: '请先登录' }), /请先登录/);
});
