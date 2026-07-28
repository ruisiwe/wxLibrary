# VIP Batch Operation and WeChat Profile Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为权益台账增加带头像的微信用户远程多选和批量人工开通/补偿，并将微信头像与文档暂存统一放到 `RuoYiConfig.getWechatProfile()` 的隔离子目录。

**Architecture:** 新增一个只负责解析微信资料根目录的路径组件，头像服务使用 `avatar` 子目录，文档转换和分片上传使用 `document-temp` 子目录。新增批量权益编排服务，复用现有单用户权益发放幂等逻辑；管理端通过 VIP 专用候选接口搜索启用用户，提交隐藏批次标识和用户编号列表。

**Tech Stack:** Java 8、Spring Boot 2.5、Spring MVC/Security、MyBatis、JUnit 5、Mockito、Vue 2、Element UI、Node.js 契约测试。

---

## File Structure

### New files

- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/WechatProfileStoragePaths.java`
  - 统一解析并校验 `wechatProfile`、`avatar`、`document-temp` 三层路径。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/WechatProfileStoragePathsTest.java`
  - 验证统一根目录、空配置及子目录边界。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipUserOptionView.java`
  - 用户候选最小返回视图。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipBatchOperationResult.java`
  - 批量处理人数返回值。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipBatchOperationService.java`
  - 负责请求校验、去重排序、业务编号生成及整批事务。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipBatchOperationServiceTest.java`
  - 验证批量业务编号、排序、去重、上限和候选搜索。
- `ruoyi-ui/tests/vip-entitlement-batch-operation.test.js`
  - 验证弹窗多选、头像、隐藏批次标识、请求体和模板编译。

### Modified files

- `ruoyi-common/src/main/java/com/ruoyi/common/config/RuoYiConfig.java`
  - 保留用户已新增的 `wechatProfile` 属性，补充中文注释并整理现有代码风格。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/AvatarStorageService.java`
  - 从统一路径组件取得 `avatar` 根目录。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/config/AvatarStorageProperties.java`
  - 删除不再使用的 `rootDirectory`，只保留大小和尺寸限制。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/AvatarStorageServiceTest.java`
  - 改用统一路径组件提供测试目录，并断言实际写入 `avatar/yyyyMM`。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/RestrictedProcessDocumentConverter.java`
  - 从统一路径组件取得 `document-temp`。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/BannerImageProcessor.java`
  - 将轮播图处理临时目录放入统一 `document-temp`。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentUploadService.java`
  - 将分片上传会话目录放入统一 `document-temp/upload-sessions`。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/config/DocumentConversionProperties.java`
  - 删除不再使用的 `tempDirectory`，保留转换程序和限制配置。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/RestrictedProcessDocumentConverterTest.java`
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java`
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/PreparedDocumentProcessorTest.java`
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentUploadServiceTest.java`
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/BannerManagementServiceTest.java`
  - 相关测试改为注入统一路径组件，并验证新子目录。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipOperationRequest.java`
  - 将单个 `userId` 和人工 `bizNo` 改为 `userIds` 与隐藏 `batchNo`。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java`
- `ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml`
  - 新增启用用户候选查询，支持昵称和纯数字编号。
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipOperationController.java`
  - 增加候选接口，并将两个操作接口委托给批量服务。
- `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`
  - 增加候选接口权限契约。
- `ruoyi-ui/src/api/library/vip.js`
  - 增加候选用户 API。
- `ruoyi-ui/src/views/library/vip/entitlement/index.vue`
  - 重写为可维护的多行组件，实现头像远程多选和批量提交。

---

### Task 1: Add the Shared WeChat Profile Path Resolver

**Files:**
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/WechatProfileStoragePathsTest.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/WechatProfileStoragePaths.java`
- Modify: `ruoyi-common/src/main/java/com/ruoyi/common/config/RuoYiConfig.java`

- [ ] **Step 1: Write the failing path tests**

