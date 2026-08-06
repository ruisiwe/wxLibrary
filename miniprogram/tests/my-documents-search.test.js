const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { filterDocumentsByTitle } = require('../services/document');

test('我的文档按标题实时本地过滤并保留原顺序', () => {
  const source = [
    { id: 1, title: 'Quality Manual' },
    { id: 2, title: '实验室质量控制' },
    { id: 3, title: null }
  ];
  const snapshot = source.slice();

  assert.deepEqual(filterDocumentsByTitle(source, ' quality '), [source[0]]);
  assert.deepEqual(filterDocumentsByTitle(source, '质量'), [source[1]]);
  assert.deepEqual(filterDocumentsByTitle(source, ''), source);
  assert.deepEqual(source, snapshot);
  assert.notEqual(filterDocumentsByTitle(source, ''), source);
});

test('非数组输入和空标题安全返回', () => {
  assert.deepEqual(filterDocumentsByTitle(null, '质量'), []);
  assert.deepEqual(filterDocumentsByTitle([{ id: 1 }, { id: 2, title: '文档' }], '文档'),
    [{ id: 2, title: '文档' }]);
});

test('我的文档输入时仅遍历已加载列表并区分空状态', () => {
  const page = fs.readFileSync(path.resolve(__dirname, '../pages/my-documents/my-documents.js'), 'utf8');
  const template = fs.readFileSync(path.resolve(__dirname, '../pages/my-documents/my-documents.wxml'), 'utf8');

  assert.match(page, /allItems:\s*\[\]/);
  assert.match(page, /searchKeyword:\s*''/);
  assert.equal((page.match(/documents\.unlocked\(\)/g) || []).length, 1,
    '页面加载接口只能调用一次，输入事件不能请求后台');
  assert.match(page, /onSearchInput[\s\S]*filterDocumentsByTitle\(this\.data\.allItems/);
  assert.match(template, /bindinput="onSearchInput"/);
  assert.doesNotMatch(template, /bindtap="search"|>搜索<\/button>/);
  assert.match(template, /未找到相关文档/);
  assert.match(template, /还没有兑换文档/);
});

test('我的文档和分类列表复用共享文件类型标签', () => {
  const myDocuments = fs.readFileSync(path.resolve(__dirname, '../pages/my-documents/my-documents.wxml'), 'utf8');
  const category = fs.readFileSync(path.resolve(__dirname, '../pages/category/category.wxml'), 'utf8');
  const component = fs.readFileSync(path.resolve(__dirname, '../components/document-row/index.wxml'), 'utf8');

  assert.match(myDocuments, /<document-row[^>]*show-file-type="\{\{true\}\}"/);
  assert.match(category, /<document-row[^>]*show-file-type="\{\{true\}\}"/);
  assert.match(component, /class="row__file-type"/);
  assert.doesNotMatch(myDocuments, /class="row__file-type"/);
  assert.doesNotMatch(category, /class="row__file-type"/);
});
