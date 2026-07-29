const vip = require('../../services/vip');

Page({
  data: {
    profile: {},
    loading: true,
    error: '',
    pageConfig: {
      benefits: [],
      customerServiceTip: '',
      customerServiceImageUrl: ''
    },
    configLoading: true,
    configError: ''
  },
  onShow() {
    this.load();
    this.loadPageConfig();
  },
  load() {
    this.setData({ loading: true, error: '' });
    vip.profile()
      .then(profile => this.setData({ profile: profile || {}, loading: false }))
      .catch(error => this.setData({
        loading: false,
        error: error.message || '会员信息加载失败，请重试'
      }));
  },
  loadPageConfig() {
    this.setData({ configLoading: true, configError: '' });
    return vip.pageConfig()
      .then(pageConfig => this.setData({
        pageConfig: {
          benefits: [],
          customerServiceTip: '',
          customerServiceImageUrl: '',
          ...(pageConfig || {})
        },
        configLoading: false
      }))
      .catch(error => this.setData({
        configLoading: false,
        configError: error.message || 'VIP 权益介绍加载失败，请重试'
      }));
  }
});
