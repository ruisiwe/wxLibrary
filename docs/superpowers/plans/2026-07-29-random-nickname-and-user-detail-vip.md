# 随机昵称与微信用户详情会员操作实施计划

> **For Codex:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task.

**Goal:** 新微信用户首次登录时由后端生成 10 位大小写英文字母昵称；美化后台微信用户详情弹窗，展示头像、格式化会员与登录信息，并支持直接开通或续期会员。

**Architecture:** 登录服务仅在创建新用户时生成随机昵称，已有用户登录及主动修改昵称逻辑保持不变。后台详情页面复用现有头像公共访问接口、会员套餐查询接口和人工开通接口，不新增数据库表或后端接口。

**Tech Stack:** Java 8、Spring Boot、JUnit 5、Mockito、Vue 2、Element UI、Node.js、Vue Template Compiler。

---

## Task 1：用测试锁定新用户随机昵称规则

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java`

### Step 1：修改首次登录测试

将首次登录成功测试中的固定昵称断言改为：

```java
assertTrue(inserted.getNickname().matches("[A-Za-z]{10}"));
assertFalse("测试用户".equals(inserted.getNickname()));
```

将“缺少昵称使用默认昵称”测试改名为“缺少昵称生成随机昵称”，并断言昵称符合 `[A-Za-z]{10}`。

### Step 2：验证首次登录忽略客户端昵称

把原“首次登录要求昵称合规”测试改成成功场景，提交包含 HTML 的昵称后仍完成注册，并断言保存的昵称是 10 位字母且不等于提交值。

### Step 3：保留已有用户昵称校验覆盖

把 Unicode 控制字符和格式字符拒绝测试调整为已有用户资料更新场景，确保已有用户主动修改昵称时仍调用原校验逻辑并抛出中文错误。

### Step 4：运行测试并确认先失败

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  '-Dtest=WxLoginServiceTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

预期：当前实现仍保存客户端昵称或“微信用户”，随机昵称断言失败。

## Task 2：实现后端随机昵称

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java`

### Step 1：增加随机昵称常量

```java
private static final char[] RANDOM_NICKNAME_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
private static final int RANDOM_NICKNAME_LENGTH = 10;
private static final SecureRandom RANDOM = new SecureRandom();
```

### Step 2：实现生成方法

```java
private String generateRandomNickname()
{
    char[] value = new char[RANDOM_NICKNAME_LENGTH];
    for (int index = 0; index < value.length; index++)
    {
        value[index] = RANDOM_NICKNAME_ALPHABET[RANDOM.nextInt(RANDOM_NICKNAME_ALPHABET.length)];
    }
    return new String(value);
}
```

### Step 3：仅替换新用户创建逻辑

在 `createFirstUser` 中直接调用 `generateRandomNickname()`，删除不再使用的 `DEFAULT_NICKNAME` 和 `resolveLoginNickname`。不要修改 `updateExistingProfile`，以保留已有用户昵称更新功能。

### Step 4：运行后端定向测试

重复 Task 1 的 Maven 命令，预期 `WxLoginServiceTest` 全部通过。

### Step 5：提交后端改动

仅暂存后端服务和测试文件：

```powershell
git add -- ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java
git commit -m "feat: generate random nickname for new users"
```

## Task 3：用前端契约测试锁定详情弹窗与会员操作

**Files:**
- Create: `ruoyi-ui/tests/wx-user-detail-vip-operation.test.js`
- Modify: `ruoyi-ui/src/views/library/user/index.vue`

### Step 1：新增页面契约测试

测试读取并编译 `src/views/library/user/index.vue`，至少断言：

```javascript
assert.ok(source.includes('<el-avatar'))
assert.ok(source.includes('avatarUrl(detail)'))
assert.ok(!source.includes('头像路径'))
assert.ok(source.includes('detailVipState'))
assert.ok(source.includes('已过期'))
assert.ok(source.includes('开通会员'))
assert.ok(source.includes('续期会员'))
assert.ok(source.includes('formatDateTime'))
assert.ok(source.includes("parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}')"))
assert.ok(source.includes('listVipPlans'))
assert.ok(source.includes('openVip'))
assert.ok(source.includes('userIds: [this.detail.id]'))
assert.ok(source.includes('batchNo: this.createBatchNo()'))
assert.ok(source.includes(':loading="vipSubmitting"'))
```

并使用 `vue-template-compiler` 编译模板，断言没有编译错误。

### Step 2：运行前端测试并确认先失败

```powershell
node ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
```

预期：当前详情弹窗没有头像和会员操作，契约测试失败。

## Task 4：重构详情弹窗并接入会员开通

**Files:**
- Modify: `ruoyi-ui/src/views/library/user/index.vue`

### Step 1：重构详情展示

在详情弹窗顶部展示头像、昵称、用户编号和账号状态。使用现有公共头像地址规则：

```javascript
avatarUrl(user) {
  if (!user || !user.avatarPath) return ''
  const path = String(user.avatarPath)
    .split('/')
    .map(part => encodeURIComponent(part))
    .join('/')
  return `${process.env.VUE_APP_BASE_API}/wx/public/avatar/${path}`
}
```

详情字段保留 OpenID、积分、会员状态、会员到期时间、最后登录时间，不再显示内部头像存储路径。

### Step 2：增加时间和会员状态格式化

```javascript
formatDateTime(value) {
  return value ? this.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}
```

根据 `vipExpireTime` 与当前时间计算 `未开通`、`VIP会员`、`已过期` 三种状态；到期时间为空时显示“未开通”。

### Step 3：增加会员操作弹窗

从 `@/api/library/vip` 导入：

```javascript
import { listVipPlans, openVip } from '@/api/library/vip'
```

点击“开通会员”或“续期会员”时：

1. 以当前详情用户构造 `userIds: [this.detail.id]`。
2. 生成并保存 `batchNo: this.createBatchNo()`，不要求管理员填写业务编码。
3. 查询启用的会员套餐。
4. 管理员选择套餐并填写最多 500 字的操作原因。
5. 调用现有 `openVip` 接口。
6. 成功后重新调用 `getUser` 刷新详情，并刷新列表。

按钮仅对启用用户显示，并复用权限 `library:vip:operation`。

### Step 4：补充页面样式

为用户摘要、头像、昵称、会员状态、套餐摘要增加 scoped 样式，保持当前 RuoYi 页面间距、边框色和 Element UI 视觉风格。

### Step 5：运行前端契约测试

```powershell
node ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
```

预期：测试通过且模板无编译错误。

### Step 6：运行生产构建

```powershell
npm run build:prod
```

工作目录：`ruoyi-ui`。构建产物不暂存、不提交。

### Step 7：提交前端改动

```powershell
git add -- ruoyi-ui/src/views/library/user/index.vue ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
git commit -m "feat: enhance WeChat user detail and VIP action"
```

## Task 5：最终验证和范围检查

**Files:**
- Verify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java`
- Verify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java`
- Verify: `ruoyi-ui/src/views/library/user/index.vue`
- Verify: `ruoyi-ui/tests/wx-user-detail-vip-operation.test.js`

### Step 1：重新运行后端定向测试

运行 Task 1 的 Maven 命令，确认测试通过。

### Step 2：重新运行前端契约测试

```powershell
node ruoyi-ui/tests/wx-user-detail-vip-operation.test.js
```

### Step 3：检查差异质量

```powershell
git diff --check HEAD
git status --short
git diff --stat HEAD
```

确认没有读取或修改敏感配置，没有提交 `ruoyi-ui/dist`，没有覆盖用户原有的无关改动。

