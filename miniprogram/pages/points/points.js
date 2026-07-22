const points = require('../../services/point');

Page({
  data: { balance: 0, records: [], loading: true, error: '' },
  onShow() { this.load(); },
  load() {
    this.setData({ loading: true, error: '' });
    Promise.all([points.balance(), points.records({ pageNum: 1, pageSize: 30 })])
      .then(([balance, records]) => this.setData({
        balance: balance.pointBalance || 0,
        records: records.items || [],
        loading: false
      }))
      .catch(error => this.setData({ loading: false, error: error.message || '积分明细加载失败，请重试' }));
  }
});
