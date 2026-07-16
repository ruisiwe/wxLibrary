Component({
  data: {
    value: '/pages/index/index',
    list: [
      { value: '/pages/index/index', text: '首页', icon: 'home' },
      { value: '/pages/courses/courses', text: '视频课程', icon: 'play-circle' },
      { value: '/pages/profile/profile', text: '我的', icon: 'user' }
    ]
  },

  pageLifetimes: {
    show() {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      if (currentPage) {
        this.setData({ value: `/${currentPage.route}` });
      }
    }
  },

  methods: {
    onChange(event) {
      const url = event.detail.value;
      this.setData({ value: url });
      wx.switchTab({ url });
    }
  }
});
