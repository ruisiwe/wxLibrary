# 后台首页统计图表 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后台首页接入真实文库统计数据，并从功能上线后准确记录微信用户日活和文档发送成功行为。

**Architecture:** 后台使用单个 `/library/dashboard` 聚合接口，由独立 Mapper 查询原始计数、Service 按业务时区补齐 7 日和 12 月数据桶、Vue 首页一次请求后分发给 ECharts 组件。小程序登录在现有事务中幂等写入每日活跃表，原文件发送成功后通过独立接口幂等写入发送记录表。

**Tech Stack:** Java 8、Spring Boot、Spring MVC、MyBatis、JUnit 5、Mockito、Vue 2、Element UI、ECharts 5、微信小程序、Node.js 测试。

---

## 文件结构

新增文件：

- `docs/2026-08-03-admin-dashboard-statistics.sql`：两张统计表及索引，可由用户手工执行。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlDocumentSendRecord.java`：发送成功记录实体。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DocumentSendRecordRequest.java`：小程序发送记录请求。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardSummary.java`：顶部四指标。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardCategoryCount.java`：分类计数行及响应项。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardPeriodCategoryCount.java`：月份与分类原始聚合行。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardDailyCount.java`：日期原始聚合行。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardMonthlySeries.java`：分类的 12 月序列。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardMonthlyData.java`：月份轴与堆叠系列。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardTrendData.java`：7 日日期轴及双曲线。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/LibraryDashboardData.java`：聚合接口完整响应。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlDocumentSendRecordMapper.java`：发送记录写入。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/LibraryDashboardMapper.java`：首页统计查询。
- `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentSendRecordMapper.xml`：发送记录 SQL。
- `ruoyi-wechat-library/src/main/resources/mapper/library/LibraryDashboardMapper.xml`：首页统计 SQL。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentSendRecordService.java`：发送权限校验和幂等记录。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/LibraryDashboardService.java`：时间桶构造与聚合响应。
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxDocumentSendRecordController.java`：小程序发送成功上报接口。
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDashboardController.java`：后台首页聚合接口。
- `ruoyi-ui/src/api/library/dashboard.js`：后台聚合接口客户端。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/sql/DashboardStatisticsSchemaTest.java`：交付 SQL 契约。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/DashboardMapperXmlTest.java`：新 Mapper XML 契约。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentSendRecordServiceTest.java`：发送记录服务测试。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/LibraryDashboardServiceTest.java`：聚合与补零测试。
- `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/WxDocumentSendRecordControllerTest.java`：发送记录接口测试。
- `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryDashboardControllerTest.java`：后台聚合接口测试。
- `ruoyi-ui/tests/dashboard-statistics.test.js`：后台首页源码与模板契约测试。

修改文件：

- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java`：增加每日活跃幂等写入方法。
- `ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml`：增加 `insert ignore` 日活 SQL。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java`：成功登录事务内记录当日日活。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java`：验证首次和再次登录写入日活。
- `miniprogram/services/document.js`：增加发送记录请求方法。
- `miniprogram/pages/document-detail/document-detail.js`：仅在微信发送成功后上报。
- `miniprogram/tests/document-access.test.js`：覆盖成功、失败和统计上报失败行为。
- `ruoyi-ui/src/views/index.vue`：加载聚合数据并使用确认后的布局。
- `ruoyi-ui/src/views/dashboard/PanelGroup.vue`：展示四项真实指标。
- `ruoyi-ui/src/views/dashboard/LineChart.vue`：展示 7 日双折线。
- `ruoyi-ui/src/views/dashboard/RaddarChart.vue`：展示分类文档雷达图。
- `ruoyi-ui/src/views/dashboard/PieChart.vue`：展示分类发送占比。
- `ruoyi-ui/src/views/dashboard/BarChart.vue`：展示 12 月分类兑换堆叠柱。

不修改 `application.yml`、`application-druid.yml`、`.env`。不创建分支，不执行建表 SQL，不提交构建产物。

### Task 1: 交付统计表 SQL

**Files:**
- Create: `docs/2026-08-03-admin-dashboard-statistics.sql`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/sql/DashboardStatisticsSchemaTest.java`

- [ ] **Step 1: 写失败的 SQL 契约测试**

测试读取 `docs/2026-08-03-admin-dashboard-statistics.sql`，明确断言：

