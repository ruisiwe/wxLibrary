const documents = require('../../services/document');
Page({
  data: { keyword: '', items: [], loading: false, error: '' },
  onLoad(options) {
    const keyword = options && options.keyword ? decodeURIComponent(options.keyword) : '';
    if (keyword) this.setData({ keyword }, () => this.search());
  },
  input(event) { this.setData({ keyword: event.detail.value }); },
  search() {
    this.setData({ loading: true, error: '' });
    documents.list({ keyword: this.data.keyword.trim(), pageNum: 1, pageSize: 50 })
      .then(data => this.setData({ items: data.items || [], loading: false }))
      .catch(error => this.setData({ loading: false, error: error.message }));
  }
});
