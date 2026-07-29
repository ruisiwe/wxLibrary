const auth = require('../../services/auth');
const session = require('../../store/session');
const { request } = require('../../services/request');
const vip = require('../../services/vip');

Page({
  data: {
    profile: null,
    loginVisible: false,
    profileRequired: true,
    agreements: [],
    authChecking: false,
    nicknameSaving: false
  },
  onShow() {
    if (this.getTabBar) this.getTabBar().setData({ value: '/pages/profile/profile' });
    if (session.getToken()) return this.load();
    this.trySilentLogin();
  },
  login() {
    this.trySilentLogin();
  },
  trySilentLogin() {
    this.setData({ authChecking: true, loginVisible: false, profileRequired: true });
    auth.silentLogin().then(state => {
      if (state.user && state.user.agreementRequired) return this.openAgreementUpdate();
      this.setData({ authChecking: false, loginVisible: false });
      return this.load();
    }).catch(error => {
      if (error.firstLoginRequired) return this.openFirstLogin();
      this.setData({ authChecking: false, loginVisible: false });
      wx.showToast({ title: error.message || '登录失败，请稍后重试', icon: 'none' });
    });
  },
  openFirstLogin() {
    request({ url: '/wx/public/agreements/current', protected: false })
      .then(agreements => this.setData({
        agreements, loginVisible: true, profileRequired: true, authChecking: false
      }))
      .catch(error => {
        this.setData({ authChecking: false });
        wx.showToast({ title: error.message, icon: 'none' });
      });
  },
  openAgreementUpdate() {
    request({ url: '/wx/public/agreements/current', protected: false })
      .then(agreements => this.setData({
        agreements, loginVisible: true, profileRequired: false, authChecking: false
      }))
      .catch(error => {
        this.setData({ authChecking: false });
        wx.showToast({ title: error.message, icon: 'none' });
      });
  },
  submitLogin(event) {
    const privacy = this.data.agreements.find(item => item.agreementType === 'PRIVACY');
    if (!this.data.profileRequired) {
      return request({ url: '/wx/agreements/accept', method: 'POST', data: {
        privacyAccepted: true, privacyVersion: privacy && privacy.version
      }}).then(() => { this.setData({ loginVisible: false }); this.load(); })
        .catch(error => wx.showToast({ title: error.message, icon: 'none' }));
    }
    wx.login({ success: result => auth.firstLogin({
      code: result.code, nickname: event.detail.nickname, avatarPath: event.detail.avatarPath
    }).then(state => {
      if (state.user && state.user.agreementRequired) {
        this.setData({ profileRequired: false, authChecking: false });
        return;
      }
      this.setData({ loginVisible: false, authChecking: false });
      this.load();
    }).catch(error => wx.showToast({ title: error.message, icon: 'none' })) });
  },
  closeLogin(event) {
    const rejected = !this.data.profileRequired && event && event.detail && event.detail.rejected;
    if (!this.data.profileRequired) {
      session.clear();
      if (rejected)
        wx.showToast({ title: '需要同意隐私协议后继续使用', icon: 'none' });
    }
    this.setData({
      profile: null, loginVisible: !rejected, profileRequired: true, authChecking: false
    });
  },
  editNickname() {
    if (this.data.nicknameSaving || !this.data.profile) return;
    wx.showModal({
      title: '修改昵称',
      editable: true,
      content: this.data.profile.nickname || '',
      placeholderText: '请输入昵称',
      success: result => {
        if (result.confirm) this.saveNickname(result.content);
      }
    });
  },
  saveNickname(value) {
    const nickname = (value || '').trim();
    if (!nickname) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' });
      return;
    }
    if (nickname === this.data.profile.nickname || this.data.nicknameSaving) return;

    this.setData({ nicknameSaving: true });
    return auth.updateNickname(nickname).then(profile => {
      const cachedUser = session.getUser() || {};
      session.save(session.getToken(), { ...cachedUser, nickname: profile.nickname });
      this.setData({ profile, nicknameSaving: false });
      wx.showToast({ title: '昵称修改成功', icon: 'success' });
    }).catch(error => {
      this.setData({ nicknameSaving: false });
      wx.showToast({
        title: error.message || '昵称修改失败，请稍后重试',
        icon: 'none'
      });
    });
  },
  load() { vip.profile().then(profile => this.setData({ profile })).catch(error => {
    if (error.code === 40901) return this.openAgreementUpdate();
    wx.showToast({ title: error.message, icon: 'none' });
  }); },
  open(event) { wx.navigateTo({ url: event.currentTarget.dataset.url }); }
});
