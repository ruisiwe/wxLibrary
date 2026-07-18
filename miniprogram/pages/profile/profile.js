const auth = require('../../services/auth');
const session = require('../../store/session');
const { request } = require('../../services/request');
const vip = require('../../services/vip');

Page({
  data: { profile: null, loginVisible: false, profileRequired: true, agreements: [] },
  onShow() {
    if (this.getTabBar) this.getTabBar().setData({ value: '/pages/profile/profile' });
    if (session.getToken()) this.load();
  },
  login() {
    wx.login({ success: result => auth.loginWithCode(result.code).then(state => {
      if (state.user && state.user.agreementRequired) return this.openAgreementUpdate();
      return this.load();
    }).catch(error => {
      if (/首次登录/.test(error.message || '')) return this.openFirstLogin();
      wx.showToast({ title: error.message || '登录失败，请重试', icon: 'none' });
    }), fail: () => wx.showToast({ title: '微信登录失败', icon: 'none' }) });
  },
  openFirstLogin() {
    request({ url: '/wx/public/agreements/current', protected: false }).then(agreements => this.setData({ agreements, loginVisible: true, profileRequired: true }))
      .catch(error => wx.showToast({ title: error.message, icon: 'none' }));
  },
  openAgreementUpdate() {
    request({ url: '/wx/public/agreements/current', protected: false }).then(agreements => this.setData({ agreements, loginVisible: true, profileRequired: false }))
      .catch(error => wx.showToast({ title: error.message, icon: 'none' }));
  },
  submitLogin(event) {
    const privacy = this.data.agreements.find(item => item.agreementType === 'PRIVACY');
    const statement = this.data.agreements.find(item => item.agreementType === 'STATEMENT');
    if (!this.data.profileRequired) {
      return request({ url: '/wx/agreements/accept', method: 'POST', data: {
        privacyAccepted: true, privacyVersion: privacy && privacy.version,
        statementAccepted: true, statementVersion: statement && statement.version
      }}).then(() => { this.setData({ loginVisible: false }); this.load(); })
        .catch(error => wx.showToast({ title: error.message, icon: 'none' }));
    }
    wx.login({ success: result => auth.firstLogin({ code: result.code, nickname: event.detail.nickname, avatarPath: event.detail.avatarPath, privacyVersion: privacy && privacy.version, statementVersion: statement && statement.version }).then(() => { this.setData({ loginVisible: false }); this.load(); }).catch(error => wx.showToast({ title: error.message, icon: 'none' })) });
  },
  closeLogin() { this.setData({ loginVisible: false }); },
  load() { vip.profile().then(profile => this.setData({ profile })).catch(error => {
    if (error.code === 40901) return this.openAgreementUpdate();
    wx.showToast({ title: error.message, icon: 'none' });
  }); },
  open(event) { wx.navigateTo({ url: event.currentTarget.dataset.url }); }
});