```java
assertTrue(sql.contains("create table if not exists `wl_wx_user_daily_active`"));
assertTrue(sql.contains("unique key `uk_wx_user_daily_active` (`user_id`, `active_date`)"));
assertTrue(sql.contains("key `idx_wx_user_daily_active_date` (`active_date`)"));
assertTrue(sql.contains("create table if not exists `wl_document_send_record`"));
assertTrue(sql.contains("unique key `uk_document_send_request` (`request_id`)"));
assertTrue(sql.contains("key `idx_document_send_document_time` (`document_id`, `send_time`)"));
assertTrue(sql.contains("key `idx_document_send_time` (`send_time`)"));
assertFalse(sql.contains("foreign key"));
```

- [ ] **Step 2: 运行测试确认 RED**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-wechat-library' '-am' '-Dtest=DashboardStatisticsSchemaTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
```

Expected: FAIL，原因是 SQL 文件尚不存在。

- [ ] **Step 3: 写最小建表 SQL**

SQL 使用 `CREATE TABLE IF NOT EXISTS`，两张表都包含 `id bigint NOT NULL AUTO_INCREMENT`、审计列和 `del_flag`。关键字段固定为：

```sql
`user_id` bigint NOT NULL,
`active_date` date NOT NULL,
UNIQUE KEY `uk_wx_user_daily_active` (`user_id`, `active_date`)
```

以及：

```sql
`user_id` bigint NOT NULL,
`document_id` bigint NOT NULL,
`request_id` varchar(64) NOT NULL,
`send_time` datetime NOT NULL,
UNIQUE KEY `uk_document_send_request` (`request_id`)
```

不添加外键，不写数据回填，不执行该 SQL。

- [ ] **Step 4: 运行测试确认 GREEN**

运行 Step 2 相同命令。Expected: `DashboardStatisticsSchemaTest` PASS。

### Task 2: 成功登录时记录每日活跃

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/auth/WxLoginService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/auth/WxLoginServiceTest.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/DashboardMapperXmlTest.java`

- [ ] **Step 1: 写登录服务失败测试**

在首次登录和已有用户登录测试中分别加入：

```java
verify(userMapper).insertDailyActive(18L);
verify(userMapper).insertDailyActive(9L);
```

并验证登录事务失败时不签发 Token，沿用现有事务回滚测试结构。

- [ ] **Step 2: 写 Mapper XML 失败测试**

解析 `WlWxUserMapper.xml` 并断言 `insertDailyActive` SQL 包含：

```java
assertTrue(sql.contains("insert ignore into wl_wx_user_daily_active"));
assertTrue(sql.contains("curdate()"));
```