```java
class WechatProfileStoragePathsTest
{
    @TempDir Path root;

    @Test
    void resolvesSeparatedAvatarAndDocumentTempDirectories()
    {
        WechatProfileStoragePaths paths = new WechatProfileStoragePaths(() -> root.toString());
        assertEquals(root.resolve("avatar").toAbsolutePath().normalize(), paths.avatarRoot());
        assertEquals(root.resolve("document-temp").toAbsolutePath().normalize(), paths.documentTempRoot());
    }

    @Test
    void rejectsBlankRoot()
    {
        ServiceException error = assertThrows(ServiceException.class,
                () -> new WechatProfileStoragePaths(() -> " ").avatarRoot());
        assertEquals("微信资料存储根目录尚未配置", error.getMessage());
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  -Dtest=WechatProfileStoragePathsTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation failure because `WechatProfileStoragePaths` does not exist.

- [ ] **Step 3: Implement the resolver**

```java
@Component
public class WechatProfileStoragePaths
{
    private final Supplier<String> rootSupplier;

    public WechatProfileStoragePaths()
    {
        this(RuoYiConfig::getWechatProfile);
    }

    WechatProfileStoragePaths(Supplier<String> rootSupplier)
    {
        this.rootSupplier = rootSupplier;
    }

    public Path avatarRoot() { return child("avatar"); }
    public Path documentTempRoot() { return child("document-temp"); }

    private Path child(String name)
    {
        String configured = rootSupplier.get();
        if (configured == null || configured.trim().isEmpty())
            throw new ServiceException("微信资料存储根目录尚未配置");
        Path root = Paths.get(configured.trim()).toAbsolutePath().normalize();
        Path child = root.resolve(name).normalize();
        if (!child.startsWith(root) || !root.equals(child.getParent()))
            throw new ServiceException("微信资料存储路径不合法");
        return child;
    }
}
```

Retain the user's existing `wechatProfile` field/getter/setter in `RuoYiConfig`; format it with the surrounding brace style and add `/** 微信资料本地根路径。 */`.

- [ ] **Step 4: Run the test and verify GREEN**

Run the Step 2 command.

Expected: `WechatProfileStoragePathsTest` passes.

- [ ] **Step 5: Commit**

```powershell
git add ruoyi-common/src/main/java/com/ruoyi/common/config/RuoYiConfig.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/WechatProfileStoragePaths.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/WechatProfileStoragePathsTest.java
git commit -m "feat: add shared WeChat profile storage paths"
```

---

### Task 2: Move Avatar and Document Temporary Files Under the Shared Root

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/AvatarStorageService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/config/AvatarStorageProperties.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/RestrictedProcessDocumentConverter.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/BannerImageProcessor.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentUploadService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/config/DocumentConversionProperties.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/AvatarStorageServiceTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/RestrictedProcessDocumentConverterTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/PreparedDocumentProcessorTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentUploadServiceTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/BannerManagementServiceTest.java`

- [ ] **Step 1: Change the tests first**

Construct or mock the shared path component in each test:

```java
WechatProfileStoragePaths paths = mock(WechatProfileStoragePaths.class);
when(paths.avatarRoot()).thenReturn(root.resolve("avatar"));
when(paths.documentTempRoot()).thenReturn(root.resolve("document-temp"));
```

Update assertions:

```java
assertTrue(Files.isRegularFile(root.resolve("avatar").resolve(storedAvatar)));
assertTrue(Files.isDirectory(root.resolve("document-temp")));
assertTrue(Files.isDirectory(root.resolve("document-temp").resolve("upload-sessions")));
```

Add a regression assertion that the stored database path remains `yyyyMM/UUID.ext`, without the `avatar/` prefix.
Remove obsolete `setTempDirectory(...)` calls from tests that only exercise conversion limits or
prepared-document behavior. Where a test creates `BannerImageProcessor`, pass the mocked shared
path component explicitly.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  -Dtest=AvatarStorageServiceTest,RestrictedProcessDocumentConverterTest,BannerImageProcessorTest,PreparedDocumentProcessorTest,DocumentUploadServiceTest,BannerManagementServiceTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: constructor or path assertions fail because production services still use the old properties.

- [ ] **Step 3: Inject and use `WechatProfileStoragePaths`**

Avatar:

```java
public AvatarStorageService(AvatarStorageProperties properties, WechatProfileStoragePaths paths)
{
    this.properties = properties;
    this.root = paths.avatarRoot();
}
```

Document converter:

