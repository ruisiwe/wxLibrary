const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { resolveDocumentAction, canPreview, canSendOriginal } = require('../services/document');

test('未登录用户浏览元数据时不强制登录，预览时才登录', () => {
  assert.equal(resolveDocumentAction({ loggedIn: false, unlocked: false }), 'LOGIN');
  assert.equal(canPreview({ loggedIn: false }), false);
});

test('兑换前后在线阅读都只请求试看文件', () => {
  assert.equal(resolveDocumentAction({ loggedIn: true, unlocked: false }), 'PREVIEW');
  assert.equal(resolveDocumentAction({ loggedIn: true, unlocked: true }), 'PREVIEW');
  const service = fs.readFileSync(path.resolve(__dirname, '../services/document.js'), 'utf8');
  const page = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.wxml'), 'utf8');
  assert.doesNotMatch(service, /\/full/);
  assert.doesNotMatch(page, /打开完整 PDF/);
});

test('原文件分享不指定固定接收人', () => {
  const options = require('../services/document').buildShareOptions('/tmp/source.docx');
  assert.deepEqual(options, { filePath: '/tmp/source.docx' });
});

test('首页只渲染宣传、分类、文档三个内容区块', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/index/index.wxml'), 'utf8');
  assert.match(source, /<promotion-strip/);
  assert.match(source, /<category-grid/);
  assert.match(source, /<document-row/);
  assert.doesNotMatch(source, /课程|会员|积分中心/);
});

test('首页按设计草图展示小程序名称和专题推荐标题', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/index/index.wxml'), 'utf8');
  const style = fs.readFileSync(path.resolve(__dirname, '../pages/index/index.wxss'), 'utf8');
  assert.match(source, /实验室文库/);
  assert.match(source, /专题推荐/);
  assert.match(source, /home__brand/);
  assert.match(source, /home__section-title/);
  assert.match(style, /\.home__brand/);
  assert.match(style, /\.home__section-title/);
  assert.match(style, /\.home__section-bar/);
  assert.match(style,
    /\.home category-grid\s*\{[^}]*margin:\s*0\s+0\s+12rpx[^}]*\}/s,
    '分类宫格与文档列表之间应保留 12rpx 间距');
});

test('原文件分享调用不包含固定接收人字段', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  assert.match(source, /wx\.shareFileMessage\(\{[\s\S]*documents\.buildShareOptions\(filePath\)/);
  assert.doesNotMatch(source, /toUser|openId|openid/);
});

test('原文件发送前使用后台免责声明和可选免提示确认', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  const template = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.wxml'), 'utf8');
  assert.match(source, /documents\.fileDisclaimer\(\)/);
  assert.match(source, /reminderSuppressed/);
  assert.match(template, /以后不再提示/);
  assert.match(template, /scroll-view/);
  assert.doesNotMatch(source, /本账号转载资源均收集于网络/);
});

test('我的页面无论登录与否都提供微信原生客服入口', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/profile/profile.wxml'), 'utf8');
  assert.match(source, /open-type="contact"/);
  assert.match(source, /联系客服/);
  assert.match(source, /class="menu-button" open-type="contact"/);
  assert.doesNotMatch(source, /<button class="contact" open-type="contact">联系客服<\/button>/);
});

test('已登录用户重新进入详情时恢复兑换和收藏状态', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  assert.match(source, /Promise\.all\(\[documents\.unlocked\(\), documents\.favorites\(\), vip\.profile\(\)\]\)/);
  assert.match(source, /const isUnlocked\s*=\s*matches\(unlocked\)/);
  assert.match(source, /const vipActive\s*=\s*Boolean\(profile\.vipActive\)/);
  assert.match(source, /unlocked:\s*isUnlocked,\s*favorite:\s*matches\(favorites\),\s*vipActive/s);
});

test('会员免费文档显示会员免费标识并保留非会员积分兑换入口', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.wxml'), 'utf8');
  const page = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  assert.match(source, /会员免费/);
  assert.match(source, /免费下载/);
  assert.match(source, /积分兑换/);
  assert.match(page, /isVipFreeDocument/);
  assert.match(page, /profile\.vipActive/);
});