- [ ] **Step 3: 运行测试确认 RED**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-wechat-library' '-am' '-Dtest=WxLoginServiceTest,DashboardMapperXmlTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
```

Expected: FAIL，`insertDailyActive` 尚不存在。

- [ ] **Step 4: 实现幂等日活写入**

Mapper 增加：

```java
int insertDailyActive(@Param("id") Long id);
```

XML 增加：

```xml
<insert id="insertDailyActive">
    insert ignore into wl_wx_user_daily_active(user_id, active_date,
        create_by, create_time, update_by, update_time, del_flag)
    values(#{id}, curdate(), concat('wx:', #{id}), now(), '', now(), '0')
</insert>
```

`WxLoginService.loginInTransaction` 在首次创建、并发创建回退和已有用户三个分支最终汇合后执行：

```java
userMapper.insertDailyActive(user.getId());
```

调用必须位于现有登录事务内且位于 Token 签发之前。

- [ ] **Step 5: 运行测试确认 GREEN**

运行 Step 3 相同命令。Expected: 两个测试类 PASS。

### Task 3: 仅在原文件发送成功后幂等上报

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlDocumentSendRecord.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DocumentSendRecordRequest.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlDocumentSendRecordMapper.java`
- Create: `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentSendRecordMapper.xml`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentSendRecordService.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxDocumentSendRecordController.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentSendRecordServiceTest.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/WxDocumentSendRecordControllerTest.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/DashboardMapperXmlTest.java`
- Modify: `miniprogram/services/document.js`
- Modify: `miniprogram/pages/document-detail/document-detail.js`
- Modify: `miniprogram/tests/document-access.test.js`

- [ ] **Step 1: 写发送服务失败测试**

测试固定覆盖：

```java
assertThrows(ServiceException.class, () -> service.record(9L, 8L, ""));
when(accessService.isUnlocked(9L, 8L)).thenReturn(false);
assertEquals("请先兑换文档", assertThrows(ServiceException.class,
        () -> service.record(9L, 8L, "send-1")).getMessage());
```

以及已解锁时写入一条、相同 `requestId` 再次上报返回成功且不重复插入。请求号最长 64 字符，超长返回“发送请求号不能超过64个字符”。

- [ ] **Step 2: 写 Controller 失败测试**

使用 `MockMvcBuilders.standaloneSetup` 和 `WxUserContext` 测试：

```java
mockMvc.perform(post("/wx/documents/8/send-record")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"requestId\":\"send-8-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.recorded").value(true));
```

- [ ] **Step 3: 写小程序失败测试**

源码契约必须断言：

```javascript
assert.match(service, /recordSend/)
assert.match(service, /\/wx\/documents\/\$\{id\}\/send-record/)
assert.match(page, /success:[\s\S]*documents\.recordSend/)
assert.match(page, /fail:\s*reject/)
assert.match(page, /文档发送统计上报失败/)
```

并断言统计上报错误不会进入“发送失败”分支。

`DashboardMapperXmlTest` 同时解析 `WlDocumentSendRecordMapper.xml`，断言 `insertRecord` 包含 `insert ignore into wl_document_send_record`，`selectByRequestId` 只查询 `del_flag = '0'`。

- [ ] **Step 4: 运行测试确认 RED**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-admin' '-am' '-Dtest=DocumentSendRecordServiceTest,WxDocumentSendRecordControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'miniprogram/tests/document-access.test.js'
```

Expected: Java 与小程序契约测试均因功能不存在而 FAIL。

- [ ] **Step 5: 实现后端发送记录**

`DocumentSendRecordService.record` 的顺序固定为：校验用户和请求号、查询相同请求号、校验 `DocumentAccessService.isUnlocked`、构造记录、插入。Mapper 使用 `insert ignore`；若插入返回 0，再查询相同请求号并按幂等成功返回。

Controller 独立于现有已修改的 `WxDocumentController`：

```java
/** 记录当前微信用户成功发送一次文档原文件。 */
@PostMapping("/{id}/send-record")
public WxApiResponse<Map<String, Boolean>> record(@PathVariable Long id,
        @RequestBody DocumentSendRecordRequest request)
{
    service.record(WxUserContext.get(), id, request == null ? null : request.getRequestId());
    return WxApiResponse.success(Collections.singletonMap("recorded", true));
}
```

- [ ] **Step 6: 实现小程序成功回调上报**

`documents.recordSend(id, requestId)` POST 到发送记录接口。详情页包装微信 API：

