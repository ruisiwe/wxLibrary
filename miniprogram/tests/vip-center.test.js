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
  assert.match(logic, /每日签到/);
  assert.match(logic, /激励视频广告/);
  assert.match(logic, /分享小程序/);
  assert.match(logic, /points\.balance/);
  assert.match(logic, /points\.rules/);
  assert.match(logic, /points\.signIn/);
  assert.doesNotMatch(logic, /vip\.plans/);
  assert.doesNotMatch(markup, /wx:for="\{\{plans\}\}"/);
});

test('积分任务进度合并到任务列表并提示每日上限', () => {
  const logic = read('pages/vip/vip.js');
  const markup = read('pages/vip/vip.wxml');

  assert.match(markup, /wx:for="\{\{taskRows\}\}"/);
  assert.match(markup, /\{\{item\.title\}\}（\{\{item\.count\}\}\/\{\{item\.limit\}\}）/);
  assert.doesNotMatch(markup, /<button bindtap="signIn">每日签到<\/button>/);
  assert.doesNotMatch(markup, /<button bindtap="watchAd">激励视频/);
  assert.doesNotMatch(markup, /<button open-type="share" bindtap="invite">分享邀请<\/button>/);
  assert.match(logic, /taskRows/);
  assert.match(logic, /今日任务已达上限/);
  assert.match(logic, /points\.records/);
});

test('VIP 子页面暂不展示套餐和支付入口', () => {
  assert.ok(app.pages.includes('pages/vip-plans/vip-plans'));
  const service = read('services/vip.js');
  const logic = read('pages/vip-plans/vip-plans.js');
  const markup = read('pages/vip-plans/vip-plans.wxml');

  assert.match(logic, /vip\.profile/);
  assert.doesNotMatch(logic, /vip\.plans/);
  assert.doesNotMatch(logic, /vip\.createOrder/);
  assert.doesNotMatch(logic, /vip\.queryOrder/);
  assert.doesNotMatch(logic, /wx\.requestPayment/);
  assert.doesNotMatch(markup, /wx:for="\{\{plans\}\}"/);
  assert.doesNotMatch(markup, /购买\/续费/);
  assert.doesNotMatch(markup, /paymentState/);
  assert.match(service, /const plans =/);
  assert.match(service, /const createOrder =/);
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
