const documents = require('../../services/document');

Page({
  data: { banners: [], categories: [], documents: [], loading: true, error: '' },
  onLoad() { this.load(); },
  onShow() {
    if (this.getTabBar) this.getTabBar().setData({ value: '/pages/index/index' });
  },
  load() {
    this.setData({ loading: true, error: '' });
    documents.home().then(data => this.setData({
      banners: data.banners || [], categories: (data.categories || []).slice(0, 8),
      documents: data.documents || [], loading: false
    })).catch(error => this.setData({ loading: false, error: error.message }));
  }
});
