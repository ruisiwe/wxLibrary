const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const miniRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(miniRoot, '..');
const configFiles = ['package.json', 'package-lock.json', 'project.config.json'];

test('小程序配置只保存在小程序目录', () => {
  for (const name of configFiles) {
    assert.equal(fs.existsSync(path.join(miniRoot, name)), true, `小程序目录缺少 ${name}`);
    assert.equal(fs.existsSync(path.join(repoRoot, name)), false, `仓库根目录不应保留 ${name}`);
  }
});

test('小程序配置路径以当前目录为根', () => {
  const pkg = require('../package.json');
  const project = require('../project.config.json');
  assert.equal(pkg.scripts.test, 'node --test tests/*.test.js');
  assert.equal(project.miniprogramRoot, './');
  assert.equal(project.compileType, 'miniprogram');
});
