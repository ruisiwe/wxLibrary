const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const app = require('../app.json');

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8');
}

test('VIP 主页面展示会员积分和三类任务但不展示套餐', () => {
  const logic = read('pages/vip/vip.js');
  const markup = read('pages/vip/vip.wxml');

  assert.match(markup, /当前积分/);
  assert.match(markup, /每日签到/);
  assert.match(markup, /激励视频/);
  assert.match(markup, /分享邀请/);
  assert.match(logic, /points\.balance/);
  assert.match(logic, /points\.rules/);
  assert.match(logic, /points\.signIn/);
  assert.doesNotMatch(logic, /vip\.plans/);
  assert.doesNotMatch(markup, /wx:for="\{\{plans\}\}"/);
});

test('VIP 套餐和支付逻辑位于子页面', () => {
  assert.ok(app.pages.includes('pages/vip-plans/vip-plans'));
  const logic = read('pages/vip-plans/vip-plans.js');
  const markup = read('pages/vip-plans/vip-plans.wxml');

  assert.match(logic, /vip\.plans/);
  assert.match(logic, /vip\.createOrder/);
  assert.match(logic, /wx\.requestPayment/);
  assert.match(markup, /wx:for="\{\{plans\}\}"/);
});

test('VIP 主页面提供套餐和积分明细入口', () => {
  const logic = read('pages/vip/vip.js');
  assert.match(logic, /pages\/vip-plans\/vip-plans/);
  assert.match(logic, /pages\/points\/points/);
});

test('个人中心移除重复入口且积分子页面只展示流水', () => {
  const profileMarkup = read('pages/profile/profile.wxml');
  const pointsLogic = read('pages/points/points.js');
  const pointsMarkup = read('pages/points/points.wxml');

  assert.doesNotMatch(profileMarkup, /pages\/vip\/vip/);
  assert.doesNotMatch(profileMarkup, /pages\/points\/points/);
  assert.match(pointsMarkup, /积分流水/);
  assert.match(pointsLogic, /points\.records/);
  assert.doesNotMatch(pointsMarkup, /每日签到|激励视频|分享与邀请/);
});
