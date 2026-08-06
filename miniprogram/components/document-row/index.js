Component({
  properties: {
    document: {
      type: Object,
      value: {},
      observer(document) {
        const fileFormat = document && document.fileFormat;
        this.setData({
          fileType: (fileFormat || '').trim().toUpperCase()
        });
      }
    },
    showFileType: {
      type: Boolean,
      value: false
    }
  },
  data: {
    fileType: ''
  },
  methods: {
    open() {
      wx.navigateTo({ url: `/pages/document-detail/document-detail?id=${this.data.document.id}` });
    }
  }
});
