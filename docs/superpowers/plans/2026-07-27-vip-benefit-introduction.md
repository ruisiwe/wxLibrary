# VIP 权益介绍 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 VIP 套餐页展示后台可维护的全局权益文字和客服微信图片，并新增“VIP 权益介绍”后台维护页面。

**Architecture:** 使用 `wl_vip_benefit` 保存可排序、可启停的权益文字，使用固定主键的 `wl_vip_page_config` 保存客服提示语和私有 COS 图片键。后台通过独立 CRUD 与 multipart 配置接口维护，小程序通过独立 `/wx/vip/page-config` 接口读取，现有套餐与支付接口不变。

**Tech Stack:** Java 8、Spring Boot、MyBatis XML、JUnit 5、Mockito、腾讯云 COS、Vue 2 + Element UI、微信小程序、Node.js `node:test`、MySQL SQL。

---

## 文件结构

- `ruoyi-wechat-library/.../domain/WlVipBenefit.java`：权益管理领域对象。
- `ruoyi-wechat-library/.../domain/WlVipPageConfig.java`：客服配置领域对象。
- `ruoyi-wechat-library/.../dto/VipPageConfigView.java`：小程序及后台读取视图。
- `ruoyi-wechat-library/.../mapper/WlVipBenefitMapper.java` 和 XML：权益 CRUD 与启用列表。
- `ruoyi-wechat-library/.../mapper/WlVipPageConfigMapper.java` 和 XML：单例配置查询和带旧图片键条件的更新。
- `ruoyi-wechat-library/.../storage/VipCustomerServiceImageProcessor.java`：2 MB、JPEG/PNG/WebP 图片校验和临时文件生命周期。
- `ruoyi-wechat-library/.../service/VipBenefitService.java`：权益校验及 CRUD。
- `ruoyi-wechat-library/.../service/VipPageConfigService.java`：客服配置、COS 上传补偿、短时 URL 和小程序页面视图。
- `ruoyi-admin/.../LibraryVipBenefitController.java`：后台权益接口。
- `ruoyi-admin/.../LibraryVipPageConfigController.java`：后台客服配置与 multipart 图片接口。
- `ruoyi-admin/.../wx/WxVipPageController.java`：小程序页面配置接口。
- `ruoyi-ui/src/views/library/vip/benefit/index.vue`：后台“VIP 权益介绍”页面。
- `ruoyi-ui/src/api/library/vipBenefit.js`：后台 API。
- `miniprogram/pages/vip-plans/*` 和 `miniprogram/services/vip.js`：小程序展示与独立重试。
- `docs/2026-07-27-vip-benefit-introduction.sql`：已有数据库升级 SQL。
- `sql/wechat_library.sql`、`sql/wechat_library_menu.sql`：全新环境初始化。

### Task 1: 数据库和菜单契约

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/sql/WechatLibrarySchemaTest.java`
- Create: `docs/2026-07-27-vip-benefit-introduction.sql`
- Modify: `sql/wechat_library.sql`
- Modify: `sql/wechat_library_menu.sql`
- Create: `ruoyi-ui/tests/vip-benefit-introduction.test.js`

- [ ] **Step 1: 写数据库和菜单失败测试**

在 `WechatLibrarySchemaTest` 中把核心表数量更新为 28，并要求存在：

```java
"wl_vip_benefit", "wl_vip_page_config"
```

增加断言：

```java
assertTrue(sql.contains("`benefit_text` varchar(100) not null"));
assertTrue(sql.contains("`customer_service_image_key` varchar(512) default null"));
assertTrue(sql.contains("`customer_service_tip` varchar(100) not null"));
assertTrue(sql.contains("insert into `wl_vip_page_config`"));
```

在前端契约测试中仅读取菜单脚本并断言：

```javascript
assert(menu.includes("'VIP 权益介绍'"))
assert(menu.includes("'library/vip/benefit/index'"))
assert(menu.includes("'library:vip:benefit:list'"))
assert(menu.includes("'library:vip:page-config:edit'"))
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'; & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dtest=WechatLibrarySchemaTest -Dsurefire.failIfNoSpecifiedTests=false test
node ruoyi-ui/tests/vip-benefit-introduction.test.js
```

Expected: Java 测试因缺少两张表失败，Node 测试因菜单和权限尚不存在失败。

- [ ] **Step 3: 写建表、初始数据和菜单 SQL**

`wl_vip_benefit` 使用若依审计字段，并写入三条初始权益：

```sql
INSERT INTO `wl_vip_benefit`
  (`benefit_text`, `sort_order`, `status`, `create_by`, `create_time`,
   `update_by`, `update_time`, `del_flag`)
