const { request, apiBaseUrl, unwrapResponse } = require('./request');
const session = require('../store/session');
const { formatDate } = require('../utils/date');

const FIRST_LOGIN_AVATAR_MESSAGE = '首次登录必须上传有效头像';

function saveLogin(result) {
  const user = {
    id: result.userId,
    nickname: result.nickname,
    avatarPath: result.avatarPath,
    pointBalance: result.pointBalance,
    agreementRequired: result.agreementRequired
  };
  return session.save(result.token, user);
}

function loginWithCode(code) {
  return request({ url: '/wx/auth/login', method: 'POST', data: { code }, protected: false })
    .then(saveLogin);
}

function loginCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(result) {
        if (result && result.code) return resolve(result.code);
        reject(new Error('微信登录失败，请重试'));
      },
      fail: () => reject(new Error('微信登录失败，请重试'))
    });
  });
}

function isFirstLoginRequired(error) {
  return !!(error && error.message && error.message.indexOf(FIRST_LOGIN_AVATAR_MESSAGE) !== -1);
}

function markFirstLoginRequired(error) {
  if (isFirstLoginRequired(error)) error.firstLoginRequired = true;
  return error;
}

function silentLogin() {
  return loginCode().then(code => loginWithCode(code)).catch(error => {
    throw markFirstLoginRequired(error);
  });
}

function firstLogin({ code, nickname, avatarPath }) {
  return new Promise((resolve, reject) => {
    const formData = { code };
    const nicknameValue = nickname && nickname.trim();
    if (nicknameValue) formData.nickname = nicknameValue;
    wx.uploadFile({
      url: `${apiBaseUrl()}/wx/auth/login`,
      filePath: avatarPath,
      name: 'avatar',
      formData,
      success(response) {
        try {
          const result = unwrapResponse(JSON.parse(response.data));
          resolve(saveLogin(result));
        } catch (error) { reject(error); }
      },
      fail: () => reject(new Error('头像上传失败，请稍后重试'))
    });
  });
}

function updateNickname(nickname) {
  return request({ url: '/wx/profile', method: 'PUT', data: { nickname } })
    .then(profile => ({
      ...profile,
      vipExpireTime: formatDate(profile.vipExpireTime)
    }));
}

module.exports = { loginWithCode, silentLogin, firstLogin, updateNickname, isFirstLoginRequired };
