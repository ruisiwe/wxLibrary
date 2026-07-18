const documents = require('../../services/document');
Page({
  data: { id: '', items: [], loading: true, error: '' },
  onLoad(options) { this.setData({ id: options.id || '' }); this.load(); },
  load() {
    this.setData({ loading: true, error: '' });
    documents.list({ categoryId: this.data.id, pageNum: 1, pageSize: 50 })
      .then(data => this.setData({ items: data.items || [], loading: false }))
      .catch(error => this.setData({ loading: false, error: error.message }));
  }
});