VALUES
  ('赠送积分', 10, '0', 'system', NOW(), '', NOW(), '0'),
  ('VIP 文档免费下载', 20, '0', 'system', NOW(), '', NOW(), '0'),
  ('VIP 专属课件', 30, '0', 'system', NOW(), '', NOW(), '0');
```

`wl_vip_page_config` 写入固定主键配置：

```sql
INSERT INTO `wl_vip_page_config`
  (`id`, `customer_service_image_key`, `customer_service_tip`,
   `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`)
VALUES
  (1, NULL, '开通 VIP 请添加客服微信', 'system', NOW(), '', NOW(), '0');
```

菜单使用 `2034` 新增“VIP 权益介绍”页面，使用 `2111` 至 `2116` 新增六个操作权限，不执行 SQL。

- [ ] **Step 4: 运行测试并确认 GREEN**

运行 Step 2 的命令；预期全部通过。

- [ ] **Step 5: 提交数据库批次**

```powershell
git add -- docs/2026-07-27-vip-benefit-introduction.sql sql/wechat_library.sql sql/wechat_library_menu.sql ruoyi-wechat-library/src/test/java/com/ruoyi/library/sql/WechatLibrarySchemaTest.java ruoyi-ui/tests/vip-benefit-introduction.test.js
git commit -m "feat: add VIP benefit configuration schema"
```

### Task 2: 权益领域服务与 MyBatis

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlVipBenefit.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlVipPageConfig.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipPageConfigView.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlVipBenefitMapper.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlVipPageConfigMapper.java`
- Create: `ruoyi-wechat-library/src/main/resources/mapper/library/WlVipBenefitMapper.xml`
- Create: `ruoyi-wechat-library/src/main/resources/mapper/library/WlVipPageConfigMapper.xml`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipBenefitService.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipBenefitServiceTest.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java`

- [ ] **Step 1: 写权益服务和 Mapper 失败测试**

服务测试覆盖以下期望：

```java
assertEquals("权益文字不能为空", assertThrows(ServiceException.class,
        () -> service.add(new WlVipBenefit(), "admin")).getMessage());
assertEquals("权益文字不能超过100个字符", assertThrows(ServiceException.class,
        () -> service.add(benefit(repeat("权", 101), 0, "0"), "admin")).getMessage());
assertEquals("权益排序不能小于0", assertThrows(ServiceException.class,
        () -> service.add(benefit("赠送积分", -1, "0"), "admin")).getMessage());
assertEquals("权益状态不正确", assertThrows(ServiceException.class,
        () -> service.add(benefit("赠送积分", 0, "2"), "admin")).getMessage());
```

Mapper 契约测试加载两个新 XML，并断言：

```java
assertTrue(configuration.hasStatement(
        "com.ruoyi.library.mapper.WlVipBenefitMapper.selectEnabled"));
assertTrue(configuration.hasStatement(
        "com.ruoyi.library.mapper.WlVipPageConfigMapper.updateConfigWithExpectedImage"));
```

启用列表 SQL 必须包含：

```sql
where del_flag = '0' and status = '0'
order by sort_order asc, id asc
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'; & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dtest=VipBenefitServiceTest,LibraryMapperXmlContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因类和 Mapper XML 尚不存在而编译或契约失败。

- [ ] **Step 3: 实现最小领域与 Mapper**

`VipBenefitService` 暴露：

```java
public List<WlVipBenefit> list(WlVipBenefit query)
public List<WlVipBenefit> listEnabled()
public WlVipBenefit get(Long id)
public int add(WlVipBenefit benefit, String operator)
public int edit(WlVipBenefit benefit, String operator)
public int remove(Long id, String operator)
```

`WlVipPageConfigMapper` 暴露：

```java
WlVipPageConfig selectConfig();
int updateConfigWithExpectedImage(
        @Param("config") WlVipPageConfig config,
        @Param("expectedImageKey") String expectedImageKey);
```

更新 SQL 固定 `id = 1`，并使用 null-safe 旧图片键条件：

```sql
and (customer_service_image_key = #{expectedImageKey}
  or (customer_service_image_key is null and #{expectedImageKey} is null))
```

- [ ] **Step 4: 运行测试并确认 GREEN**

运行 Step 2 的命令；预期全部通过。

- [ ] **Step 5: 提交领域批次**

```powershell
git add -- ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlVipBenefit.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlVipPageConfig.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipPageConfigView.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlVipBenefitMapper.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlVipPageConfigMapper.java ruoyi-wechat-library/src/main/resources/mapper/library/WlVipBenefitMapper.xml ruoyi-wechat-library/src/main/resources/mapper/library/WlVipPageConfigMapper.xml ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipBenefitService.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipBenefitServiceTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java
git commit -m "feat: add VIP benefit domain service"
```

