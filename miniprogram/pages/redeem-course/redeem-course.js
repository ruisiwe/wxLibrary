const vip = require('../../services/vip');
const { formatDate } = require('../../utils/date');
Page({
  data: { code: '' },
  input(event) {
    this.setData({ code: event.detail.value.toUpperCase().replace(/\s/g, '') });
  },
  submit() {
    if (!this.data.code) return wx.showToast({ title: '请输入会员码', icon: 'none' });
    vip.redeemCode(this.data.code).then(result => {
      const expire = formatDate(result.newExpireTime || result.endTime);
      wx.showModal({
        title: '兑换成功',
        content: expire ? `会员已开通或续期，有效期至 ${expire}` : '会员已开通或续期。',
        showCancel: false,
        success: () => wx.switchTab({ url: '/pages/vip/vip' })
      });
    }).catch(error => wx.showToast({ title: error.message, icon: 'none' }));
  }
});
