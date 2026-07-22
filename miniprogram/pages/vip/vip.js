const vip = require('../../services/vip');
const points = require('../../services/point');
const session = require('../../store/session');

function todayKey() {
  const date = new Date();
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
}

Page({
  data: {
    loggedIn: false,
    profile: null,
    balance: 0,
    rules: [],
    adCount: 0,
    adLimit: 5,
    loading: false,
    error: ''
  },
  onShow() {
    if (this.getTabBar) this.getTabBar().setData({ value: '/pages/vip/vip' });
    this.restoreAdCount();
    const loggedIn = Boolean(session.getToken());
    this.setData({ loggedIn });
    if (loggedIn) this.load();
    else this.setData({ profile: null, loading: false, error: '' });
  },
  restoreAdCount() {
    const cached = wx.getStorageSync('wx-library-ad-count');
    this.setData({ adCount: cached && cached.date === todayKey() ? cached.count : 0 });
  },
  load() {
    this.setData({ loading: true, error: '' });
    Promise.all([vip.profile(), points.balance(), points.rules()])
      .then(([profile, balance, rules]) => {
        const items = rules || [];
        const adRule = items.find(item => item.ruleCode === 'AD_REWARD');
        const configuredLimit = adRule && adRule.dailyLimit > 0 ? adRule.dailyLimit : 5;
        this.setData({
          profile,
          balance: balance.pointBalance || 0,
          rules: items,
          adLimit: Math.min(configuredLimit, 5),
          loading: false
        });
      })
      .catch(error => this.setData({ loading: false, error: error.message || 'VIP 信息加载失败，请重试' }));
  },
  goLogin() { wx.switchTab({ url: '/pages/profile/profile' }); },
  openPlans() { wx.navigateTo({ url: '/pages/vip-plans/vip-plans' }); },
  openPoints() { wx.navigateTo({ url: '/pages/points/points' }); },
  signIn() {
    points.signIn()
      .then(() => { wx.showToast({ title: '签到成功' }); this.load(); })
      .catch(error => wx.showToast({ title: error.message || '签到失败，请重试', icon: 'none' }));
  },
  watchAd() {
    if (this.data.adCount >= this.data.adLimit) {
      return wx.showToast({ title: `今天最多观看 ${this.data.adLimit} 次`, icon: 'none' });
    }
    const adUnitId = getApp().globalData.rewardedAdUnitId;
    if (!adUnitId) return wx.showToast({ title: '激励视频暂未配置', icon: 'none' });
    const ad = wx.createRewardedVideoAd({ adUnitId });
    ad.onClose(result => {
      if (!result || !result.isEnded) return wx.showToast({ title: '完整观看后才能领取积分', icon: 'none' });
      const adBizNo = `ad-${Date.now()}-${Math.random().toString(16).slice(2)}`;
      points.rewardAd(adBizNo).then(() => {
        const adCount = this.data.adCount + 1;
        wx.setStorageSync('wx-library-ad-count', { date: todayKey(), count: adCount });
        this.setData({ adCount });
        this.load();
      }).catch(error => wx.showToast({ title: error.message || '积分领取失败，请重试', icon: 'none' }));
    });
    ad.show().catch(() => ad.load().then(() => ad.show())
      .catch(() => wx.showToast({ title: '激励视频加载失败，请重试', icon: 'none' })));
  },
  invite() {
    wx.showShareMenu({ menus: ['shareAppMessage'] });
    wx.showToast({ title: '请点击右上角分享邀请', icon: 'none' });
  },
  onShareAppMessage() {
    points.share().then(() => this.load()).catch(() => {});
    return { title: '微信图书馆，优质文档随时学习', path: '/pages/index/index' };
  }
});
