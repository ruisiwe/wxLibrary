const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { courseLabel, canPlay, mergeMyCourses } = require('../services/course');

test('课程标签和播放条件按互斥类型计算', () => {
  assert.equal(courseLabel({ accessType: 'VIP' }), 'VIP 可看');
  assert.equal(courseLabel({ accessType: 'CODE' }), '课程码兑换');
  assert.equal(canPlay({ accessType: 'CODE', hasCodeGrant: true, vipActive: false }), true);
  assert.equal(canPlay({ accessType: 'VIP', hasCodeGrant: false, vipActive: false }), false);
});

test('视频播放结束后卸载页面不会覆盖完成状态', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/course-player/course-player.js'), 'utf8');
  assert.match(source, /if \(!this\.finishedSaved\) this\.persist\(false\)/);
  assert.match(source, /this\.finishedSaved = true; this\.persist\(true\)/);
});

test('我的课程合并会员可见课程和永久课程码授权并标明来源', () => {
  const result = mergeMyCourses(
    [{ id: 1, accessType: 'VIP' }, { id: 2, accessType: 'CODE' }],
    [{ courseId: 2 }], true
  );
  assert.deepEqual(result.map(item => item.sourceLabel), ['VIP 会员', '课程码永久权限']);
});
