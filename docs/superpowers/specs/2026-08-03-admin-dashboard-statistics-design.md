# 后台首页统计图表设计

## 1. 目标

将 RuoYi 后台首页替换为微信文库业务统计看板。首页一次加载四项累计指标、最近 12 个月分类积分兑换趋势、最近 7 日积分兑换与日活趋势、分类文档库存和分类文档发送占比。

本设计中的“付费文档”统一表示用户实际消耗积分完成的文档兑换记录，不表示文档格式，也不等同于文档当前的访问类型。

## 2. 已确认的业务口径

### 2.1 顶部指标

1. 用户数：统计 `wl_wx_user.del_flag = '0'` 的全部微信注册用户，包含已停用用户，不统计 RuoYi `sys_user`。
2. 会员数：统计未删除且 `vip_expire_time > 当前时间` 的微信用户，包含已停用用户。
3. 文档数：统计 `wl_document.del_flag = '0'` 的全部文档，不限制发布状态或转换状态。
4. 付费文档数：累计统计 `wl_document_unlock.del_flag = '0' AND spent_points > 0` 的成功兑换记录数，不按文档编号去重。

### 2.2 付费兑换判断

- 普通用户花积分兑换 `POINT` 文档时计入。
- 普通用户花积分兑换 `VIP_FREE` 会员文档时也计入。
- 有效会员免费获取会员文档时不计入。
- 0 积分免费文档不计入。

判断依据只使用兑换记录的 `spent_points > 0`，不根据文档当前 `access_type` 推断历史兑换是否付费。

### 2.3 时间范围

- 7 日趋势：包含今天在内的最近 7 个自然日。
- 12 月趋势：包含当月在内的最近 12 个自然月。
- 查询使用左闭右开的时间范围，缺少的日期和月份由服务层补零。
- 自然日与自然月使用系统业务时区。

## 3. 页面布局

桌面端布局：

1. 顶部一行四张指标卡：用户数、会员数、文档数、付费文档数。
2. 中部整行：近 12 个月各分类付费文档堆叠柱状图。
3. 底部三等分：
   - 左：近 7 日兑换文档数、活跃用户数双折线图；
   - 中：各分类文档数雷达图；
   - 右：各分类文档成功发送占比饼图。
4. 窄屏按中部堆叠柱、底部双折线、雷达图、饼图的顺序纵向排列。

顶部卡片仅展示数据，不再通过点击切换折线图。页面不得保留示例假数据。

## 4. 总体架构

后台首页使用单个聚合接口：

```text
GET /library/dashboard
  -> LibraryDashboardController
  -> LibraryDashboardService
  -> LibraryDashboardMapper
  -> DashboardData
```

接口一次返回：

- `summary`：四项顶部指标；
- `monthlyPaidExchanges`：月份轴和按分类组织的堆叠系列；
- `sevenDayTrend`：日期轴、付费兑换次数、活跃用户数；
- `categoryDocumentCounts`：分类文档数量；
- `categorySendShares`：分类发送成功次数。

返回数据结构固定为：

```json
{
  "summary": {
    "userCount": 0,
    "memberCount": 0,
    "documentCount": 0,
    "paidDocumentCount": 0
  },
  "monthlyPaidExchanges": {
    "months": ["2025-09", "2025-10"],
    "series": [
      {
        "categoryId": 1,
        "categoryName": "行业标准",
        "values": [0, 0]
      }
    ]
  },
  "sevenDayTrend": {
    "dates": ["2026-07-28", "2026-07-29"],
    "paidExchangeCounts": [0, 0],
    "activeUserCounts": [0, 0]
  },
  "categoryDocumentCounts": [
    {
      "categoryId": 1,
      "categoryName": "行业标准",
      "count": 0
    }
  ],
  "categorySendShares": [
    {
      "categoryId": 1,
      "categoryName": "行业标准",
      "count": 0
    }
  ]
}
```

示例数组为节选；生产响应必须返回完整的 12 个月和 7 个自然日。

统计 Controller、Service、Mapper 和 DTO 独立于现有用户、文档业务服务，避免把聚合查询混入交易逻辑。接口仅供已登录的 RuoYi 后台用户访问，不依赖微信用户上下文。

## 5. 新增统计数据

### 5.1 微信用户每日活跃表

新增 `wl_wx_user_daily_active`，固定包含：

- `id bigint` 自增主键；
- `user_id bigint` 微信用户编号，非空；
- `active_date date` 活跃日期，非空；
- `create_by varchar(64)`、`create_time datetime`；
- `update_by varchar(64)`、`update_time datetime`；
- `del_flag char(1)`，默认 `'0'`。

建立唯一索引 `uk_wx_user_daily_active(user_id, active_date)` 和日期索引 `idx_wx_user_daily_active_date(active_date)`。`WxLoginService` 完成一次成功登录后，在与登录状态更新一致的事务边界内插入当日记录；重复登录使用幂等写入，同一用户同一天只产生一条有效记录。

该表不通过 `wl_wx_user.last_login_time` 伪造过去每天的日活。准确日活从功能上线后开始累计，页面不额外显示起始时间说明。

### 5.2 文档发送成功记录表

新增 `wl_document_send_record`，固定包含：

- `id bigint` 自增主键；
- `user_id bigint` 微信用户编号，非空；
- `document_id bigint` 文档编号，非空；
- `request_id varchar(64)` 客户端生成的唯一发送请求号，非空；
- `send_time datetime` 发送成功时间，非空；
- `create_by varchar(64)`、`create_time datetime`；
- `update_by varchar(64)`、`update_time datetime`；
- `del_flag char(1)`，默认 `'0'`。

