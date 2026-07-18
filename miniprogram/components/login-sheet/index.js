Component({
  properties: {
    visible: Boolean,
    agreements: { type: Array, value: [] },
    profileRequired: { type: Boolean, value: true }
  },
  data: { avatarPath: '', nickname: '', privacyAccepted: false, statementAccepted: false },
  methods: {
    noop() {},
    chooseAvatar(event) { this.setData({ avatarPath: event.detail.avatarUrl }); },
    inputNickname(event) { this.setData({ nickname: event.detail.value }); },
    togglePrivacy() { this.setData({ privacyAccepted: !this.data.privacyAccepted }); },
    toggleStatement() { this.setData({ statementAccepted: !this.data.statementAccepted }); },
    close() { this.triggerEvent('close'); },
    submit() {
      const { avatarPath, nickname, privacyAccepted, statementAccepted } = this.data;
      if (this.properties.profileRequired && (!avatarPath || !nickname.trim()))
        return wx.showToast({ title: '请选择头像并填写昵称', icon: 'none' });
      if (!privacyAccepted || !statementAccepted) return wx.showToast({ title: '请阅读并同意两份协议', icon: 'none' });
      this.triggerEvent('submit', { avatarPath, nickname: nickname.trim(), agreements: this.properties.agreements });
    }
  }
});
