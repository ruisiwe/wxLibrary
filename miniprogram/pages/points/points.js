const points = require('../../services/point');

function todayKey() {
  const date = new Date();
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
}

Page({
  data: { balance: 0, rules: [], records: [], adCount: 0, loading: true, error: '' },
  onLoad() { this.restoreAdCount(); this.load(); },
  restoreAdCount() {
    const cached = wx.getStorageSync('wx-library-ad-count');
    this.setData({ adCount: cached && cached.date === todayKey() ? cached.count : 0 });
  },
  load() {
    this.setData({ loading: true, error: '' });
    Promise.all([points.balance(), points.rules(), points.records({ pageNum: 1, pageSize: 30 })])
      .then(([balance, rules, records]) => this.setData({ balance: balance.pointBalance || 0, rules: rules || [], records: records.items || [], loading: false }))
      .catch(error => this.setData({ loading: false, error: error.message }));
  },
  signIn() { points.signIn().then(() => { wx.showToast({ title: '签到成功' }); this.load(); }).catch(error => wx.showToast({ title: error.message, icon: 'none' })); },
  watchAd() {
    if (this.data.adCount >= 5) return wx.showToast({ title: '今天最多观看 5 次', icon: 'none' });
    const adUnitId = getApp().globalData.rewardedAdUnitId;
    if (!adUnitId) return wx.showToast({ title: '激励视频暂未配置', icon: 'none' });
    const ad = wx.createRewardedVideoAd({ adUnitId });
    ad.onClose(result => {
      if (!result || !result.isEnded) return wx.showToast({ title: '完整观看后才能领取积分', icon: 'none' });
      const adBizNo = `ad-${Date.now()}-${Math.random().toString(16).slice(2)}`;
      points.rewardAd(adBizNo).then(() => {
        const adCount = this.data.adCount + 1;
        wx.setStorageSync('wx-library-ad-count', { date: todayKey(), count: adCount });
        this.setData({ adCount }); this.load();
      }).catch(error => wx.showToast({ title: error.message, icon: 'none' }));
    });
    ad.show().catch(() => ad.load().then(() => ad.show()));
  },
  invite() { wx.showShareMenu({ menus: ['shareAppMessage'] }); wx.showToast({ title: '请点击右上角分享邀请', icon: 'none' }); },
  onShareAppMessage() {
    points.share().then(() => this.load()).catch(() => {});
    return { title: '微信图书馆，文档与课程随时学习', path: '/pages/index/index' };
  }
});
