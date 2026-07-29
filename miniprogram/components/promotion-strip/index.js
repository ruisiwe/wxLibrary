Component({
  properties: { items: { type: Array, value: [] } },
  methods: {
    open(event) {
      const item = event.currentTarget.dataset.item;
      const documentId = Number(item && item.documentId);
      if (!Number.isInteger(documentId) || documentId <= 0) return;
      wx.navigateTo({ url: `/pages/document-detail/document-detail?id=${documentId}` });
    }
  }
});
