# 微信用户昵称校验与唯一性 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将微信用户昵称限制为最长 20 个白名单字符，并确保首次随机昵称和主动修改昵称在全部微信用户记录中唯一。

**Architecture:** `WxLoginService` 统一承担格式校验、程序查重和首次昵称碰撞重试；`WlWxUserMapper` 提供排除当前用户的昵称计数查询；数据库唯一索引作为并发兜底。初始化 SQL 与独立人工迁移 SQL 同步约束，但不执行迁移。

**Tech Stack:** Java 8、Spring、MyBatis、JUnit 5、Mockito、MySQL、IntelliJ 内置 Maven

---

### Task 1：用测试锁定昵称格式与程序查重

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/WxProfileServiceTest.java`

- [ ] **Step 1：增加昵称白名单与保留名称失败测试**

在 `WxLoginServiceTest` 中直接调用真实校验器，断言合法昵称 `中文Abc_12-` 通过；断言空值、空白、21 个字符、空格、标点、表情符号、控制字符以及忽略大小写的 `null`、`undefined` 分别返回设计中的简体中文错误。

- [ ] **Step 2：增加主动修改昵称查重测试**

让 `userMapper.countByNickname("重复昵称", 9L)` 返回 `1`，通过已有用户登录携带昵称的路径断言返回：

```java
assertEquals("昵称已被使用，请更换后重试", assertThrows(ServiceException.class,
        () -> loginService.login(request, null, null)).getMessage());
verify(userMapper, never()).updateProfile(any(WlWxUser.class));
```

在 `WxProfileServiceTest` 中把既有 mock 调用改为 `validateUniqueNickname("新昵称", 3L)`，并增加数据库并发唯一键冲突转换为同一中文错误的测试。

- [ ] **Step 3：运行测试确认 RED**

Run：

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am '-Dtest=WxLoginServiceTest,WxProfileServiceTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected：因 `validateUniqueNickname`、`countByNickname` 及新校验行为尚不存在而编译或断言失败。

### Task 2：实现格式校验、程序查重与并发错误转换

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/WxProfileService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml`

- [ ] **Step 1：增加 Mapper 昵称计数契约**

在接口中增加：

```java
int countByNickname(@Param("nickname") String nickname,
        @Param("excludedUserId") Long excludedUserId);
```

在 XML 中增加不限定 `del_flag` 的查询，确保逻辑删除记录仍占用昵称：

```xml
<select id="countByNickname" resultType="int">
    select count(1) from wl_wx_user where nickname = #{nickname}
    <if test="excludedUserId != null">and id != #{excludedUserId}</if>
</select>
```

- [ ] **Step 2：实现白名单与唯一性方法**

在 `WxLoginService` 中增加最大长度 20 和白名单正则 `^[\\p{IsHan}A-Za-z0-9_-]+$`。`validateNickname` 使用 `codePointCount` 计算 Unicode 字符数，拒绝完整值为 `null` 或 `undefined` 的昵称；增加：

```java
public String validateUniqueNickname(String nickname, Long currentUserId)
{
    String value = validateNickname(nickname, true);
    if (userMapper.countByNickname(value, currentUserId) > 0)
        throw new ServiceException("昵称已被使用，请更换后重试");
    return value;
}
```

- [ ] **Step 3：让两个修改入口复用唯一性校验**

已有用户登录携带昵称和 `WxProfileService.update` 都调用 `validateUniqueNickname`。在两个数据库更新边界捕获 `DuplicateKeyException` 并转换为：

```java
throw new ServiceException("昵称已被使用，请更换后重试");
```

保留头像失败清理逻辑；`nickname == null && avatar == null` 返回“昵称不能为空”。

- [ ] **Step 4：运行聚焦测试确认 GREEN**

重复 Task 1 的 Maven 命令。

Expected：`WxLoginServiceTest`、`WxProfileServiceTest` 全部 PASS。

### Task 3：用测试锁定首次随机昵称的查重与碰撞重试

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java`

- [ ] **Step 1：增加随机昵称程序查重测试**

让 `countByNickname(any(String.class), isNull())` 依次返回 `1`、`0`，断言插入成功且计数查询调用两次。

- [ ] **Step 2：增加插入并发碰撞重试测试**

让第一次 `insertWxUser` 抛出昵称唯一键 `DuplicateKeyException`，同时 `selectByOpenidForUpdate` 返回空；第二次插入设置用户编号并成功。断言插入两次、头像只存储一次、最终登录成功。

- [ ] **Step 3：运行测试确认 RED**

运行 `WxLoginServiceTest`，预期新测试因当前实现不查重、不重试而失败。

- [ ] **Step 4：实现有上限的随机昵称重试**

在 `createFirstUser` 中最多尝试 10 次：生成昵称、调用 `countByNickname(nickname, null)`、尝试插入。捕获唯一键异常后优先按 `openid` 恢复并发登录；不存在相同 `openid` 时视为昵称碰撞并继续。耗尽后清理新头像并抛出“系统生成昵称失败，请稍后重试”。

- [ ] **Step 5：运行测试确认 GREEN**

运行 `WxLoginServiceTest`，预期全部 PASS。

### Task 4：用测试锁定 Mapper 和初始化 SQL 契约

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/sql/WechatLibrarySchemaTest.java`
- Modify: `sql/wechat_library.sql`
- Create: `docs/2026-08-05-wx-nickname-validation-uniqueness.sql`

- [ ] **Step 1：增加 Mapper XML 失败测试**

断言 `countByNickname` 映射存在，并检查有排除用户编号参数时生成的 SQL 包含 `nickname = ?`、`id != ?`，且不包含 `del_flag`。

- [ ] **Step 2：增加初始化 SQL 失败测试**

断言 `wl_wx_user.nickname` 为 `varchar(20)`，并存在：

```sql
UNIQUE KEY `uk_wx_user_nickname` (`nickname`)
```

- [ ] **Step 3：运行测试确认 RED**

运行 `LibraryMapperXmlContractTest,WechatLibrarySchemaTest`，预期初始化 SQL 断言失败；若 Mapper 已在 Task 2 实现，则 Mapper 断言应通过。

- [ ] **Step 4：更新初始化 SQL 并新增独立迁移 SQL**

把初始化表中的昵称列改为 `varchar(20)` 并增加唯一索引。新增的 `docs` SQL 先提供 `CHAR_LENGTH(nickname) > 20`、空值/保留名称和 `GROUP BY nickname HAVING COUNT(*) > 1` 预检，再执行 `MODIFY COLUMN` 与 `ADD UNIQUE KEY`；文件明确要求人工审核，不自动修复历史数据。

- [ ] **Step 5：运行测试确认 GREEN**

运行 `LibraryMapperXmlContractTest,WechatLibrarySchemaTest`，预期全部 PASS。

### Task 5：相关回归验证和差异复核

**Files:**
- Verify all files listed above

- [ ] **Step 1：运行模块测试**

Run：

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected：相关 reactor 模块全部 SUCCESS。

- [ ] **Step 2：检查差异质量和范围**

Run：

```powershell
git diff --check -- ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/WxProfileService.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/WxProfileServiceTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/sql/WechatLibrarySchemaTest.java sql/wechat_library.sql docs/2026-08-05-wx-nickname-validation-uniqueness.sql
```

Expected：无空白错误；差异只包含昵称功能及原先已存在的同文件用户改动。

- [ ] **Step 3：交付**

不执行 SQL、不部署、不创建分支、不暂存或提交文件。说明测试结果、SQL 人工执行前置条件和现有工作区重叠改动。