### Task 3: 客服图片、页面配置服务和接口

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/VipCustomerServiceImageProcessor.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipPageConfigService.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/VipCustomerServiceImageProcessorTest.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipPageConfigServiceTest.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipBenefitController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipPageConfigController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxVipPageController.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryVipPageConfigControllerTest.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/WxVipPageControllerTest.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`

- [ ] **Step 1: 写图片、配置和控制器失败测试**

图片测试要求：

```java
assertEquals("客服微信图片不能超过2MB", oversized.getMessage());
assertEquals("客服微信图片仅支持JPEG、PNG或WebP格式", badType.getMessage());
assertEquals("客服微信图片文件扩展名与实际内容不匹配", mismatch.getMessage());
```

配置服务测试要求：

```java
assertEquals("客服提示语不能为空", blankTip.getMessage());
assertEquals("客服提示语不能超过100个字符", longTip.getMessage());
assertEquals(Arrays.asList("赠送积分", "VIP 文档免费下载"), view.getBenefits());
assertEquals("https://signed.example/customer.webp", view.getCustomerServiceImageUrl());
```

并验证数据库更新失败时删除新对象、更新成功提交后删除旧对象。

后台 multipart 接口：

```java
PUT /library/vip-page-config
parts: config(application/json), image(optional)
```

小程序接口：

```java
GET /wx/vip/page-config
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'; & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am -Dtest=VipCustomerServiceImageProcessorTest,VipPageConfigServiceTest,LibraryVipPageConfigControllerTest,WxVipPageControllerTest,LibraryContentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因新类和接口不存在而失败。

- [ ] **Step 3: 实现图片处理和配置服务**

图片处理器校验文件名扩展名、MIME、实际 ImageIO 格式和解码结果，保留原格式并生成：

```java
public final class ProcessedImage implements AutoCloseable
{
    public InputStream openStream();
    public long getSize();
    public String getContentType();
    public String getExtension();
}
```

新对象键格式：

```java
"vip/customer-service/" + UUID.randomUUID().toString().replace("-", "")
        + "/wechat." + processed.getExtension()
```

页面配置服务暴露：

```java
public VipPageConfigView getManagementView()
public VipPageConfigView getPublicView()
public int update(WlVipPageConfig request, MultipartFile image, String operator)
```

短时 URL 为 30 分钟。上传失败、数据库失败和旧图清理遵循已批准规格的补偿顺序。

- [ ] **Step 4: 实现简体中文控制器**

后台权益接口使用权限：

```java
library:vip:benefit:list
library:vip:benefit:add
library:vip:benefit:edit
library:vip:benefit:remove
```

后台配置接口使用：

```java
library:vip:page-config:query
library:vip:page-config:edit
```

所有 API 方法写简体中文注释。

- [ ] **Step 5: 运行测试并确认 GREEN**

运行 Step 2 的命令；预期全部通过。

- [ ] **Step 6: 提交后端批次**

```powershell
git add -- ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/VipCustomerServiceImageProcessor.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipPageConfigService.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/VipCustomerServiceImageProcessorTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipPageConfigServiceTest.java ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipBenefitController.java ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipPageConfigController.java ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxVipPageController.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryVipPageConfigControllerTest.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/WxVipPageControllerTest.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java
git commit -m "feat: add VIP page configuration APIs"
```

### Task 4: 若依后台“VIP 权益介绍”页面

**Files:**
- Create: `ruoyi-ui/src/api/library/vipBenefit.js`
- Create: `ruoyi-ui/src/views/library/vip/benefit/index.vue`
- Modify: `ruoyi-ui/tests/vip-benefit-introduction.test.js`

- [ ] **Step 1: 扩展失败契约测试**

测试读取 API 和 Vue 文件并断言：

```javascript
assert(page.includes('VIP 权益介绍'))
assert(page.includes('客服微信图片'))
assert(page.includes('开通 VIP 请添加客服微信'))
assert(page.includes('权益文字'))
assert(api.includes('/library/vip-benefit/list'))
assert(api.includes('/library/vip-page-config'))
assert(api.includes("formData.append('config'"))
assert(api.includes("formData.append('image'"))
```

