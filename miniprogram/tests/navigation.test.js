const test = require('node:test');
const assert = require('node:assert/strict');
const app = require('../app.json');

test('底部导航固定为首页、视频课程、我的', () => {
  assert.deepEqual(app.tabBar.list.map(item => item.text), ['首页', '视频课程', '我的']);
  assert.equal(app.tabBar.list.some(item => item.text === '分类'), false);
});
