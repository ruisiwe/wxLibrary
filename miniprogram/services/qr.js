const session = require('../store/session');
const { request, apiBaseUrl } = require('./request');

const list = () => request({ url: '/wx/qr-configs' });
const detail = id => request({ url: `/wx/qr-configs/${id}` });

function downloadImage(id) {
  return new Promise((resolve, reject) => {
    const baseUrl = apiBaseUrl();
    if (!baseUrl) {
      reject(new Error('小程序服务地址尚未配置'));
      return;
    }
    wx.downloadFile({
      url: `${baseUrl}/wx/qr-configs/${id}/image`,
      header: { 'Wx-Token': session.getToken() },
      success(result) {
        if (result.statusCode === 200) {
          resolve(result.tempFilePath);
          return;
        }
        reject(new Error('二维码图片加载失败，请重试'));
      },
      fail: () => reject(new Error('二维码图片加载失败，请重试'))
    });
  });
}

module.exports = { list, detail, downloadImage };