```java
public RestrictedProcessDocumentConverter(DocumentConversionProperties properties,
        WechatProfileStoragePaths paths)
{
    this.properties = properties;
    this.paths = paths;
}

private Path createWorkDirectory()
{
    Path root = paths.documentTempRoot();
    Files.createDirectories(root);
    // retain the existing symlink checks and wl-convert- prefix
}
```

Banner processing and upload sessions use `paths.documentTempRoot()` instead of
`properties.getTempDirectory()` or the system temporary directory fallback. Remove
`rootDirectory` from `AvatarStorageProperties` and `tempDirectory` from
`DocumentConversionProperties`; retain all other configuration fields.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the Step 2 command.

Expected: all focused test classes pass and files are created only below `avatar` or `document-temp`.

- [ ] **Step 5: Run constructor wiring regression**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  -Dtest=SpringServiceConstructorInjectionTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: Spring service constructor test passes; if it enumerates the changed beans, update its mocked dependencies to include `WechatProfileStoragePaths`.

- [ ] **Step 6: Commit**

```powershell
git add ruoyi-wechat-library/src/main/java/com/ruoyi/library/config/AvatarStorageProperties.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/config/DocumentConversionProperties.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/AvatarStorageService.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/RestrictedProcessDocumentConverter.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/BannerImageProcessor.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentUploadService.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/AvatarStorageServiceTest.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/RestrictedProcessDocumentConverterTest.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/PreparedDocumentProcessorTest.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentUploadServiceTest.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/BannerManagementServiceTest.java
git commit -m "refactor: unify WeChat profile storage roots"
```

---

### Task 3: Add Batch VIP Operations and Searchable User Candidates

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipUserOptionView.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipBatchOperationResult.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipBatchOperationService.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipBatchOperationServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipOperationRequest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipOperationController.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`

- [ ] **Step 1: Write failing batch service tests**

Cover at least:

```java
@Test
void openDeduplicatesAndSortsUsersAndBuildsPerUserBusinessNumbers()
{
    VipOperationRequest request = request(Arrays.asList(9L, 3L, 9L), "A1B2C3D4E5F6G7H8I9J0");
    request.setPlanId(2L);

    VipBatchOperationResult result = service.open(request, 88L);

    assertEquals(2, result.getProcessedCount());
    InOrder order = inOrder(entitlementService);
    order.verify(entitlementService).openOrRenew(eq(3L), same(plan), eq("MANUAL"),
            eq("MANUAL:A1B2C3D4E5F6G7H8I9J0:3"), eq(88L), eq("线下购买"));
    order.verify(entitlementService).openOrRenew(eq(9L), same(plan), eq("MANUAL"),
            eq("MANUAL:A1B2C3D4E5F6G7H8I9J0:9"), eq(88L), eq("线下购买"));
}

@Test
void compensateUsesCompensationBusinessNumbersAndNoPlan()
{
    VipOperationRequest request = request(Arrays.asList(5L, 6L), "K1L2M3N4O5P6Q7R8S9T0");
    request.setDays(7);
    assertEquals(2, service.compensate(request, 88L).getProcessedCount());
    verify(entitlementService).compensate(5L, 7, 88L, "服务补偿",
            "COMPENSATION:K1L2M3N4O5P6Q7R8S9T0:5");
}
```

Also test empty users, null/invalid IDs, more than 100 distinct IDs, invalid 20-character batch number, blank reason, and numeric candidate keyword parsing.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  -Dtest=VipBatchOperationServiceTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation failure because the batch types and service do not exist.

- [ ] **Step 3: Implement request and result DTOs**

`VipOperationRequest`:

```java
private List<Long> userIds;
private Long planId;
private Integer days;
private String batchNo;
private String reason;
```

`VipBatchOperationResult`:

```java
public final class VipBatchOperationResult
{
    private final int processedCount;
    public VipBatchOperationResult(int processedCount) { this.processedCount = processedCount; }
    public int getProcessedCount() { return processedCount; }
}
```

`VipUserOptionView` contains only `id`, `nickname`, `avatarPath`, and `vipExpireTime`, with a static `from(WlWxUser)` factory.

- [ ] **Step 4: Implement candidate mapper**

Java signature:

```java
List<WlWxUser> selectVipOperationCandidates(@Param("keyword") String keyword,
        @Param("userId") Long userId);
