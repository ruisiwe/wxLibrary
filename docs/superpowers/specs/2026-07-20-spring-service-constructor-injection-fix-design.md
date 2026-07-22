# Spring Service 构造器注入修复设计

## 背景

`PointService`、`VipEntitlementService` 和 `VideoPlaybackService` 均包含两个构造器：一个供生产环境注入依赖，另一个额外接收 `Clock` 以支持可控时间测试。生产构造器未标注 `@Autowired`，Spring 无法从多个候选构造器中选择注入入口，随后尝试调用不存在的无参构造器，导致应用启动失败。

## 修复范围

- 给三个 Service 的公开生产构造器添加 `@Autowired`。
- 保留带 `Clock` 的包级构造器，继续供现有单元测试使用。
- 不增加无参构造器，不改变业务方法、接口、数据库结构或配置文件。
- 不读取或修改 `.env`、`application.yml`、`application-druid.yml`。

涉及类：

- `com.ruoyi.library.service.PointService`
- `com.ruoyi.library.service.VipEntitlementService`
- `com.ruoyi.library.service.VideoPlaybackService`

## 实现方式

三个类分别导入 `org.springframework.beans.factory.annotation.Autowired`，并仅在公开生产构造器上添加 `@Autowired`。测试构造器不添加 Spring 注解，避免 Spring 尝试解析测试专用的 `Clock` 参数。

该方案与项目中 `VipOrderService`、`VipRefundService`、`DocumentConversionService` 和 `CosPrivateStorageService` 的多构造器注入方式保持一致。

## 测试设计

先增加一个轻量 Spring 容器回归测试，注册三个 Service 所需的模拟依赖，并请求创建对应 Bean。修复前，测试应因找不到无参构造器而失败；修复后，三个 Bean 均应成功创建。

验证顺序：

1. 运行新增回归测试并确认按预期失败。
2. 添加三个 `@Autowired` 注解。
3. 重新运行回归测试并确认通过。
4. 使用 `E:\JDK8` 和 IntelliJ IDEA 2026.1.2 自带 Maven 执行 Java 全量 `clean test`。
5. 检查 `git diff --check`，确保没有格式错误或构建产物进入改动。

## 成功标准

- Spring 能明确选择三个 Service 的生产构造器。
- 原始的 `NoSuchMethodException: PointService.<init>()` 不再出现。
- 同类的 `VipEntitlementService` 和 `VideoPlaybackService` 启动隐患同时消除。
- 新增回归测试与原有 Java 测试全部通过。
