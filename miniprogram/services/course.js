const { request } = require('./request');

function courseLabel(course) {
  return course && course.accessType === 'VIP' ? 'VIP 可看' : '课程码兑换';
}

function canPlay({ accessType, hasCodeGrant, vipActive }) {
  if (accessType === 'VIP') return Boolean(vipActive);
  return accessType === 'CODE' && Boolean(hasCodeGrant);
}

function mergeMyCourses(courses, grants, vipActive) {
  const codeIds = new Set((grants || []).map(item => item.courseId));
  return (courses || []).filter(course =>
    (course.accessType === 'VIP' && vipActive) ||
    (course.accessType === 'CODE' && codeIds.has(course.id))
  ).map(course => ({
    ...course,
    sourceLabel: course.accessType === 'VIP' ? 'VIP 会员' : '课程码永久权限'
  }));
}

const list = () => request({ url: '/wx/public/courses', protected: false });
const videos = courseId => request({ url: `/wx/public/courses/${courseId}/videos`, protected: false });
const mine = () => request({ url: '/wx/courses/mine' });
const redeem = code => request({ url: '/wx/courses/redeem', method: 'POST', data: { code } });
const play = videoId => request({ url: `/wx/courses/videos/${videoId}/play`, method: 'POST' });
const progress = () => request({ url: '/wx/courses/progress' });
const saveProgress = (videoId, progressSeconds, finished) => request({ url: `/wx/courses/videos/${videoId}/progress`, method: 'PUT', data: { progressSeconds, finished } });

module.exports = { courseLabel, canPlay, mergeMyCourses, list, videos, mine, redeem, play, progress, saveProgress };
