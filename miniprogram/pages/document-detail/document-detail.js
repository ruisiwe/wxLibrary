const documents = require('../../services/document');
const auth = require('../../services/auth');
const session = require('../../store/session');
const { request } = require('../../services/request');

Page({
  data: {
    id: '', document: null, unlocked: false, favorite: false, loading: true, error: '',
    loginVisible: false, agreements: [], pendingAction: '', disclaimerVisible: false,
    fileDisclaimer: null, suppressReminder: false, sendingOriginal: false
  },
  onLoad(options) { this.setData({ id: options.id }); this.load(); },
  load() {
    this.setData({ loading: true, error: '' });
    documents.detail(this.data.id).then(document => {
      this.setData({ document, loading: false });
      if (!session.getToken()) return;
      return Promise.all([documents.unlocked(), documents.favorites()]).then(([unlocked, favorites]) => {
        const matches = items => (items || []).some(item => String(item.id || item.documentId) === String(this.data.id));
        this.setData({ unlocked: matches(unlocked), favorite: matches(favorites) });
      }).catch(() => {});
    }).catch(error => this.setData({ loading: false, error: error.message }));
  },
  requireLogin(action) {
    if (session.getToken()) return true;
    this.setData({ pendingAction: action });
    request({ url: '/wx/public/agreements/current', protected: false })
      .then(agreements => this.setData({ agreements, loginVisible: true }))
      .catch(error => wx.showToast({ title: error.message, icon: 'none' }));
    return false;
  },
  closeLogin() { this.setData({ loginVisible: false, pendingAction: '' }); },
  submitLogin(event) {
    const privacy = this.data.agreements.find(item => item.agreementType === 'PRIVACY');
    const statement = this.data.agreements.find(item => item.agreementType === 'STATEMENT');
    wx.login({
      success: result => auth.firstLogin({
        code: result.code, nickname: event.detail.nickname, avatarPath: event.detail.avatarPath,
        privacyVersion: privacy && privacy.version, statementVersion: statement && statement.version
      }).then(() => {
        const action = this.data.pendingAction;
        this.setData({ loginVisible: false, pendingAction: '' });
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
    wx.showModal({ title: '积分兑换', content: `确认使用 ${pointPrice} 积分永久兑换该文档？`, success: result => {
      if (!result.confirm) return;
      const requestId = `unlock-${this.data.id}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
      documents.unlock(this.data.id, requestId).then(() => {
        this.setData({ unlocked: true });
        wx.showToast({ title: '兑换成功' });
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
