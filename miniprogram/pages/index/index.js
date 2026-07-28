const documents = require('../../services/document');

Page({
  data: { banners: [], categories: [], documents: [], searchKeyword: '', loading: true, error: '' },
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
  },
  onSearchInput(event) {
    this.setData({ searchKeyword: event.detail.value });
  },
  searchDocuments(event) {
    const inputValue = event && event.detail && typeof event.detail.value === 'string'
      ? event.detail.value : this.data.searchKeyword;
    const keyword = (inputValue || '').trim();
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
    wx.navigateTo({ url: `/pages/search/search${query}` });
  }
});
