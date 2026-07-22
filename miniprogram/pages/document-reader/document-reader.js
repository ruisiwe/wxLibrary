const documents = require('../../services/document');
Page({
  data: { id: '', loading: true, error: '' },
  onLoad(options) { this.setData({ id: options.id }); this.open(); },
  open() {
    this.setData({ loading: true, error: '' });
    documents.preview(this.data.id).then(file => new Promise((resolve, reject) => {
      wx.downloadFile({ url: file.url, success: result => result.statusCode === 200 ? resolve(result.tempFilePath) : reject(new Error('文档下载失败')), fail: reject });
    })).then(filePath => wx.openDocument({ filePath, fileType: 'pdf', showMenu: false, success: () => this.setData({ loading: false }), fail: () => this.setData({ loading: false, error: '文档打开失败，请重试' }) }))
      .catch(error => this.setData({ loading: false, error: error.message || '文档加载失败' }));
  }
});
