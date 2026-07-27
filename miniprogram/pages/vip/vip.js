const vip = require('../../services/vip');
const points = require('../../services/point');
const session = require('../../store/session');

function todayKey() {
  const date = new Date();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

const TASK_META = {
  SIGN_IN: { title: '每日签到', fallbackLimit: 1 },
  AD_REWARD: { title: '激励视频广告', fallbackLimit: 5 },
  SHARE: { title: '分享小程序', fallbackLimit: 1 },
  INVITE: { title: '邀请新用户', fallbackLimit: 1 }
};

Page({
  data: {
    loggedIn: false,
    profile: null,
    balance: 0,
    rules: [],
    taskRows: [],
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
    Promise.all([
      vip.profile(),
      points.balance(),
      points.rules(),
      points.records({ pageNum: 1, pageSize: 50 })
    ])
      .then(([profile, balance, rules, records]) => {
        const items = rules || [];
        const adRule = items.find(item => item.eventType === 'AD_REWARD');
        const configuredLimit = this.taskLimit(adRule || { eventType: 'AD_REWARD' });
        const taskRows = this.buildTaskRows(items, records && records.items);
        const adTask = taskRows.find(item => item.eventType === 'AD_REWARD');
        this.setData({
          profile,
          balance: balance.pointBalance || 0,
          rules: items,
          taskRows,
          adCount: adTask ? adTask.count : this.data.adCount,
          adLimit: configuredLimit,
          loading: false
        });
      })
      .catch(error => this.setData({ loading: false, error: error.message || 'VIP 信息加载失败，请重试' }));
  },
  buildTaskRows(rules, records) {
    const today = todayKey();
    const todayRecords = (records || []).filter(item => {
      const createTime = item.createTime || '';
      return typeof createTime === 'string' && createTime.indexOf(today) === 0;
    });
    return (rules || [])
      .filter(rule => TASK_META[rule.eventType])
      .map(rule => {
        const eventType = rule.eventType;
        const limit = this.taskLimit(rule);
        const count = Math.min(this.taskCount(eventType, todayRecords), limit);
        return {
          eventType,
          shareOpen: eventType === 'SHARE',
          title: (rule.ruleName || TASK_META[eventType].title),
          points: rule.pointValue || 0,
          count,
          limit,
          reachedLimit: count >= limit
        };
      });
  },
  taskLimit(rule) {
    const eventType = rule && rule.eventType;
    const fallback = TASK_META[eventType] ? TASK_META[eventType].fallbackLimit : 1;
    const configured = rule && rule.dailyLimit > 0 ? rule.dailyLimit : fallback;
    return eventType === 'AD_REWARD' ? Math.min(configured, 5) : configured;
  },
  taskCount(eventType, todayRecords) {
    if (eventType === 'AD_REWARD') {
      return Math.max(this.data.adCount, todayRecords.filter(item => item.eventType === eventType).length);
    }
    return todayRecords.some(item => item.eventType === eventType) ? 1 : 0;
  },
  taskByEvent(eventType) {
    return (this.data.taskRows || []).find(item => item.eventType === eventType);
  },
  ensureTaskAvailable(task) {
    if (!task) {
      wx.showToast({ title: '当前积分任务未启用', icon: 'none' });
      return false;
    }
    if (task.reachedLimit) {
      wx.showToast({ title: '今日任务已达上限', icon: 'none' });
      return false;
    }
    return true;
  },
  goLogin() { wx.switchTab({ url: '/pages/profile/profile' }); },
  openPlans() { wx.navigateTo({ url: '/pages/vip-plans/vip-plans' }); },
  openPoints() { wx.navigateTo({ url: '/pages/points/points' }); },
  handleTask(event) {
    const eventType = event.currentTarget.dataset.event;
    const task = this.taskByEvent(eventType);
    if (!this.ensureTaskAvailable(task)) return;
    if (eventType === 'SIGN_IN') return this.signIn();
    if (eventType === 'AD_REWARD') return this.watchAd();
    if (eventType === 'SHARE') return this.invite();
    wx.showToast({ title: '请将小程序分享给新用户完成邀请', icon: 'none' });
  },
  signIn() {
    if (!this.ensureTaskAvailable(this.taskByEvent('SIGN_IN'))) return;
    points.signIn()
      .then(() => { wx.showToast({ title: '签到成功' }); this.load(); })
      .catch(error => wx.showToast({ title: error.message || '签到失败，请重试', icon: 'none' }));
  },
  watchAd() {
    if (!this.ensureTaskAvailable(this.taskByEvent('AD_REWARD'))) return;
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
    if (!this.ensureTaskAvailable(this.taskByEvent('SHARE'))) return;
    wx.showShareMenu({ menus: ['shareAppMessage'] });
    wx.showToast({ title: '请点击右上角分享邀请', icon: 'none' });
  },
  onShareAppMessage() {
    const task = this.taskByEvent('SHARE');
    if (!this.ensureTaskAvailable(task)) return { title: '微信图书馆，优质文档随时学习', path: '/pages/index/index' };
    points.share().then(() => this.load())
      .catch(error => wx.showToast({ title: error.message || '分享奖励领取失败', icon: 'none' }));
    return { title: '微信图书馆，优质文档随时学习', path: '/pages/index/index' };
  }
});
