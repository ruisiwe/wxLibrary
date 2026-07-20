# 小程序配置目录迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将微信小程序的 NPM 和微信开发者工具配置全部迁入 `miniprogram`，并保持测试与文档一致。

**Architecture:** `miniprogram` 成为唯一的小程序工程根目录，其中同时保存源码、NPM 配置和微信开发者工具配置。仓库根目录只保留 Java Maven 聚合工程，`ruoyi-ui` 继续保持独立前端工程。

**Tech Stack:** 微信原生小程序、Node.js `node:test`、NPM、微信开发者工具 JSON 配置。

---

### Task 1: 建立目录契约并迁移配置

**Files:**
- Create: `miniprogram/tests/project-layout.test.js`
- Create: `miniprogram/package.json`
- Create: `miniprogram/package-lock.json`
- Create: `miniprogram/project.config.json`
- Delete: `package.json`
- Delete: `package-lock.json`
- Delete: `project.config.json`

- [ ] **Step 1: 写入失败的目录契约测试**

```javascript
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const miniRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(miniRoot, '..');
const configFiles = ['package.json', 'package-lock.json', 'project.config.json'];

test('小程序配置只保存在小程序目录', () => {
  for (const name of configFiles) {
    assert.equal(fs.existsSync(path.join(miniRoot, name)), true, `小程序目录缺少 ${name}`);
    assert.equal(fs.existsSync(path.join(repoRoot, name)), false, `仓库根目录不应保留 ${name}`);
  }
});

test('小程序配置路径以当前目录为根', () => {
  const pkg = require('../package.json');
  const project = require('../project.config.json');
  assert.equal(pkg.scripts.test, 'node --test tests/*.test.js');
  assert.equal(project.miniprogramRoot, './');
  assert.equal(project.compileType, 'miniprogram');
});
```

- [ ] **Step 2: 运行测试并确认因配置尚未迁移而失败**

Run: `node --test miniprogram/tests/project-layout.test.js`

Expected: FAIL，提示 `miniprogram` 缺少 `package.json`，且根目录仍存在配置。

- [ ] **Step 3: 迁移并调整三个配置文件**

`miniprogram/package.json`：

```json
{
  "private": true,
  "engines": { "node": ">=18" },
  "scripts": { "test": "node --test tests/*.test.js" },
  "dependencies": { "tdesign-miniprogram": "1.15.3" }
}
```

`miniprogram/package-lock.json` 保持现有锁定版本、镜像地址与完整性摘要不变。

`miniprogram/project.config.json`：

```json
{
  "appid": "touristappid",
  "projectname": "wechat-library",
  "compileType": "miniprogram",
  "miniprogramRoot": "./",
  "setting": { "es6": true, "nodeModules": true, "minified": true }
}
```

- [ ] **Step 4: 在新工程根目录验证全部小程序测试**

Run: `Set-Location miniprogram; node --test tests/*.test.js`

Expected: 目录契约测试与既有 22 项测试全部 PASS。

### Task 2: 更新运行说明并完成验证

**Files:**
- Modify: `docs/wechat-library-operations.md`
- Modify: `docs/wechat-library-acceptance.md`

- [ ] **Step 1: 更新运行手册**

将微信开发者工具入口明确为仓库的 `miniprogram` 目录；将小程序测试命令改为：

```powershell
Set-Location miniprogram
npm test
Set-Location ..
node ruoyi-ui\scripts\verify-library-routes.js
```

- [ ] **Step 2: 更新验收证据**

将小程序契约测试证据更新为“在 `miniprogram` 目录执行 `npm test`”，并记录配置文件集中存放的目录契约测试。

- [ ] **Step 3: 运行最终检查**

Run:

```powershell
Set-Location miniprogram
npm test
Set-Location ..
node ruoyi-ui\scripts\verify-library-routes.js
git diff --check
git status --short
```

Expected: 小程序测试和菜单契约全部通过；`git diff --check` 无输出；变更只包含计划文件、三个配置迁移、目录契约测试和两份文档。

- [ ] **Step 4: 提交迁移**

```powershell
git add -A
git commit -m "chore: colocate mini program configuration"
```
