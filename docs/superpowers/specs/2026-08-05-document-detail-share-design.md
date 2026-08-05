# 文档详情页右上角分享设计

## 1. 目标

在微信小程序文档详情页开启右上角原生分享入口，同时支持转发给微信好友和分享到朋友圈。接收方打开后仍进入同一篇文档的详情页，不增加后端接口、不改变文档访问权限和登录流程。

## 2. 当前上下文

- 文档详情页为 `pages/document-detail/document-detail`。
- 页面已经通过 `options.id` 加载文档详情，列表入口使用 `?id=<documentId>`。
- VIP 页面已经使用 `wx.showShareMenu` 和 `onShareAppMessage`，可沿用小程序现有代码风格。
- 文档详情页当前没有 `onShareAppMessage`、`onShareTimeline` 或分享菜单配置。

## 3. 方案

在 `miniprogram/pages/document-detail/document-detail.js` 增加三部分行为：

1. 页面加载时调用 `wx.showShareMenu({ menus: ['shareAppMessage', 'shareTimeline'] })`，显示好友和朋友圈两个右上角入口。
2. `onShareAppMessage` 返回当前文档标题、封面和 `/pages/document-detail/document-detail?id=<id>` 路径。
3. `onShareTimeline` 返回当前文档标题、封面和 `id=<id>` 查询参数。

分享回调使用当前页面已经加载的 `data.document` 和 `data.id`，不新增网络请求。文档尚未加载或详情接口失败时，使用固定中文兜底标题；路径仍使用当前页面和已有 ID，避免分享回调抛出异常。

## 4. 数据与权限边界

- 分享只携带文档 ID，不携带用户 token、openid、解锁状态或原始文件地址。
- 接收方进入详情页后仍由现有公开详情接口加载元数据；预览、兑换、收藏和原文件发送继续走现有登录与授权流程。
- 不记录分享调用作为文档发送或兑换成功事件。

## 5. 测试与验收

新增小程序静态契约测试，验证：

- 页面配置好友和朋友圈分享菜单；
- 好友分享返回详情页 `id` 路径；
- 朋友圈分享返回详情页 `id` 查询参数；
- 分享标题使用文档标题并存在兜底值；
- 分享内容不包含 token、openid 或原始文件 URL。

使用项目 bundled Node 运行对应测试，并执行 `git diff --check`。验收时在微信开发者工具和真机分别打开一篇文档，从右上角验证两个分享入口；接收方应能打开同一文档详情页，未登录用户仍只能使用原有公开和登录后功能边界。
