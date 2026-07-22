const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { resolveDocumentAction, canPreview } = require('../services/document');

test('未登录用户浏览元数据时不强制登录，预览时才登录', () => {
  assert.equal(resolveDocumentAction({ loggedIn: false, unlocked: false }), 'LOGIN');
  assert.equal(canPreview({ loggedIn: false }), false);
});

test('兑换前后在线阅读都只请求试看文件', () => {
  assert.equal(resolveDocumentAction({ loggedIn: true, unlocked: false }), 'PREVIEW');
  assert.equal(resolveDocumentAction({ loggedIn: true, unlocked: true }), 'PREVIEW');
  const service = fs.readFileSync(path.resolve(__dirname, '../services/document.js'), 'utf8');
  const page = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.wxml'), 'utf8');
  assert.doesNotMatch(service, /\/full/);
  assert.doesNotMatch(page, /打开完整 PDF/);
});

test('原文件分享不指定固定接收人', () => {
  const options = require('../services/document').buildShareOptions('/tmp/source.docx');
  assert.deepEqual(options, { filePath: '/tmp/source.docx' });
});

test('首页只渲染宣传、分类、文档三个内容区块', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/index/index.wxml'), 'utf8');
  assert.match(source, /<promotion-strip/);
  assert.match(source, /<category-grid/);
  assert.match(source, /<document-row/);
  assert.doesNotMatch(source, /课程|会员|积分中心/);
});

test('原文件分享调用不包含固定接收人字段', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  assert.match(source, /wx\.shareFileMessage\(documents\.buildShareOptions\(filePath\)\)/);
  assert.doesNotMatch(source, /toUser|openId|openid/);
});

test('原文件发送前使用后台免责声明和可选免提示确认', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  const template = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.wxml'), 'utf8');
  assert.match(source, /documents\.fileDisclaimer\(\)/);
  assert.match(source, /reminderSuppressed/);
  assert.match(template, /以后不再提示/);
  assert.match(template, /scroll-view/);
  assert.doesNotMatch(source, /本账号转载资源均收集于网络/);
});

test('我的页面无论登录与否都提供微信原生客服入口', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/profile/profile.wxml'), 'utf8');
  assert.match(source, /open-type="contact"/);
  assert.match(source, /联系客服/);
  assert.match(source, /<button class="contact" open-type="contact">联系客服<\/button><login-sheet/);
});

test('已登录用户重新进入详情时恢复兑换和收藏状态', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  assert.match(source, /Promise\.all\(\[documents\.unlocked\(\), documents\.favorites\(\)\]\)/);
  assert.match(source, /unlocked: matches\(unlocked\), favorite: matches\(favorites\)/);
});
