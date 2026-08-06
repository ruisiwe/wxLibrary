# 首页分类区与文档列表间隙调整设计

## 目标

缩小首页分类宫格与首条文档之间的灰色分隔带，使内容衔接更紧凑，同时保留两个区域的视觉层次。

## 现状

首页通过 `miniprogram/pages/index/index.wxss` 中的以下规则控制该间隙：

```css
.home category-grid {
  display: block;
  margin: 0 0 28rpx
}
```

截图中红箭头所指位置对应 `category-grid` 的 `28rpx` 底部外边距。分类组件内部没有额外底部留白，文档列表也没有顶部外边距。

## 调整方案

只将 `category-grid` 的底部外边距由 `28rpx` 改为 `12rpx`：

```css
.home category-grid {
  display: block;
  margin: 0 0 12rpx
}
```

不修改分类宫格高度、分类项内边距、文档列表内边距、文档行高度或页面其他区块间距。

## 验证

增加或更新首页样式契约测试，断言 `category-grid` 使用 `12rpx` 底部外边距，并运行首页导航、文档访问相关小程序测试。最后执行 `git diff --check`。

## 非目标

- 不修改首页专题推荐与分类宫格之间的间距；
- 不修改文档缩略图、标题或积分信息布局；
- 不修改后端接口和数据；
- 不执行发布、部署、暂存或提交。