```javascript
shareFileAndRecord(filePath) {
  const requestId = `send-${this.data.id}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return new Promise((resolve, reject) => wx.shareFileMessage({
    ...documents.buildShareOptions(filePath),
    success: () => documents.recordSend(this.data.id, requestId)
      .catch(error => console.warn('文档发送统计上报失败', error))
      .finally(resolve),
    fail: reject
  }));
}
```

原发送链改为 `.then(filePath => this.shareFileAndRecord(filePath))`。微信取消或失败进入现有“发送失败”提示；微信成功但上报失败仍按发送成功结束。

- [ ] **Step 7: 运行测试确认 GREEN**

运行 Step 4 相同命令。Expected: Java 与小程序测试 PASS。

### Task 4: 实现后台聚合接口

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardSummary.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardCategoryCount.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardPeriodCategoryCount.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardDailyCount.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardMonthlySeries.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardMonthlyData.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DashboardTrendData.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/LibraryDashboardData.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/LibraryDashboardMapper.java`
- Create: `ruoyi-wechat-library/src/main/resources/mapper/library/LibraryDashboardMapper.xml`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/LibraryDashboardService.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDashboardController.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/LibraryDashboardServiceTest.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryDashboardControllerTest.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/DashboardMapperXmlTest.java`

- [ ] **Step 1: 写 Service 失败测试**

使用固定时钟：

```java
Clock clock = Clock.fixed(Instant.parse("2026-08-03T04:00:00Z"), ZoneId.of("Asia/Shanghai"));
service = new LibraryDashboardService(mapper, clock);
```

Mock Mapper 返回两个分类、零散月份和日期，断言：四指标原样返回；月份严格为 `2025-09` 至 `2026-08`；日期严格为 `2026-07-28` 至 `2026-08-03`；缺失桶为 0；分类按 `sortOrder,id` 的 Mapper 返回顺序组装。

- [ ] **Step 2: 写 Mapper SQL 失败测试**

`DashboardMapperXmlTest` 解析新 XML 并断言：

```java
assertTrue(summarySql.contains("from wl_wx_user"));
assertTrue(summarySql.contains("vip_expire_time >"));
assertFalse(summarySql.contains("status = '0'"));
assertTrue(summarySql.contains("from wl_document_unlock"));
assertTrue(summarySql.contains("spent_points > 0"));
assertTrue(monthlySql.contains("date_format(u.unlock_time, '%y-%m')"));
assertTrue(monthlySql.contains("u.spent_points > 0"));
assertTrue(categorySql.contains("from wl_category c"));
assertTrue(categorySql.contains("left join wl_document d"));
assertTrue(categorySql.contains("d.del_flag = '0'"));
assertTrue(sendSql.contains("from wl_document_send_record s"));
assertTrue(sendSql.contains("s.del_flag = '0'"));
```

- [ ] **Step 3: 写 Controller 失败测试**

Mock Service 返回固定 `LibraryDashboardData`，GET `/library/dashboard` 断言 `code=200` 及：

```java
jsonPath("$.data.summary.userCount").value(10)
jsonPath("$.data.monthlyPaidExchanges.months.length()").value(12)
jsonPath("$.data.sevenDayTrend.dates.length()").value(7)
```

- [ ] **Step 4: 运行测试确认 RED**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-admin' '-am' '-Dtest=LibraryDashboardServiceTest,DashboardMapperXmlTest,LibraryDashboardControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
```

Expected: FAIL，新类和新接口尚不存在。

- [ ] **Step 5: 实现 DTO 与 Mapper**

Mapper 方法固定为：

```java
DashboardSummary selectSummary(@Param("now") Date now);
List<DashboardPeriodCategoryCount> selectMonthlyPaidExchangeCounts(
        @Param("start") Date start, @Param("end") Date end);
List<DashboardDailyCount> selectDailyPaidExchangeCounts(
        @Param("start") Date start, @Param("end") Date end);
List<DashboardDailyCount> selectDailyActiveUserCounts(
        @Param("startDate") String startDate, @Param("endDate") String endDate);
List<DashboardCategoryCount> selectCategoryDocumentCounts();
List<DashboardCategoryCount> selectCategorySendCounts();
```

SQL 规则固定为设计文档中的过滤条件。分类文档数从未删除分类出发左连接未删除文档；发送占比只返回 `count > 0` 的分类；所有分类查询按 `c.sort_order, c.id` 排序。

- [ ] **Step 6: 实现补零 Service**

生产构造器使用 `Clock.systemDefaultZone()`，包级测试构造器接收固定 `Clock`。Service 先以 `LocalDate.now(clock)` 计算左闭右开边界，再构造 `LinkedHashMap` 保持日期、月份和分类顺序，最后生成设计文档规定的 JSON 字段。

- [ ] **Step 7: 实现后台 Controller**

```java
/** 查询后台首页全部文库统计数据。 */
@RestController
@RequestMapping("/library/dashboard")
public class LibraryDashboardController extends BaseController
{
    @GetMapping
    public AjaxResult dashboard()
    {
        return success(service.load());
    }
}
```

不新增菜单，不统计 `sys_user`，依赖现有后台登录认证。

- [ ] **Step 8: 运行测试确认 GREEN**

运行 Step 4 相同命令。Expected: 三个测试类 PASS。

### Task 5: 将后台首页替换为真实图表

**Files:**
- Create: `ruoyi-ui/src/api/library/dashboard.js`
- Create: `ruoyi-ui/tests/dashboard-statistics.test.js`
- Modify: `ruoyi-ui/src/views/index.vue`
- Modify: `ruoyi-ui/src/views/dashboard/PanelGroup.vue`
- Modify: `ruoyi-ui/src/views/dashboard/LineChart.vue`
- Modify: `ruoyi-ui/src/views/dashboard/RaddarChart.vue`
- Modify: `ruoyi-ui/src/views/dashboard/PieChart.vue`
- Modify: `ruoyi-ui/src/views/dashboard/BarChart.vue`

