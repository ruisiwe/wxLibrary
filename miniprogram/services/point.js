const { request } = require('./request');

const balance = () => request({ url: '/wx/points/balance' });
const records = params => request({ url: '/wx/points/records', data: params });
const rules = () => request({ url: '/wx/points/rules' });
const signIn = () => request({ url: '/wx/points/signin', method: 'POST' });
const rewardAd = adBizNo => request({ url: '/wx/points/ad-reward', method: 'POST', data: { adBizNo } });
const share = () => request({ url: '/wx/points/share', method: 'POST' });

module.exports = { balance, records, rules, signIn, rewardAd, share };
