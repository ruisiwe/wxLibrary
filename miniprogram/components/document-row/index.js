Component({
  properties: { document: { type: Object, value: {} } },
  methods: {
    open() {
      wx.navigateTo({ url: `/pages/document-detail/document-detail?id=${this.data.document.id}` });
    }
  }
});