```

MyBatis condition:

```xml
<select id="selectVipOperationCandidates" resultMap="WlWxUserResult">
  <include refid="selectWxUserColumns"/>
  where del_flag = '0' and status = '0'
  <if test="keyword != null and keyword != ''">
    and (nickname like concat('%', #{keyword}, '%')
      <if test="userId != null">or id = #{userId}</if>)
  </if>
  order by id desc
</select>
```

- [ ] **Step 5: Implement transactional batch service**

Use a `TreeSet<Long>` to deduplicate and sort IDs. Validate 1–100 users, batch number
`[A-Za-z0-9]{20}`, and nonblank reason of at most 500 characters.

```java
@Transactional
public VipBatchOperationResult open(VipOperationRequest request, Long operatorId)
{
    List<Long> userIds = normalize(request);
    if (request.getPlanId() == null) throw new ServiceException("会员套餐编号不能为空");
    WlVipPlan plan = planService.getEnabled(request.getPlanId());
    for (Long userId : userIds)
        entitlementService.openOrRenew(userId, plan, "MANUAL",
                bizNo("MANUAL", request.getBatchNo(), userId), operatorId, request.getReason().trim());
    return new VipBatchOperationResult(userIds.size());
}
```

Implement `compensate` the same way with `COMPENSATION`, explicit days validation, and the existing no-gift compensation method. Add `@Transactional` to both public methods.

- [ ] **Step 6: Implement the controller**

Add `VipBatchOperationService` as a dependency. Clamp the endpoint page size to 1–100
before starting PageHelper pagination. Add:

```java
/** 分页查询可执行会员操作的启用微信用户。 */
@PreAuthorize("@ss.hasPermi('library:vip:operation')")
@GetMapping("/user-options")
public TableDataInfo userOptions(@RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize)
{
    PageHelper.startPage(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
    List<WlWxUser> users = batchService.userOptions(keyword);
    long total = new PageInfo<>(users).getTotal();
    return new TableDataInfo(users.stream().map(VipUserOptionView::from)
            .collect(Collectors.toList()), total);
}
```

`open` and `compensate` return `success(batchService.open/compensate(request, getUserId()))`.
Keep the existing Simplified Chinese API comments and operation logs.

- [ ] **Step 7: Add permission assertion**

In `LibraryContentControllerTest`, add:

```java
assertPermission(LibraryVipOperationController.class, "userOptions",
        "library:vip:operation", String.class, int.class, int.class);
```

Use the actual helper signature already present in that test; do not change unrelated assertions.

- [ ] **Step 8: Run focused backend tests and verify GREEN**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library,ruoyi-admin -am `
  -Dtest=VipBatchOperationServiceTest,VipEntitlementServiceTest,LibraryContentControllerTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all specified tests pass.

- [ ] **Step 9: Commit**

```powershell
git add ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipOperationRequest.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipUserOptionView.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/VipBatchOperationResult.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlWxUserMapper.java `
  ruoyi-wechat-library/src/main/resources/mapper/library/WlWxUserMapper.xml `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipBatchOperationService.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipBatchOperationServiceTest.java `
  ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryVipOperationController.java `
  ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java
git commit -m "feat: add batch VIP operations"
```

---

### Task 4: Build the Avatar User Multi-Select Dialog

**Files:**
- Create: `ruoyi-ui/tests/vip-entitlement-batch-operation.test.js`
- Modify: `ruoyi-ui/src/api/library/vip.js`
- Modify: `ruoyi-ui/src/views/library/vip/entitlement/index.vue`

- [ ] **Step 1: Write the failing Vue contract test**

```javascript
assert(page.includes('multiple'), '两个会员操作弹窗应支持用户多选')
assert(page.includes('remote-method'), '用户选择器应支持远程搜索')
assert(page.includes('el-avatar'), '用户选项应显示微信头像')
assert(page.includes('avatarFallback'), '头像加载失败应显示默认占位')
assert(!page.includes('label="业务编号"'), '弹窗不应要求人工填写业务编号')
assert(page.includes('userIds'), '请求应提交用户编号数组')
assert(page.includes('batchNo'), '请求应提交隐藏批次标识')
assert(api.includes("url: '/library/vip-operation/user-options'"),
  '应调用VIP专用用户候选接口')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [],
  `权益台账页面模板编译失败：${compiled.errors.join('；')}`)
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  ruoyi-ui/tests/vip-entitlement-batch-operation.test.js
```

Expected: failure because the API and multi-select UI are absent.

- [ ] **Step 3: Add the API**

```javascript
export const listVipUserOptions = query => request({
  url: '/library/vip-operation/user-options',
  method: 'get',
  params: query
})
```

- [ ] **Step 4: Implement the page**

Rewrite the current minified single-line component into the repository's standard multi-line Vue style.
Use one shared remote multi-select for both modes:

```vue
<el-select
  v-model="form.userIds"
  multiple
  filterable
  remote
  reserve-keyword
  :remote-method="searchUsers"
  :loading="userLoading"
  placeholder="请输入昵称或用户编号"
>
  <el-option v-for="user in userOptions" :key="user.id" :value="user.id">
    <div class="user-option">
      <el-avatar :size="32" :src="avatarUrl(user)" @error="avatarFallback" />
      <span>{{ userLabel(user) }}</span>
    </div>
  </el-option>
</el-select>
```

Maintain a selected-user map and merge it with every search response so selected labels remain visible.
Build avatar URLs with `process.env.VUE_APP_BASE_API + '/wx/public/avatar/' + avatarPath`.

Generate the 20-character batch number once in `show(mode)`:

```javascript
createBatchNo() {
  const value = `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}00000000000000000000`
  return value.replace(/[^a-z0-9]/gi, '').slice(0, 20)
}
```

Submit `{ userIds, planId|days, batchNo, reason }`, keep the same batch number after failure,
disable the confirm button with `:loading="submitting"`, and show
`已成功处理 ${res.data.processedCount} 位用户` after success.

- [ ] **Step 5: Run the contract test and verify GREEN**

Run the Step 2 command.

Expected: `VIP权益批量操作页面契约测试通过`.

- [ ] **Step 6: Run the production Vue build**

Run:

```powershell
npm run build:prod
```

Working directory: `ruoyi-ui`.

Expected: build succeeds. Existing webpack asset-size warnings are acceptable; new template or JavaScript errors are not.

- [ ] **Step 7: Commit**

```powershell
git add ruoyi-ui/src/api/library/vip.js `
  ruoyi-ui/src/views/library/vip/entitlement/index.vue `
  ruoyi-ui/tests/vip-entitlement-batch-operation.test.js
git commit -m "feat: add avatar VIP user multi-select"
```

---

### Task 5: Run Cross-Feature Verification

**Files:**
- Verify only; do not stage build output or environment files.

- [ ] **Step 1: Run focused Java tests**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library,ruoyi-admin -am `
  -Dtest=WechatProfileStoragePathsTest,AvatarStorageServiceTest,RestrictedProcessDocumentConverterTest,BannerImageProcessorTest,PreparedDocumentProcessorTest,DocumentUploadServiceTest,BannerManagementServiceTest,VipBatchOperationServiceTest,VipEntitlementServiceTest,LibraryContentControllerTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all focused tests pass.

- [ ] **Step 2: Run all admin UI contract tests**

Run each `ruoyi-ui/tests/*.test.js` with the bundled Node runtime. Record any pre-existing unrelated failure separately; the new VIP test must pass.

- [ ] **Step 3: Run Vue production build**

Run `npm run build:prod` in `ruoyi-ui`.

Expected: build succeeds without new errors.

- [ ] **Step 4: Check repository hygiene**

Run:

```powershell
git diff --check
git status --short
git status --ignored --short ruoyi-ui/dist
```

Expected:

- no whitespace errors;
- no `.env`, `application.yml`, `application-druid.yml`, `target`, or `ruoyi-ui/dist` staged;
- pre-existing user changes remain present and are not overwritten;
- only task source, tests, docs and the user's existing `RuoYiConfig` change are included in task commits.

- [ ] **Step 5: Final review**

Confirm:

- both dialogs use avatar multi-select;
- both dialogs hide business number;
- both operations produce per-user business numbers;
- batch retries are idempotent;
- avatar files resolve under `<wechatProfile>/avatar`;
- document conversion, banner processing and upload sessions resolve under `<wechatProfile>/document-temp`;
- no old files are migrated or deleted.
