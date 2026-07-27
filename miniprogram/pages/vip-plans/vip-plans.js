const vip = require('../../services/vip');

Page({
  data: {
    plans: [],
    profile: {},
    loading: true,
    error: '',
    paymentState: '',
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
    Promise.all([vip.plans(), vip.profile()])
      .then(([plans, profile]) => this.setData({ plans: plans || [], profile, loading: false }))
      .catch(error => this.setData({ loading: false, error: error.message || '会员套餐加载失败，请重试' }));
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
  },
  buy(event) {
    const planId = event.currentTarget.dataset.id;
    this.setData({ paymentState: '正在创建支付订单…' });
    vip.createOrder(planId).then(order => {
      this.pendingOrderNo = order.merchantOrderNo;
      return new Promise((resolve, reject) => wx.requestPayment({
        timeStamp: order.timeStamp,
        nonceStr: order.nonceStr,
        package: order.package,
        signType: order.signType,
        paySign: order.paySign,
        success: resolve,
        fail: reject
      }));
    }).then(() => {
      const next = vip.afterRequestPaymentSuccess();
      if (next.nextAction === 'QUERY_ORDER') this.pollOrder(0);
    }).catch(error => {
      const cancelled = error && /cancel/i.test(error.errMsg || '');
      this.setData({ paymentState: cancelled ? vip.paymentDisplayState('CANCELLED') : '支付未完成，请稍后重试' });
    });
  },
  pollOrder(attempt) {
    if (!this.pendingOrderNo) return;
    vip.queryOrder(this.pendingOrderNo).then(order => {
      this.setData({ paymentState: vip.paymentDisplayState(order.orderStatus) });
      if (order.orderStatus === 'PAID') return this.load();
      if (attempt < 5) setTimeout(() => this.pollOrder(attempt + 1), 1500);
    }).catch(() => this.setData({ paymentState: '支付结果确认中，请稍后在本页刷新' }));
  }
});
