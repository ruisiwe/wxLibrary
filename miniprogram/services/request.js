const session = require('../store/session');

function buildHeaders({ wxToken } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (wxToken) headers['Wx-Token'] = wxToken;
  return headers;
}

function unwrapResponse(body) {
  if (body && body.code === 0) return body.data;
  const error = new Error((body && body.message) || '请求失败，请稍后重试');
  error.code = body && body.code;
  throw error;
}

function apiBaseUrl() {
  if (typeof getApp !== 'function') return '';
  const app = getApp();
  return (app.globalData && app.globalData.apiBaseUrl) || '';
}

function request(options) {
  const token = options.protected === false ? '' : session.getToken();
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${apiBaseUrl()}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: buildHeaders({ wxToken: token }),
      success(response) {
        try {
          resolve(unwrapResponse(response.data));
        } catch (error) {
          if (error.code === 401) session.clear();
          reject(error);
        }
      },
      fail: () => reject(new Error('网络异常，请检查网络后重试'))
    });
  });
}

module.exports = { buildHeaders, unwrapResponse, request, apiBaseUrl };
