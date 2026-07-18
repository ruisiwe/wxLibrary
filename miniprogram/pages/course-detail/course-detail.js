const courses = require('../../services/course');
const vip = require('../../services/vip');
const session = require('../../store/session');

Page({
  data: { id: '', course: null, videos: [], grants: [], vipActive: false, loading: true, error: '' },
  onLoad(options) { this.setData({ id: Number(options.id) }); this.load(); },
  load() {
    this.setData({ loading: true, error: '' });
    courses.list().then(items => {
      const course = (items || []).find(item => item.id === this.data.id);
      if (!course) throw new Error('课程不存在或已停用');
      return courses.videos(this.data.id).then(videos => ({ course, videos }));
    }).then(data => {
      this.setData({ ...data, loading: false });
      if (session.getToken()) this.loadAccess();
    }).catch(error => this.setData({ loading: false, error: error.message }));
  },
  loadAccess() {
    Promise.all([vip.profile(), courses.mine()]).then(([profile, grants]) => this.setData({
      vipActive: Boolean(profile.vipActive), grants: grants || []
    })).catch(() => {});
  },
  play(event) {
    if (!session.getToken()) return wx.switchTab({ url: '/pages/profile/profile' });
    const course = this.data.course;
    const hasCodeGrant = this.data.grants.some(item => item.courseId === course.id);
    if (!courses.canPlay({ accessType: course.accessType, hasCodeGrant, vipActive: this.data.vipActive })) {
      return wx.navigateTo({ url: course.accessType === 'VIP' ? '/pages/vip/vip' : `/pages/redeem-course/redeem-course?courseId=${course.id}` });
    }
    wx.navigateTo({ url: `/pages/course-player/course-player?videoId=${event.currentTarget.dataset.id}` });
  }
});
