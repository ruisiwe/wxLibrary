# 微信资料库后台页面与表格样式统一设计

## 目标

将微信资料库后台模块中的列表页和表格逐页调整为项目现有的若依列表页风格：

- 单页面单功能区不再显示重复的页面功能标题；
- 去掉包裹单功能区的最外层卡片边框；
- 表格不显示外侧边框、竖向边框和斑马纹，只保留项目标准的表头与横向分隔线；
- 同一页面存在多个配置区时保留各配置区标题；
- 不改变任何接口、字段、权限、分页和业务流程。

## 设计原则

本次采用逐页面控制方案，不使用全局 CSS 覆盖，也不强制改变公共组件的全部调用方。

公共列表组件只增加显式样式开关。每个页面必须主动启用无标题、无卡片外框和无边框表格模式，因此页面之间可以独立控制，未启用开关的调用方式保持原样。

## 视觉基准

以项目现有若依标准列表页为基准：

- 页面内容直接放在 `app-container` 中；
- 查询区位于表格上方；
- 操作按钮使用紧凑的 `mini`、`plain` 风格，并放入 `mb8` 操作行；
- 刷新操作使用右侧圆形工具按钮；
- `el-table` 不使用 `border` 和 `stripe`；
- 分页位于表格下方；
- 页面不增加没有业务作用的装饰性标题。

本次只统一现有功能的布局与表格外观，不为原本没有查询条件、批量操作或分页接口的页面新增业务功能。

## 公共列表组件

文件：

- `ruoyi-ui/src/views/library/common/SimpleList.vue`

增加两个显式布尔属性：

- `plain`：启用无页面标题、无卡片外框、无边框且无斑马纹的表格样式；
- `embedded`：用于已经位于页面 `app-container` 内的列表，避免重复容器和重复内边距。

`plain` 模式下：

- 不渲染 `title` 对应的页面功能标题；
- 不使用 `el-card` 作为最外层容器；
- 新增按钮进入标准 `mb8` 操作行，并使用 `primary plain mini`；
- 刷新操作改为右侧 `right-toolbar`，不显示搜索按钮；
- 表格的 `border` 和 `stripe` 均为关闭状态。

`title` 属性仍保留，用于新增/修改弹窗标题和删除确认文案，不改变业务语义。

非 `plain` 模式保持当前表现，避免公共组件产生隐式的全局视觉变化。

## 逐页调整清单

### 使用 `SimpleList` 的页面

以下页面逐个显式传入 `plain`：

- `library/points/record/index.vue`
- `library/points/rule/index.vue`
- `library/vip/order/index.vue`
- `library/vip/plan/index.vue`
- `library/content/category/index.vue`
- `library/vip/code/index.vue`
- `library/access/courseCode/index.vue`

其中已经自行包含 `app-container` 的会员码和课程码页面同时传入 `embedded`，并将列表与页面操作栏之间的间距保持为项目标准值。

### 自行编写表格的页面

以下页面逐个删除列表表格的 `border` 和 `stripe`，不影响弹窗内的 `el-descriptions border`：

- `library/user/index.vue`
- `library/vip/entitlement/index.vue`
- `library/vip/refund/index.vue`
- `library/content/banner/index.vue`
- `library/content/document/index.vue`
- `library/content/video/index.vue`
- `library/content/course/index.vue`
- `library/agreement/index.vue`
- `library/vip/benefit/index.vue`

各页面现有新增、修改、删除、人工开通、补偿、退款等按钮逐页整理为标准紧凑操作栏。页面若已有查询表单则保留查询表单；页面若只有刷新能力，则使用右侧刷新工具按钮。

### 多配置区页面

`library/vip/benefit/index.vue` 同时包含客服微信配置和权益文字列表：

- 删除页面顶部重复的“VIP 权益介绍”总标题；
- 保留“客服微信配置”和“权益文字列表”两个分区标题；
- 保留用于区分两个配置区的区块结构；
- 权益文字表格去掉 `border` 和 `stripe`。

### 不属于本次删除范围的标题和边框

以下内容具有业务含义，继续保留：

- 新增、修改、退款、人工开通、补偿等弹窗标题；
- 弹窗中的提示和警告；
- 文档上传后的“文件处理结果”等结果区标题；
- 弹窗内用于字段说明的 `el-descriptions border`；
- 多配置区页面中的分区标题；
- 表格下方分页组件。

## 数据与交互

此次修改不改变数据流：

- 页面仍调用原有 loader、查询和保存接口；
- 查询参数、分页参数和返回结构不变；
- 权限指令不变；
- 刷新按钮仍调用各页面原有加载方法；
- 表格列、操作列和弹窗字段不变。

不新增后端代码、数据库表、SQL 文件或数据迁移。

## 兼容现有改动

当前工作区已有与本任务无关的未提交改动。实施时必须逐文件检查并只修改样式相关位置，尤其不得覆盖：

- 小程序首页和搜索页的现有改动；
- `library/vip/plan/index.vue` 中用户现有改动；
- `DocumentService.java` 中用户现有改动。

构建生成的 `ruoyi-ui/dist` 继续保持忽略状态，不加入提交。

## 验证

新增管理端静态契约测试，逐页验证：

- 所有列出的微信资料库列表表格均未启用 `border` 或 `stripe`；
- 所有列出的 `SimpleList` 调用方都显式启用 `plain`；
- 已位于页面容器中的 `SimpleList` 调用方显式启用 `embedded`；
- `SimpleList` 的 `plain` 模式不显示页面功能标题，且保留 `title` 的弹窗用途；
- VIP 权益介绍页面删除总标题但保留两个配置区标题；
- Vue 模板可以正常编译。

随后运行：

- 新增的页面样式契约测试；
- 现有管理端契约测试；
- `npm run build:prod`。

已知横幅裁剪比例旧契约要求 `112:55`，而当前业务实现为 `952:550`。该无关失败单独记录，不在本任务中修改。

## 验收标准

- 微信资料库模块的单功能列表页不再显示重复功能标题或最外层卡片边框；
- 所有业务列表表格不再显示外侧边框、竖向边框和斑马纹；
- 多配置区页面仍能清楚区分各配置区；
- 按钮、刷新和分页位置与若依标准列表页一致；
- 原有功能、权限、接口调用和弹窗行为没有变化；
- 管理端生产构建成功；
- 任务文件提交独立，不包含用户现有改动和构建产物。
