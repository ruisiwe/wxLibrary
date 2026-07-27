const { request, apiBaseUrl } = require('./request');

function afterRequestPaymentSuccess() {
  return { nextAction: 'QUERY_ORDER', grantVipLocally: false };
}

function paymentDisplayState(status) {
  if (status === 'CANCELLED') return '支付已取消';
  if (status === 'PAID') return '会员已开通';
  return '支付结果确认中';
}

const plans = () => request({ url: '/wx/vip/plans' });
const pageConfig = () => request({ url: '/wx/vip/page-config' });
const createOrder = planId => request({ url: `/wx/vip/orders/${planId}`, method: 'POST' });
const queryOrder = merchantOrderNo => request({ url: `/wx/vip/orders/status/${merchantOrderNo}` });
const redeemCode = code => request({ url: '/wx/vip/code/redeem', method: 'POST', data: { code } });
const profile = () => request({ url: '/wx/profile' }).then(data => ({
  ...data,
  avatarUrl: data.avatarPath ? `${apiBaseUrl()}/wx/public/avatar/${data.avatarPath}` : ''
}));

module.exports = {
  afterRequestPaymentSuccess,
  paymentDisplayState,
  plans,
  pageConfig,
  createOrder,
  queryOrder,
  redeemCode,
  profile
};
