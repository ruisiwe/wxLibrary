const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function loadPage() {
  const source = fs.readFileSync(
    path.resolve(__dirname, '../pages/document-detail/document-detail.js'),
    'utf8'
  );
  let definition;
  const shareMenus = [];
  vm.runInNewContext(source, {
    Page: value => { definition = value; },
    wx: { showShareMenu: options => shareMenus.push(options) },
    require: () => ({})
  });
  return { definition, shareMenus };
}

function pageContext(data) {
  return {
    data,
    setData(values) { this.data = { ...this.data, ...values }; },
    load() {}
  };
}

test('文档详情页右上角同时开启好友和朋友圈分享', () => {
  const { definition, shareMenus } = loadPage();
  const page = pageContext({ id: '', document: null });

  definition.onLoad.call(page, { id: '42' });

  assert.deepEqual(JSON.parse(JSON.stringify(shareMenus)), [
    { menus: ['shareAppMessage', 'shareTimeline'] }
  ]);
  assert.equal(typeof definition.onShareAppMessage, 'function');
  assert.equal(typeof definition.onShareTimeline, 'function');
});

test('文档详情页分享当前文档并在缺少文档时使用兜底标题', () => {
  const { definition } = loadPage();
  const page = pageContext({
    id: '42',
    document: { title: '测试文档', coverUrl: 'https://cdn.example.test/cover.jpg' }
  });

  assert.deepEqual(JSON.parse(JSON.stringify(definition.onShareAppMessage.call(page))), {
    title: '测试文档',
    path: '/pages/document-detail/document-detail?id=42',
    imageUrl: 'https://cdn.example.test/cover.jpg'
  });
  assert.deepEqual(JSON.parse(JSON.stringify(definition.onShareTimeline.call(page))), {
    title: '测试文档',
    query: 'id=42',
    imageUrl: 'https://cdn.example.test/cover.jpg'
  });

  page.data.document = null;
  assert.equal(definition.onShareAppMessage.call(page).title, '文档详情');
  assert.equal(definition.onShareTimeline.call(page).title, '文档详情');
});
