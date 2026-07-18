const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { resolveDocumentAction, canPreview } = require('../services/document');

test('未登录用户浏览元数据时不强制登录，预览时才登录', () => {
  assert.equal(resolveDocumentAction({ loggedIn: false, unlocked: false }), 'LOGIN');
  assert.equal(canPreview({ loggedIn: false }), false);
});

test('未兑换文档不能请求完整文件', () => {
  assert.equal(resolveDocumentAction({ loggedIn: true, unlocked: false }), 'PREVIEW');
  assert.equal(resolveDocumentAction({ loggedIn: true, unlocked: true }), 'FULL');
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
