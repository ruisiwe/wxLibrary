Component({
  data: {
    value: '/pages/index/index',
    list: [
      { value: '/pages/index/index', text: '首页', icon: 'home' },
      { value: '/pages/vip/vip', text: 'VIP', icon: 'user-vip' },
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
      const currentUrl = this.data.value;
      wx.switchTab({
        url,
        success: () => {
          this.setData({ value: url });
        },
        fail: () => {
          this.setData({ value: currentUrl });
        }
      });
    }
  }
});
