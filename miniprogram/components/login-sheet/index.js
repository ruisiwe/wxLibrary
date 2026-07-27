Component({
  properties: {
    visible: Boolean,
    embedded: { type: Boolean, value: false },
    agreements: { type: Array, value: [], observer: 'syncPrivacyAgreement' },
    profileRequired: { type: Boolean, value: true }
  },
  data: {
    avatarPath: '',
    nickname: '',
    privacyContent: ''
  },
  methods: {
    noop() {},
    syncPrivacyAgreement(agreements) {
      const agreement = (agreements || []).find(item => item.agreementType === 'PRIVACY');
      this.setData({ privacyContent: agreement && agreement.content ? agreement.content : '' });
    },
    chooseAvatarAndSubmit(event) {
      const avatarPath = event.detail && event.detail.avatarUrl;
      if (!avatarPath) return wx.showToast({ title: '请选择有效头像', icon: 'none' });
      this.setData({ avatarPath });
      this.submitProfile(avatarPath);
    },
    inputNickname(event) { this.setData({ nickname: event.detail.value }); },
    handleRootTap() {
      if (!this.properties.embedded && this.properties.profileRequired) this.close();
    },
    close() { this.triggerEvent('close'); },
    rejectAgreement() { this.triggerEvent('close', { rejected: true }); },
    submitProfile(avatarPath) {
      const nickname = this.data.nickname || '';
      this.triggerEvent('submit', {
        avatarPath,
        nickname: nickname.trim(),
        agreements: this.properties.agreements
      });
    },
    submit() {
      this.triggerEvent('submit', { agreements: this.properties.agreements });
    }
  }
});
