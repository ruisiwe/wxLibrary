const courses = require('../../services/course');

Page({
  data: { items: [], loading: true, error: '' },
  onLoad() { this.load(); },
  onShow() {
    if (this.getTabBar) this.getTabBar().setData({ value: '/pages/courses/courses' });
  },
  load() {
    this.setData({ loading: true, error: '' });
    courses.list().then(items => this.setData({ items: items || [], loading: false }))
      .catch(error => this.setData({ loading: false, error: error.message }));
  },
  open(event) { wx.navigateTo({ url: `/pages/course-detail/course-detail?id=${event.currentTarget.dataset.id}` }); }
});
