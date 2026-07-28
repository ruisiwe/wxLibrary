const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

function read(relativePath) {
  return fs.readFileSync(path.resolve(__dirname, '..', relativePath), 'utf8');
}

test('无本地令牌时先按 openid 静默登录，首次用户才展示头像登录', () => {
  const auth = read('services/auth.js');
  const profile = read('pages/profile/profile.js');
  const detail = read('pages/document-detail/document-detail.js');

  assert.match(auth, /function silentLogin\(/, '登录服务应提供静默登录方法');
  assert.match(auth, /firstLoginRequired\s*=\s*true/, '静默登录应标记首次登录错误');
  assert.match(auth, /首次登录必须上传有效头像/, '首次登录判定应来自后端头像错误');

  assert.match(profile, /auth\.silentLogin\(\)/, '我的页无本地令牌时应先静默登录');
  assert.doesNotMatch(profile, /if \(session\.getToken\(\)\) return this\.load\(\);\s*this\.openFirstLogin\(\)/,
    '我的页不能无令牌时直接展示头像登录');

  assert.match(detail, /auth\.silentLogin\(\)/, '文档详情受保护动作应先静默登录');
  assert.match(detail, /firstLoginRequired/, '文档详情仅在首次用户场景展示头像登录');
});