- [ ] **Step 1: 写前端失败契约测试**

测试读取 API、首页和五个组件，并使用 `vue-template-compiler` 编译模板。固定断言：

```javascript
assert(api.includes("url: '/library/dashboard'"))
assert(index.includes('<bar-chart :chart-data="dashboard.monthlyPaidExchanges"'))
assert(index.indexOf('<bar-chart') < index.indexOf('<line-chart'))
assert(index.indexOf('<line-chart') < index.indexOf('<raddar-chart'))
assert(index.indexOf('<raddar-chart') < index.indexOf('<pie-chart'))
assert(panel.includes('付费文档数'))
assert(line.includes('兑换文档数') && line.includes('活跃用户数'))
assert(bar.includes('stack:'))
assert(pie.includes('暂无数据'))
```

同时断言 `index.vue` 不再包含 `newVisitis`、`expectedData`、示例英文图例。

- [ ] **Step 2: 运行测试确认 RED**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'tests/dashboard-statistics.test.js'
```

Working directory: `ruoyi-ui`。Expected: FAIL，真实 API 和数据绑定尚不存在。

- [ ] **Step 3: 实现 API 与页面加载**

API：

```javascript
export function getDashboardStatistics() {
  return request({ url: '/library/dashboard', method: 'get' })
}
```

首页数据使用与后端一致的空结构，`created` 调用一次加载方法；失败时执行：

```javascript
this.$modal.msgError('首页统计数据加载失败')
```

布局固定为顶部四卡、中部整行 `BarChart`、底部左 `LineChart`、中 `RaddarChart`、右 `PieChart`。保留现有 staged 首页改造的业务相关内容，不恢复已删除的 RuoYi 宣传首页。

- [ ] **Step 4: 实现五个展示组件**

- `PanelGroup` 接收 `summary`，四个 `CountTo` 的 `end-val` 分别绑定四项指标，不发出点击事件。
- `BarChart` 接收 `{months,series}`，每个分类生成 `type:'bar', stack:'paidDocuments'` 系列。
- `LineChart` 接收 `{dates,paidExchangeCounts,activeUserCounts}`，两条系列名称固定为“兑换文档数”“活跃用户数”。
- `RaddarChart` 接收分类计数数组，indicator 最大值至少为 1，零数据可正常显示。
- `PieChart` 接收正数分类发送数组；空数组时显示组件内“暂无数据”，非空时 ECharts 饼图展示次数及占比。

所有组件监听 prop 深度变化并调用 `setOption(..., true)`，销毁时释放实例，继续复用现有 resize mixin。

- [ ] **Step 5: 运行测试确认 GREEN**

运行 Step 2 相同命令。Expected: 输出“后台首页统计图表契约测试通过”。

- [ ] **Step 6: 运行生产构建**

```powershell
npm run build:prod
```

Working directory: `ruoyi-ui`。Expected: 构建成功；不暂存或提交 `dist`。

### Task 6: 聚焦回归与交付

**Files:**
- Verify only; do not edit unrelated files.

- [ ] **Step 1: 运行全部新增后端测试及受影响登录/文档测试**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-admin' '-am' '-Dtest=DashboardStatisticsSchemaTest,DashboardMapperXmlTest,WxLoginServiceTest,DocumentSendRecordServiceTest,LibraryDashboardServiceTest,WxDocumentSendRecordControllerTest,LibraryDashboardControllerTest,DocumentAccessServiceTest,PointAndDocumentControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
```

Expected: 全部 PASS。

- [ ] **Step 2: 运行小程序与后台前端测试**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'miniprogram/tests/document-access.test.js'
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'ruoyi-ui/tests/dashboard-statistics.test.js'
```

Expected: 两个测试文件全部 PASS。

- [ ] **Step 3: 静态检查改动**

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` 无输出；状态中不出现 `.env`、配置文件、`target`、`node_modules` 或新构建产物。

- [ ] **Step 4: 交付 SQL 与验证结果**

向用户提供 `docs/2026-08-03-admin-dashboard-statistics.sql` 的绝对路径，明确“SQL 未执行”。同时列出后端测试、小程序测试、后台前端测试和构建结果，并说明所有既有无关工作区改动均保留。
