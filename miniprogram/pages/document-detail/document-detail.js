const documents = require('../../services/document');
const auth = require('../../services/auth');
const vip = require('../../services/vip');
const session = require('../../store/session');
const { request } = require('../../services/request');

Page({
  data: {
    id: '', document: null, unlocked: false, favorite: false, loading: true, error: '',
    vipActive: false, vipFreeDocument: false,
    loginVisible: false, profileRequired: true, agreements: [], pendingAction: '', disclaimerVisible: false,
    fileDisclaimer: null, suppressReminder: false, sendingOriginal: false
  },
  onLoad(options) { this.setData({ id: options.id }); this.load(); },
  load() {
    this.setData({ loading: true, error: '' });
    documents.detail(this.data.id).then(document => {
      this.setData({ document, vipFreeDocument: this.isVipFreeDocument(document), loading: false });
      if (!session.getToken()) return;
      return Promise.all([documents.unlocked(), documents.favorites(), vip.profile()]).then(([unlocked, favorites, profile]) => {
        const matches = items => (items || []).some(item => String(item.id || item.documentId) === String(this.data.id));
        this.setData({ unlocked: matches(unlocked), favorite: matches(favorites), vipActive: Boolean(profile.vipActive) });
      }).catch(() => {});
    }).catch(error => this.setData({ loading: false, error: error.message }));
  },
  isVipFreeDocument(document) {
    return document && document.accessType === 'VIP_FREE';
  },
  requireLogin(action) {
    if (session.getToken()) return true;
    this.setData({ pendingAction: action });
    this.trySilentLoginForAction(action);
    return false;
  },
  trySilentLoginForAction(action) {
    auth.silentLogin().then(state => {
      if (state.user && state.user.agreementRequired) return this.openAgreementForAction();
      this.setData({ loginVisible: false, pendingAction: '', profileRequired: true });
      if (action && typeof this[action] === 'function') this[action]();
    }).catch(error => {
      if (error.firstLoginRequired) return this.openFirstLoginForAction();
      this.setData({ loginVisible: false, pendingAction: '', profileRequired: true });
      wx.showToast({ title: error.message || '登录失败，请稍后重试', icon: 'none' });
    });
  },
  openFirstLoginForAction() {
    request({ url: '/wx/public/agreements/current', protected: false })
      .then(agreements => this.setData({ agreements, loginVisible: true, profileRequired: true }))
      .catch(error => {
        this.setData({ pendingAction: '', loginVisible: false, profileRequired: true });
        wx.showToast({ title: error.message, icon: 'none' });
      });
  },
  openAgreementForAction() {
    request({ url: '/wx/public/agreements/current', protected: false })
      .then(agreements => this.setData({ agreements, loginVisible: true, profileRequired: false }))
      .catch(error => {
        this.setData({ pendingAction: '', loginVisible: false, profileRequired: true });
        wx.showToast({ title: error.message, icon: 'none' });
      });
  },
  closeLogin(event) {
    if (!this.data.profileRequired) {
      session.clear();
      if (event && event.detail && event.detail.rejected)
        wx.showToast({ title: '需要同意隐私协议后继续使用', icon: 'none' });
    }
    this.setData({ loginVisible: false, pendingAction: '', profileRequired: true });
  },
  submitLogin(event) {
    const privacy = this.data.agreements.find(item => item.agreementType === 'PRIVACY');
    if (!this.data.profileRequired) {
      return request({ url: '/wx/agreements/accept', method: 'POST', data: {
        privacyAccepted: true, privacyVersion: privacy && privacy.version
      }}).then(() => {
        const action = this.data.pendingAction;
        this.setData({ loginVisible: false, pendingAction: '', profileRequired: true });
        if (action && typeof this[action] === 'function') this[action]();
      }).catch(error => wx.showToast({ title: error.message, icon: 'none' }));
    }
    wx.login({
      success: result => auth.firstLogin({
        code: result.code, nickname: event.detail.nickname, avatarPath: event.detail.avatarPath
      }).then(state => {
        if (state.user && state.user.agreementRequired) {
          this.setData({ profileRequired: false });
          return;
        }
        const action = this.data.pendingAction;
        this.setData({ loginVisible: false, pendingAction: '', profileRequired: true });
        if (action && typeof this[action] === 'function') this[action]();
      }).catch(error => wx.showToast({ title: error.message, icon: 'none' })),
      fail: () => wx.showToast({ title: '微信登录失败，请重试', icon: 'none' })
    });
  },
  preview() {
    if (!this.requireLogin('preview')) return;
    wx.navigateTo({ url: `/pages/document-reader/document-reader?id=${this.data.id}` });
  },
  unlock() {
    if (!this.requireLogin('unlock')) return;
    const pointPrice = this.data.document.pointPrice || 0;
    const vipFree = this.data.vipFreeDocument && this.data.vipActive;
    wx.showModal({ title: vipFree ? '会员免费下载' : '积分兑换', content: vipFree ? '确认免费下载该会员免费文档？' : `确认使用 ${pointPrice} 积分永久兑换该文档？`, success: result => {
      if (!result.confirm) return;
      const requestId = `unlock-${this.data.id}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
      documents.unlock(this.data.id, requestId).then(() => {
        this.setData({ unlocked: true });
        wx.showToast({ title: vipFree ? '已解锁' : '兑换成功' });
      }).catch(error => wx.showToast({ title: error.message, icon: 'none' }));
    }});
  },
  toggleFavorite() {
    if (!this.requireLogin('toggleFavorite')) return;
    const operation = this.data.favorite ? documents.unfavorite : documents.favorite;
    operation(this.data.id).then(result => this.setData({ favorite: result.favorite }))
      .catch(error => wx.showToast({ title: error.message, icon: 'none' }));
  },
  shareOriginal() {
    if (!this.requireLogin('shareOriginal')) return;
    documents.fileDisclaimer().then(disclaimer => {
      if (disclaimer.reminderSuppressed) return this.sendOriginal(disclaimer, false, true);
      this.setData({ fileDisclaimer: disclaimer, disclaimerVisible: true, suppressReminder: false });
    }).catch(error => wx.showToast({ title: error.message || '免责声明加载失败，请重试', icon: 'none' }));
  },
  onSuppressReminderChange(event) {
    this.setData({ suppressReminder: (event.detail.value || []).includes('suppress') });
  },
  cancelDisclaimer() {
    this.setData({ disclaimerVisible: false, fileDisclaimer: null, suppressReminder: false });
  },
  confirmDisclaimer() {
    const disclaimer = this.data.fileDisclaimer;
    if (!disclaimer || this.data.sendingOriginal) return;
    this.setData({ disclaimerVisible: false });
    this.sendOriginal(disclaimer, true, this.data.suppressReminder);
  },
  sendOriginal(disclaimer, confirmed, reminderSuppressed) {
    this.setData({ sendingOriginal: true });
    return documents.original(this.data.id, {
      agreementId: disclaimer.agreementId,
      agreementVersion: disclaimer.agreementVersion,
      confirmed,
      reminderSuppressed
    }).then(file => this.showLocalOpenNotice().then(() => file))
      .then(file => new Promise((resolve, reject) => {
        wx.downloadFile({
          url: file.url,
          success: result => result.statusCode === 200
            ? resolve(result.tempFilePath) : reject(new Error('原文件下载失败')),
          fail: reject
        });
      }))
      .then(filePath => wx.shareFileMessage(documents.buildShareOptions(filePath)))
      .catch(error => wx.showToast({ title: error.message || '发送失败，请重试', icon: 'none' }))
      .finally(() => this.setData({
        sendingOriginal: false, fileDisclaimer: null, suppressReminder: false
      }));
  },
  showLocalOpenNotice() {
    return new Promise(resolve => wx.showModal({
      title: '文件使用提示',
      content: '请将原文件发送到本地后打开',
      showCancel: false,
      success: resolve,
      fail: resolve
    }));
  }
});
