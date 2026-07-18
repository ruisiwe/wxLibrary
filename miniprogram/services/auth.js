const { request, apiBaseUrl, unwrapResponse } = require('./request');
const session = require('../store/session');

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

function firstLogin({ code, nickname, avatarPath, privacyVersion, statementVersion }) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${apiBaseUrl()}/wx/auth/login`,
      filePath: avatarPath,
      name: 'avatar',
      formData: {
        code, nickname, privacyAccepted: 'true', privacyVersion,
        statementAccepted: 'true', statementVersion
      },
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
  return request({ url: '/wx/profile', method: 'PUT', data: { nickname } });
}

module.exports = { loginWithCode, firstLogin, updateNickname };