使用 `vue-template-compiler` 编译模板并要求无错误。

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
node ruoyi-ui/tests/vip-benefit-introduction.test.js
```

Expected: 因 API 和页面不存在而失败。

- [ ] **Step 3: 实现后台 API 和页面**

页面顶部为客服配置卡片，文件选择采用 `el-upload` 手动上传并限制：

```javascript
const allowed = ['image/jpeg', 'image/png', 'image/webp']
if (!allowed.includes(file.raw.type)) this.$modal.msgError('客服微信图片仅支持JPEG、PNG或WebP格式')
if (file.raw.size > 2 * 1024 * 1024) this.$modal.msgError('客服微信图片不能超过2MB')
```

下方权益表格包含权益文字、排序、状态和操作列，新增/编辑对话框限制 100 字。保存成功使用简体中文提示并刷新对应区域。

- [ ] **Step 4: 运行测试并确认 GREEN**

运行 Step 2 的命令；预期通过。

- [ ] **Step 5: 提交后台页面批次**

```powershell
git add -- ruoyi-ui/src/api/library/vipBenefit.js ruoyi-ui/src/views/library/vip/benefit/index.vue ruoyi-ui/tests/vip-benefit-introduction.test.js
git commit -m "feat: add VIP benefit introduction admin page"
```

### Task 5: 微信小程序 VIP 套餐页

**Files:**
- Modify: `miniprogram/services/vip.js`
- Modify: `miniprogram/pages/vip-plans/vip-plans.js`
- Modify: `miniprogram/pages/vip-plans/vip-plans.wxml`
- Modify: `miniprogram/pages/vip-plans/vip-plans.wxss`
- Create: `miniprogram/tests/vip-benefit-introduction.test.js`

- [ ] **Step 1: 写小程序失败测试**

测试要求：

```javascript
assert(service.includes("url: '/wx/vip/page-config'"))
assert(markup.includes('VIP 权益'))
assert.match(markup, /wx:for="\{\{pageConfig\.benefits\}\}"/)
assert.match(markup, /wx:if="\{\{pageConfig\.customerServiceImageUrl\}\}"/)
assert(markup.includes('pageConfig.customerServiceTip'))
assert(logic.includes('configError'))
assert(logic.includes('loadPageConfig'))
```

读取 `vip-plans.js` 并断言 `loadPageConfig()` 的 `catch` 分支只向 `setData` 传入
`configLoading` 和 `configError`，不包含 `plans` 或 `profile`；套餐加载失败分支保持现有
`error` 行为。

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
npm test -- --test-name-pattern "VIP 权益介绍"
```

Workdir: `miniprogram`

Expected: 因配置 API 和页面区域不存在而失败。

- [ ] **Step 3: 实现独立加载与展示**

数据状态：

```javascript
pageConfig: { benefits: [], customerServiceTip: '', customerServiceImageUrl: '' },
configLoading: true,
configError: ''
```

套餐和会员仍由 `load()` 加载；`loadPageConfig()` 独立请求并独立失败：

```javascript
loadPageConfig() {
  this.setData({ configLoading: true, configError: '' })
  return vip.pageConfig()
    .then(pageConfig => this.setData({
      pageConfig: pageConfig || { benefits: [] },
      configLoading: false
    }))
    .catch(error => this.setData({
      configLoading: false,
      configError: error.message || 'VIP 权益介绍加载失败，请重试'
    }))
}
```

客服图片使用 `mode="widthFix"`，未配置图片时整个客服区域不渲染。

- [ ] **Step 4: 运行测试并确认 GREEN**

Run:

```powershell
npm test
```

Workdir: `miniprogram`

Expected: 所有小程序测试通过。

- [ ] **Step 5: 提交小程序批次**

```powershell
git add -- miniprogram/services/vip.js miniprogram/pages/vip-plans/vip-plans.js miniprogram/pages/vip-plans/vip-plans.wxml miniprogram/pages/vip-plans/vip-plans.wxss miniprogram/tests/vip-benefit-introduction.test.js
git commit -m "feat: show VIP benefits and customer WeChat"
```

### Task 6: 完整验证与交付检查

**Files:**
- Verify: all files changed by Tasks 1-5

- [ ] **Step 1: 运行相关后端测试**

使用 IntelliJ 自带 Maven 和 Java 8：

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am `
  -Dtest=WechatLibrarySchemaTest,LibraryMapperXmlContractTest,VipBenefitServiceTest,VipCustomerServiceImageProcessorTest,VipPageConfigServiceTest,LibraryVipPageConfigControllerTest,WxVipPageControllerTest,LibraryContentControllerTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: reactor 成功，目标测试 0 失败。

- [ ] **Step 2: 运行前端和小程序测试**

```powershell
node ruoyi-ui/tests/vip-benefit-introduction.test.js
npm test
```

第二条命令的 Workdir 为 `miniprogram`。Expected: 全部通过。

- [ ] **Step 3: 运行构建**

```powershell
npm run build:prod
```

Workdir: `ruoyi-ui`。Expected: 构建退出码为 0；不提交 `ruoyi-ui/dist`。

- [ ] **Step 4: 检查范围和格式**

```powershell
git diff --check
git status --short
git log -6 --oneline
```

确认没有读取或提交 `.env`、`application.yml`、`application-druid.yml`，没有构建产物、环境文件或用户原有未提交改动进入本功能提交。

- [ ] **Step 5: 汇总验证证据**

报告后端测试数量、前端测试结果、小程序测试数量、构建结果、SQL 文件路径和提交列表；不部署、不迁移数据库、不 push。