建立唯一索引 `uk_document_send_request(request_id)`，以及统计索引 `idx_document_send_document_time(document_id, send_time)` 和 `idx_document_send_time(send_time)`。记录表保存每次发送成功行为，不按用户或文档去重。

小程序在 `wx.shareFileMessage` 返回成功后调用：

```text
POST /wx/documents/{id}/send-record
```

请求携带本次发送的唯一请求号。后端校验微信登录用户、文档存在，以及当前用户拥有该文档的原文件发送权限，再幂等写入记录。取消、下载失败或微信发送失败均不上报。

发送已经成功但统计上报失败时，不得向用户提示“发送失败”。统计请求可以按相同请求号安全重试，仍失败时只记录前端诊断信息，不改变用户已成功发送的结果。

### 5.3 SQL 交付

实现阶段在 `docs/2026-08-03-admin-dashboard-statistics.sql` 提供两张新表及索引的 SQL。只交付 SQL 文件，不执行数据库迁移。

## 6. 聚合查询规则

### 6.1 四项指标

- 用户数：未删除微信用户总数。
- 会员数：未删除且会员到期时间晚于查询时刻的用户总数，不过滤用户状态。
- 文档数：未删除文档总数。
- 付费文档数：未删除兑换记录中 `spent_points > 0` 的累计记录数。

### 6.2 最近 12 月分类积分兑换

查询最近 12 个自然月内 `spent_points > 0` 的兑换记录，关联文档和分类，按月份及文档当前分类分组计数。分类名称使用后台当前维护名称。服务层构造连续 12 个月，并对每个分类补齐零值。

系列顺序按分类 `sort_order`、分类编号排列。没有兑换的月份显示 0。未删除分类均可形成系列，包括最近 12 个月兑换数全为 0 的分类。

### 6.3 最近 7 日双折线

- 兑换文档数：每天 `spent_points > 0` 的兑换记录数。
- 活跃用户数：`wl_wx_user_daily_active` 每天的有效记录数；唯一索引已经保证按用户去重。

服务层构造连续 7 个自然日，两条曲线分别补零。

### 6.4 分类文档数雷达图

以全部未删除分类为维度，左连接全部未删除文档并计数。零文档分类仍显示，顺序按分类 `sort_order`、分类编号排列。

### 6.5 分类发送占比

累计统计全部有效 `wl_document_send_record`，关联文档当前分类后分组计数。零发送分类不生成饼图扇区；全部分类均无发送记录时，前端显示“暂无数据”。页面不显示统计起始时间说明。

## 7. 前端实现

新增 `ruoyi-ui/src/api/library/dashboard.js` 封装聚合接口。`ruoyi-ui/src/views/index.vue` 负责一次加载、页面级加载状态和数据分发，现有 dashboard 图表组件改为接收真实数据的展示组件。

前端规则：

- 图表标题和标签使用简体中文；
- 顶部卡片数字默认值为 0；
- 分类在堆叠柱、雷达图和饼图中使用稳定颜色映射；
- 后端返回空数组时显示“暂无数据”；
- 聚合接口失败时提示“首页统计数据加载失败”，保留零值和空图；
- ECharts 实例在数据变化时更新，在组件销毁时释放，并继续支持窗口尺寸变化。

小程序文档服务增加发送成功上报方法。文档详情页为每次发送生成请求号，只在 `wx.shareFileMessage` 成功回调后上报。

## 8. 错误处理与安全

- 后台聚合接口的返回与报错使用简体中文。
- 新增 Controller API 注释使用简体中文。
- 发送记录接口必须通过现有微信 Token 上下文取得用户编号，不接受客户端传入用户编号。
- 服务端不信任客户端声明的兑换或会员状态，而是查询现有文档访问权限。
- 唯一请求号冲突按幂等成功处理，不能重复累计。
- 不读取或修改 `.env`、`application.yml`、`application-druid.yml`。
- 不执行发布、部署、数据库迁移或数据删除命令。

## 9. 验证

后端测试覆盖：

- 四项指标的过滤条件；
- 普通用户积分兑换会员文档计入付费统计；
- 0 积分和会员免费获取不计入付费统计；
- 7 日和 12 月边界及缺失桶补零；
- 分类顺序、零文档分类和零发送空状态；
- 同一用户同日多次登录只计一次；
- 同一发送请求重复上报只计一次；
- 无发送权限时拒绝记录；
- 聚合 Controller 和发送记录 Controller 的响应结构。

前端与小程序测试覆盖：

- 首页只调用一次真实聚合接口；
- 指标及四张图与返回字段正确绑定；
- 已确认的页面布局与窄屏顺序；
- 空数据和接口失败提示；
- 只有微信发送成功才上报，取消或失败不上报；
- 发送成功后的统计上报失败不误报为发送失败。

Java 测试使用 Java 8 和 IntelliJ 自带 Maven。前端执行现有针对性 Node 测试及必要的构建检查，不提交构建产物。

## 10. 非目标

- 不统计 RuoYi `sys_user`。
- 不按文档格式类型分组。
- 不回填无法准确还原的历史日活和历史发送成功记录。
- 不增加首页筛选器、日期选择器、自动轮询或卡片跳转。
- 不执行建表 SQL。
