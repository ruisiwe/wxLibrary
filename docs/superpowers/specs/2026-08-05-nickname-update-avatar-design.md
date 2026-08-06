# 修改昵称后头像保持有效设计

## 问题与根因

个人资料首次加载通过 `vip.profile()` 获取数据。该方法将后端返回的 `avatarPath` 转换为页面绑定的 `avatarUrl`。

修改昵称通过 `auth.updateNickname()` 获取新的完整资料响应。该方法只格式化了 `vipExpireTime`，没有生成 `avatarUrl`。个人资料页随后使用修改响应整体替换当前 `profile`，导致页面绑定的 `profile.avatarUrl` 变为空，头像不再显示。后端返回的 `avatarPath` 并未丢失。

## 修复方案

在 `auth.updateNickname()` 的响应转换中，根据 `avatarPath` 和现有 `apiBaseUrl()` 生成与 `vip.profile()` 相同的头像访问地址：

```text
{apiBaseUrl}/wx/public/avatar/{avatarPath}
```

当 `avatarPath` 为空时，`avatarUrl` 返回空字符串。页面仍然使用接口返回的完整资料整体更新，不保留旧头像地址，也不修改后端接口。

本次采用单点最小修复，不抽取公共资料格式化模块。原因是当前 `vip.js` 存在其他未提交改动，而现有服务本身已经分别格式化资料响应；修改 `auth.js` 能直接修复根因并降低冲突范围。

## 测试

先增加小程序服务层失败测试，真实加载 `auth.js` 并模拟 `/wx/profile` 返回包含 `avatarPath` 的资料，断言 `updateNickname()` 返回完整的 `avatarUrl`，同时保留现有日期格式化行为。

随后运行昵称编辑和日期格式化相关 Node 测试，确认：

- 修改昵称后的页面资料仍有可用头像地址；
- 昵称、会员日期和本地会话更新行为不变；
- 不修改或重新上传头像；
- 不改变后端昵称校验及唯一性逻辑。

## 非目标

- 不修改头像上传流程；
- 不修改后端资料响应；
- 不重构 `vip.profile()`；
- 不执行发布、部署、SQL 或 Git 集成操作。
