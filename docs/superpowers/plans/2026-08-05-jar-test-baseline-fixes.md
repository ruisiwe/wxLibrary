# JAR Test Baseline Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复阻断 Maven JAR 打包的三个过期或环境敏感测试，并同步同一轮播图契约的前端测试。

**Architecture:** 不修改现有业务实现，只维护测试基线。分类图标和轮播图断言以当前资源、后端常量及管理端实现为准；协议时间测试显式复用应用的系统默认时区策略，消除 Jackson 原始默认 UTC 与本机时区不一致造成的偏移。

**Tech Stack:** Java 8、JUnit 5、Jackson、Node.js 测试、Maven Surefire

---

### Task 1: 同步 Java 测试基线

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/category/CategoryIconCatalogTest.java:19`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/domain/WlAgreementJsonTest.java:5-18`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java:82`

- [x] **Step 1: 确认现有失败测试**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am '-Dtest=CategoryIconCatalogTest,WlAgreementJsonTest,BannerImageProcessorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: `CategoryIconCatalogTest` 报告 `24 != 31`，`WlAgreementJsonTest` 报告八小时时差，`BannerImageProcessorTest` 报告 `1120×550 != 952×550`。

- [x] **Step 2: 更新图标数量断言**

将断言更新为当前精选目录数量：

```java
assertEquals(31, options.size());
```

- [x] **Step 3: 让协议时间测试复用应用时区策略**

增加 `TimeZone` 导入，并在反序列化前设置 `ObjectMapper`：

```java
import java.util.TimeZone;

ObjectMapper objectMapper = new ObjectMapper();
objectMapper.setTimeZone(TimeZone.getDefault());
WlAgreement agreement = objectMapper.readValue(
        "{\"effectiveTime\":\"2026-07-23 14:45:48\"}", WlAgreement.class);
```

- [x] **Step 4: 更新后端轮播图错误文案断言**

```java
assertEquals("轮播图尺寸必须为952×550", exception.getMessage());
```

- [x] **Step 5: 运行聚焦 Java 测试**

Run: Task 1 Step 1 的 Maven 命令。

Expected: 相关测试 `Failures: 0, Errors: 0`。

### Task 2: 同步前端轮播图契约测试

**Files:**
- Modify: `ruoyi-ui/tests/banner-image-crop-upload.test.js:21-22`

- [x] **Step 1: 更新当前裁剪比例和画布宽度断言**

```javascript
assert(/fixed-number=["']\[476,\s*275\]["']/.test(cropper), '裁剪框比例应固定为476:275')
assert(/canvas\.width\s*=\s*952/.test(cropper), '输出画布宽度应为952像素')
```

- [x] **Step 2: 运行前端轮播图测试**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test ruoyi-ui/tests/banner-image-crop-upload.test.js
```

Expected: 测试通过，无失败。

### Task 3: 验证 JAR 打包

**Files:**
- Verify only: Maven reactor and working-tree diff

- [x] **Step 1: 运行 Maven package**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' package
```

Expected: Reactor 全部模块 `SUCCESS`，最终输出 `BUILD SUCCESS`。

- [x] **Step 2: 检查差异**

Run:

```powershell
git diff --check
git diff -- ruoyi-wechat-library/src/test/java/com/ruoyi/library/category/CategoryIconCatalogTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/domain/WlAgreementJsonTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java ruoyi-ui/tests/banner-image-crop-upload.test.js docs/superpowers/plans/2026-08-05-jar-test-baseline-fixes.md
```

Expected: `git diff --check` 无输出；差异只包含计划中的测试维护和实施计划。不提交 Git。
