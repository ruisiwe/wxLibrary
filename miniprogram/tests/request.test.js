const test = require('node:test');
const assert = require('node:assert/strict');
const { buildHeaders, unwrapResponse, request } = require('../services/request');
const session = require('../store/session');

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

test('未配置服务地址时拒绝发起网络请求', async () => {
  global.getApp = () => ({ globalData: { apiBaseUrl: '' } });
  global.wx = { request() { assert.fail('不应调用 wx.request'); } };
  await assert.rejects(() => request({ url: '/wx/public/home', protected: false }), /小程序服务地址尚未配置/);
  delete global.getApp;
  delete global.wx;
});

test('令牌过期或用户停用后立即清理本地会话', async () => {
  const originalClear = session.clear;
  let cleared = 0;
  session.clear = () => { cleared += 1; };
  global.getApp = () => ({ globalData: { apiBaseUrl: 'https://library.test' } });
  global.wx = { request(options) { options.success({ statusCode: 200, data: { code: 40301, message: '用户已停用', data: null } }); } };
  await assert.rejects(() => request({ url: '/wx/profile', protected: false }), /用户已停用/);
  assert.equal(cleared, 1);
  session.clear = originalClear;
  delete global.getApp;
  delete global.wx;
});