test('首页文档标题前显示大写红底白字文件类型标签', () => {
  const home = fs.readFileSync(path.resolve(__dirname, '../pages/index/index.wxml'), 'utf8');
  const component = fs.readFileSync(path.resolve(__dirname, '../components/document-row/index.js'), 'utf8');
  const template = fs.readFileSync(path.resolve(__dirname, '../components/document-row/index.wxml'), 'utf8');
  const style = fs.readFileSync(path.resolve(__dirname, '../components/document-row/index.wxss'), 'utf8');

  assert.match(home, /show-file-type="\{\{true\}\}"/, '首页应单独开启文件类型标签');
  assert.match(component, /showFileType\s*:\s*\{\s*type:\s*Boolean,\s*value:\s*false\s*\}/s,
    '共享组件应默认关闭文件类型标签');
  assert.match(component, /toUpperCase\(\)/, '文件类型应统一转为大写英文');
  assert.match(template,
    /row__file-type[\s\S]*\{\{fileType\}\}[\s\S]*row__title[\s\S]*\{\{document\.title\}\}/,
    '文件类型标签应位于标题前并与标题同行');
  assert.match(style, /\.row__file-type\s*\{[^}]*background:\s*#[a-fA-F0-9]+[^}]*color:\s*#fff[^}]*border-radius:/s,
    '文件类型标签应使用红底白字和圆角');
});

test('已兑换、零积分和有效会员文档可直接进入原文件发送', () => {
  assert.equal(canSendOriginal({ unlocked: true, pointPrice: 20, accessType: 'POINT', vipActive: false }), true);
  assert.equal(canSendOriginal({ unlocked: false, pointPrice: 0, accessType: 'POINT', vipActive: false }), true);
  assert.equal(canSendOriginal({ unlocked: false, pointPrice: 20, accessType: 'VIP_FREE', vipActive: true }), true);
  assert.equal(canSendOriginal({ unlocked: false, pointPrice: 20, accessType: 'VIP_FREE', vipActive: false }), false);
  assert.equal(canSendOriginal({ unlocked: false, pointPrice: 20, accessType: 'POINT', vipActive: true }), false);
});

test('免费但未兑换的文档仅免费解锁后继续发送原文件', () => {
  const service = fs.readFileSync(path.resolve(__dirname, '../services/document.js'), 'utf8');
  const page = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');
  const template = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.wxml'), 'utf8');

  assert.match(service, /freeOnly:\s*options\.freeOnly\s*===\s*true/,
    '文档服务应显式传递仅免费解锁标记');
  assert.match(page, /canSendOriginal:\s*false/, '详情页应保存直接发送状态');
  assert.match(page, /documents\.canSendOriginal\(/, '详情页应统一计算直接发送状态');
  assert.match(page, /documents\.unlock\(this\.data\.id,\s*requestId,\s*\{\s*freeOnly:\s*true\s*\}\)/s,
    '自动解锁必须禁止积分扣减');
  assert.match(page, /shareAvailableOriginal[\s\S]*return this\.shareOriginal\(\)/,
    '自动解锁成功后应继续现有原文件发送流程');
  assert.match(template, /wx:if="\{\{!canSendOriginal\}\}"[^>]*bindtap="unlock"/,
    '不满足免费条件时仍显示积分兑换入口');
  assert.match(template, /wx:if="\{\{canSendOriginal\}\}"[^>]*bindtap="shareAvailableOriginal"/,
    '可免费获取或已兑换时应直接显示发送原文件');
});

test('原文件仅在微信发送成功后上报且上报失败不误报发送失败', () => {
  const service = fs.readFileSync(path.resolve(__dirname, '../services/document.js'), 'utf8');
  const page = fs.readFileSync(path.resolve(__dirname, '../pages/document-detail/document-detail.js'), 'utf8');

  assert.match(service, /recordSend/);
  assert.match(service, /\/wx\/documents\/\$\{id\}\/send-record/);
  assert.match(page, /success:[\s\S]*documents\.recordSend/);
  assert.match(page, /fail:\s*reject/);
  assert.match(page, /文档发送统计上报失败/);
  assert.match(page, /\.catch\(error\s*=>\s*console\.warn\('文档发送统计上报失败'/);
});
