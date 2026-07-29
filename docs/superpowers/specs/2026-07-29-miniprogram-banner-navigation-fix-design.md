# 小程序首页轮播图跳转修复设计

## 目标

修复小程序首页轮播图点击后无响应的问题。点击有效轮播图时，跳转到该轮播图关联的文档详情页：

`/pages/document-detail/document-detail?id=<documentId>`

## 根因

首页接口返回的轮播图对象包含 `documentId`，不包含 `linkPath`。`promotion-strip` 组件当前只在 `item.linkPath` 存在时调用 `wx.navigateTo`，导致正常接口数据无法触发跳转。

## 方案

由小程序 `promotion-strip` 组件读取轮播图的 `documentId`，构造文档详情页路由。页面路由属于小程序展示层职责，不在后端 DTO 中增加小程序专用的 `linkPath`。

组件点击规则：

- `documentId` 为有效正整数时，调用 `wx.navigateTo` 跳转文档详情页。
- `documentId` 缺失、为零、负数或非数字时，不发起跳转。
- 跳转参数只包含经过数值校验的文档编号，避免把任意字符串拼接到页面地址。

## 改动范围

- 修改 `miniprogram/components/promotion-strip/index.js` 的点击处理。
- 新增小程序契约测试，验证有效文档编号能够跳转，无效编号不会跳转。
- 不修改首页 WXML/WXSS，不覆盖当前首页布局改动。
- 不修改首页接口、后端 DTO、数据库或后台轮播图配置。

## 验证

- 模拟点击包含 `documentId: 12` 的轮播图，断言调用：
  `wx.navigateTo({ url: '/pages/document-detail/document-detail?id=12' })`
- 验证缺少或非法 `documentId` 时不调用 `wx.navigateTo`。
- 运行首页、文档访问和导航相关小程序测试。
- 检查当前工作区中首页既有未提交改动保持不变。
