const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const app = require('../app.json');
const packageConfig = require('../../package.json');

const expectedPages = [
  'pages/index/index',
  'pages/courses/courses',
  'pages/profile/profile'
];

const expectedTabs = [
  { pagePath: 'pages/index/index', text: '首页' },
  { pagePath: 'pages/courses/courses', text: '视频课程' },
  { pagePath: 'pages/profile/profile', text: '我的' }
];

function loadCustomTabBar(switchTab) {
  let definition;
  const sourcePath = path.resolve(__dirname, '../custom-tab-bar/index.js');
  const source = fs.readFileSync(sourcePath, 'utf8');

  vm.runInNewContext(source, {
    Component(options) {
      definition = options;
    },
    getCurrentPages() {
      return [];
    },
    wx: { switchTab }
  }, { filename: sourcePath });

  assert.ok(definition, '自定义导航组件必须完成注册');
  return definition;
}

function createComponentInstance(definition, value = '/pages/index/index') {
  return {
    data: { ...definition.data, value },
    setData(nextData) {
      Object.assign(this.data, nextData);
    }
  };
}

test('底部导航固定为首页、视频课程、我的', () => {
  assert.deepEqual(app.pages.slice(0, 3), expectedPages);
  assert.equal(app.tabBar.custom, true);
  assert.deepEqual(app.tabBar.list, expectedTabs);
});

test('自定义导航与应用导航配置一一对应', () => {
  const definition = loadCustomTabBar(() => {});
  const actualTabs = Array.from(definition.data.list, item => ({
    value: item.value,
    text: item.text
  }));
  const configuredTabs = app.tabBar.list.map(item => ({
    value: `/${item.pagePath}`,
    text: item.text
  }));

  assert.deepEqual(actualTabs, configuredTabs);
});

test('测试脚本声明 node:test 所需的 Node 版本', () => {
  assert.deepEqual(packageConfig.engines, { node: '>=18' });
});

test('切换成功后才更新导航高亮', () => {
  let request;
  const definition = loadCustomTabBar(options => {
    request = options;
  });
  const instance = createComponentInstance(definition);
  const target = '/pages/courses/courses';

  definition.methods.onChange.call(instance, { detail: { value: target } });

  assert.equal(request.url, target);
  assert.equal(instance.data.value, '/pages/index/index');
  request.success();
  assert.equal(instance.data.value, target);
});

test('切换失败时保留当前导航高亮', () => {
  const definition = loadCustomTabBar(options => {
    if (options.fail) {
      options.fail({ errMsg: 'switchTab:fail' });
    }
  });
  const instance = createComponentInstance(definition);

  definition.methods.onChange.call(instance, {
    detail: { value: '/pages/profile/profile' }
  });

  assert.equal(instance.data.value, '/pages/index/index');
});
