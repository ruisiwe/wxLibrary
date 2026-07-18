const test = require('node:test');
const assert = require('node:assert/strict');
const { afterRequestPaymentSuccess, paymentDisplayState } = require('../services/vip');

test('小程序支付结果只触发后端状态查询', () => {
  assert.equal(afterRequestPaymentSuccess().nextAction, 'QUERY_ORDER');
  assert.equal(afterRequestPaymentSuccess().grantVipLocally, false);
});

test('支付取消、确认中和已支付状态分别展示', () => {
  assert.equal(paymentDisplayState('CANCELLED'), '支付已取消');
  assert.equal(paymentDisplayState('PREPAY_READY'), '支付结果确认中');
  assert.equal(paymentDisplayState('PAID'), '会员已开通');
});
