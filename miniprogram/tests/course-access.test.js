const test = require('node:test');
const assert = require('node:assert/strict');
const { courseLabel, canPlay, mergeMyCourses } = require('../services/course');

test('课程标签和播放条件按互斥类型计算', () => {
  assert.equal(courseLabel({ accessType: 'VIP' }), 'VIP 可看');
  assert.equal(courseLabel({ accessType: 'CODE' }), '课程码兑换');
  assert.equal(canPlay({ accessType: 'CODE', hasCodeGrant: true, vipActive: false }), true);
  assert.equal(canPlay({ accessType: 'VIP', hasCodeGrant: false, vipActive: false }), false);
});

test('我的课程合并会员可见课程和永久课程码授权并标明来源', () => {
  const result = mergeMyCourses(
    [{ id: 1, accessType: 'VIP' }, { id: 2, accessType: 'CODE' }],
    [{ courseId: 2 }], true
  );
  assert.deepEqual(result.map(item => item.sourceLabel), ['VIP 会员', '课程码永久权限']);
});
