const qr = require('../../services/qr');

Page({
  data: {
    id: 0,
    config: null,
    imagePath: '',
    loading: true,
    error: ''
  },
  onLoad(options) {
    const rawId = options && options.id;
    if (!/^[1-9]\d*$/.test(rawId || '')) {
      this.setData({ loading: false, error: '二维码配置编号不正确' });
      return;
    }
    this.setData({ id: Number(rawId) });
    this.load();
  },
  load() {
    const id = this.data.id;
    if (!id) return;
    this.setData({ loading: true, error: '', imagePath: '' });
    qr.detail(id).then(config => {
      wx.setNavigationBarTitle({ title: config.menuName || '二维码' });
      this.setData({ config });
      if (!config.imageConfigured) {
        this.setData({ loading: false });
        return null;
      }
      return qr.downloadImage(id).then(imagePath => {
        this.setData({ imagePath, loading: false });
      });
    }).catch(error => {
      this.setData({
        loading: false,
        error: error.message || '二维码加载失败，请重试'
      });
    });
  },
  previewImage() {
    if (!this.data.imagePath) return;
    wx.previewImage({
      current: this.data.imagePath,
      urls: [this.data.imagePath]
    });
  }
});
