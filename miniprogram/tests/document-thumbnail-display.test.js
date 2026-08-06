const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('文档列表缩略图使用正方形完整展示', () => {
  const template = fs.readFileSync(
    path.resolve(__dirname, '../components/document-row/index.wxml'),
    'utf8'
  );
  const style = fs.readFileSync(
    path.resolve(__dirname, '../components/document-row/index.wxss'),
    'utf8'
  );

  assert.match(template, /class="row__cover"[^>]*mode="aspectFit"/);
  assert.match(style, /\.row__cover\{[^}]*width:144rpx[^}]*height:144rpx/);
  assert.match(style, /\.row__cover\{[^}]*background:#fff/);
  assert.doesNotMatch(template, /mode="aspectFill"/);
});
