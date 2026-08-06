const documents = require('../../services/document');
Page({
  data: {
    allItems: [],
    items: [],
    searchKeyword: '',
    loading: true,
    error: ''
  },
  onLoad() {
    this.load();
  },
  load() {
    this.setData({
      loading: true,
      error: ''
    });
    documents.unlocked().then(items => {
      const allItems = Array.isArray(items) ? items : [];
      this.setData({
        allItems,
        items: documents.filterDocumentsByTitle(allItems, this.data.searchKeyword),
        loading: false
      });
    }).catch(error => this.setData({
      loading: false,
      error: error.message
    }));
  },
  onSearchInput(event) {
    const searchKeyword = event.detail.value || '';
    this.setData({
      searchKeyword,
      items: documents.filterDocumentsByTitle(this.data.allItems, searchKeyword)
    });
  }
});
