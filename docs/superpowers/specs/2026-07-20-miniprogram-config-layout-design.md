# 小程序配置目录迁移设计

## 目标

将微信小程序相关配置集中到 `miniprogram` 目录，使微信开发者工具、NPM 依赖和小程序测试都以该目录为唯一入口，避免与 Java 后端及 `ruoyi-ui` 管理端配置混淆。

## 方案

采用单一小程序工程目录方案：

- 将根目录的 `package.json`、`package-lock.json`、`project.config.json` 移入 `miniprogram`。
- 将 `project.config.json` 的 `miniprogramRoot` 从 `miniprogram/` 改为 `./`。
- 将 `package.json` 的测试路径从 `miniprogram/tests/*.test.js` 改为 `tests/*.test.js`。
- 微信开发者工具直接导入 `C:\Users\Administrator\Desktop\wxLibrary\miniprogram`。
- 后端继续由仓库根目录 `pom.xml` 管理，若依管理端继续由 `ruoyi-ui/package.json` 管理。

不保留根目录兼容副本，避免两套配置漂移。

## 验证

- 新增布局契约测试，确认三个配置文件只存在于 `miniprogram`。
- 校验 `miniprogramRoot` 为 `./`，测试脚本使用 `tests/*.test.js`。
- 在 `miniprogram` 目录执行全部 Node 测试。
- 检查微信开发者工具配置 JSON 可解析，Git 工作区不包含生成的依赖或构建产物。

## 文档影响

运行手册中的小程序测试和微信开发者工具导入路径同步更新为 `miniprogram` 目录。
