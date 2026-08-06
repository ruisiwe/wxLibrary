# Document Delete COS Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除后台文档记录后清理其关联 COS 对象，同时保证文档下架不触发云存储删除。

**Architecture:** 新增 `DocumentDeletionService` 作为删除协调层，通过 `REQUIRES_NEW` 的 `TransactionTemplate` 在同一独立事务内执行 `SELECT ... FOR UPDATE` 对象键快照和 `DocumentService.removeDocuments`；事务提交成功后才逐个清理 COS。Controller 的删除接口改用协调服务，下架接口继续直接调用 `DocumentService.unpublishDocument`。

**Tech Stack:** Java 8、Spring Boot、Spring `@Service`、腾讯云 COS SDK、JUnit 5、Mockito、Spring MockMvc、IntelliJ IDEA 内置 Maven。

---

### Task 1: 用失败测试定义文档删除后的 COS 清理契约

**Files:**
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentDeletionServiceTest.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentDeletionService.java`

- [ ] **Step 1: 新增“数据库删除后清理全部对象”的失败测试**

测试构造包含四个对象键的草稿文档，模拟数据库删除成功，并使用 Mockito `InOrder` 验证 `removeDocuments` 先于第一次 COS 删除发生：

```java
@Test
void deleteRemovesAllCosObjectsAfterDatabaseDeletion()
{
    WlDocument document = storedDocument(7L);
    when(documentService.getDocument(7L)).thenReturn(document);
    when(documentService.removeDocuments(any(Long[].class), eq("admin"))).thenReturn(1);

    assertEquals(1, service.remove(new Long[] {7L}, "admin"));

    InOrder order = inOrder(documentService, storage);
    order.verify(documentService).removeDocuments(any(Long[].class), eq("admin"));
    order.verify(storage).deleteObjectAfterMetadataDeletion("documents/7/original.pdf");
    verify(storage).deleteObjectAfterMetadataDeletion("documents/7/full.pdf");
    verify(storage).deleteObjectAfterMetadataDeletion("documents/7/preview.pdf");
    verify(storage).deleteObjectAfterMetadataDeletion("documents/7/thumbnail.jpg");
}
```

测试夹具中的文档字段必须显式设置：

```java
private WlDocument storedDocument(Long id)
{
    WlDocument document = new WlDocument();
    document.setId(id);
    document.setPublishStatus("DRAFT");
    document.setOriginalObjectKey("documents/" + id + "/original.pdf");
    document.setFullObjectKey("documents/" + id + "/full.pdf");
    document.setPreviewObjectKey("documents/" + id + "/preview.pdf");
    document.setCoverUrl("documents/" + id + "/thumbnail.jpg");
    return document;
}
```

- [ ] **Step 2: 运行聚焦测试并确认 RED**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am '-Dtest=DocumentDeletionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: `FAIL`，原因是 `DocumentDeletionService` 尚不存在。

- [ ] **Step 3: 实现最小删除协调服务**

创建 `DocumentDeletionService`，构造器注入 `DocumentService` 和 `CosPrivateStorageService`。`remove` 先校验编号、读取文档快照，再调用数据库删除；成功后收集四个字段并逐个清理：

```java
/** 后台文档逻辑删除与 COS 对象清理协调服务。 */
@Service
public class DocumentDeletionService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeletionService.class);
    private final DocumentService documentService;
    private final CosPrivateStorageService storage;

    public DocumentDeletionService(DocumentService documentService, CosPrivateStorageService storage)
    {
        this.documentService = documentService;
        this.storage = storage;
    }

    /** 逻辑删除文档，并在数据库删除成功后清理关联的私有对象。 */
    public int remove(Long[] ids, String operator)
    {
        requireIds(ids);
        List<WlDocument> documents = new ArrayList<>();
        for (Long id : ids) documents.add(documentService.getDocument(id));
        int rows = documentService.removeDocuments(ids, operator);
        Set<String> objectKeys = new LinkedHashSet<>();
        for (WlDocument document : documents)
        {
            addObjectKey(objectKeys, document.getOriginalObjectKey(), false);
            addObjectKey(objectKeys, document.getFullObjectKey(), false);
            addObjectKey(objectKeys, document.getPreviewObjectKey(), false);
            addObjectKey(objectKeys, document.getCoverUrl(), true);
        }
        for (String objectKey : objectKeys) cleanupObject(objectKey);
        return rows;
    }
}
```

辅助方法必须实现以下确定行为：空值/空白跳过，对象键 `trim()`，外链缩略图跳过，`LinkedHashSet` 去重；`cleanupObject` 捕获单个 `RuntimeException` 并记录 `文档云存储对象清理失败，对象键：{}` 后继续；非法编号使用现有中文提示“请选择要操作的数据”或“数据编号不正确”。

- [ ] **Step 4: 运行聚焦测试并确认 GREEN**

重复 Step 2 的 Maven 命令。

Expected: `DocumentDeletionServiceTest` 通过。

### Task 2: 覆盖失败、去重和外链边界

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentDeletionServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentDeletionService.java`

- [ ] **Step 1: 新增数据库失败不清理测试**

```java
@Test
void databaseFailureDoesNotDeleteCosObjects()
{
    when(documentService.getDocument(7L)).thenReturn(storedDocument(7L));
    when(documentService.removeDocuments(any(Long[].class), eq("admin")))
            .thenThrow(new ServiceException("文档状态已变化，请刷新后重试"));
    assertThrows(ServiceException.class, () -> service.remove(new Long[] {7L}, "admin"));
    verify(storage, never()).deleteObjectAfterMetadataDeletion(anyString());
}
```

- [ ] **Step 2: 新增部分 COS 失败仍继续清理测试**

```java
@Test
void storageFailureDoesNotStopRemainingCleanup()
{
    when(documentService.getDocument(7L)).thenReturn(storedDocument(7L));
    when(documentService.removeDocuments(any(Long[].class), eq("admin"))).thenReturn(1);
    doThrow(new ServiceException("COS删除失败"))
            .when(storage).deleteObjectAfterMetadataDeletion("documents/7/original.pdf");
    assertEquals(1, service.remove(new Long[] {7L}, "admin"));
    verify(storage).deleteObjectAfterMetadataDeletion("documents/7/preview.pdf");
    verify(storage).deleteObjectAfterMetadataDeletion("documents/7/thumbnail.jpg");
}
```

- [ ] **Step 3: 新增空值、重复键和外链缩略图跳过测试**

```java
@Test
void deleteSkipsBlankDuplicateAndExternalKeys()
{
    WlDocument document = storedDocument(7L);
    document.setFullObjectKey(" ");
    document.setPreviewObjectKey("documents/7/original.pdf");
    document.setCoverUrl("https://legacy.example/thumbnail.jpg");
    when(documentService.getDocument(7L)).thenReturn(document);
    when(documentService.removeDocuments(any(Long[].class), eq("admin"))).thenReturn(1);
    assertEquals(1, service.remove(new Long[] {7L}, "admin"));
    verify(storage).deleteObjectAfterMetadataDeletion("documents/7/original.pdf");
    verify(storage, never()).deleteObjectAfterMetadataDeletion("https://legacy.example/thumbnail.jpg");
    verifyNoMoreInteractions(storage);
}
```

- [ ] **Step 4: 运行聚焦测试**

使用 Task 1 Step 2 的 Maven 命令。

Expected: 所有 `DocumentDeletionServiceTest` 测试通过，COS 异常测试只出现预期警告日志。

### Task 3: 后台删除接口切换到协调服务

**Files:**
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryDocumentControllerTest.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDocumentController.java`

- [ ] **Step 1: 先修改 Controller 测试并确认 RED**

测试新增 `DocumentDeletionService` mock，通过覆盖 `getUsername()` 的 `TestController` 构建 MockMvc，并验证 `DELETE /library/document/7,8` 调用 `deletionService.remove(ids, "admin")`，且不直接调用 `documentService.removeDocuments`：

```java
@Test
void removeUsesDocumentDeletionService() throws Exception
{
    when(deletionService.remove(any(Long[].class), eq("admin"))).thenReturn(2);
    mockMvc.perform(delete("/library/document/7,8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    verify(deletionService).remove(
            argThat(ids -> Arrays.equals(ids, new Long[] {7L, 8L})), eq("admin"));
    verify(documentService, never()).removeDocuments(any(Long[].class), anyString());
}
```

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am '-Dtest=LibraryDocumentControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: `FAIL`，原因是 Controller 尚未接收或调用 `DocumentDeletionService`。

- [ ] **Step 2: 修改 Controller 构造器和删除接口**

构造器新增 `DocumentDeletionService deletionService` 并保存字段。删除接口改为：

```java
/** 删除未上架的文档，并在数据库删除成功后清理云存储文件。 */
@DeleteMapping("/{ids}")
public AjaxResult remove(@PathVariable Long[] ids)
{
    return toAjax(deletionService.remove(ids, getUsername()));
}
```

`unpublish` 方法保持调用 `documentService.unpublishDocument`，不引用 `deletionService`。

- [ ] **Step 3: 重跑 Controller 聚焦测试并确认 GREEN**

重复 Task 3 Step 1 的 Maven 命令。

Expected: `LibraryDocumentControllerTest` 全部通过。

### Task 4: 聚焦回归与静态检查

**Files:**
- Verify only; no production files should be added in this task.

- [ ] **Step 1: 运行服务与 Controller 聚焦测试**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am '-Dtest=DocumentDeletionServiceTest,DocumentServiceTest,LibraryDocumentControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 三个聚焦测试类全部通过；未连接真实 COS。

- [ ] **Step 2: 检查下架路径和目标文件空白错误**

```powershell
rg -n "unpublishDocument|deleteObjectAfterMetadataDeletion|deletionService" ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDocumentController.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentDeletionService.java
git diff --check -- ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDocumentController.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryDocumentControllerTest.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentDeletionService.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentDeletionServiceTest.java
```

Expected: `unpublish` 仍只调用 `DocumentService.unpublishDocument`；COS 删除仅出现在协调服务；目标文件没有空白错误。

- [ ] **Step 3: 审查最终差异**

```powershell
git diff -- ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryDocumentController.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryDocumentControllerTest.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentDeletionService.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentDeletionServiceTest.java
```

Expected: 只包含已确认的文档删除 COS 清理、Controller 路由切换和相关测试，不包含前端、配置、数据库或下架逻辑改动。

### Task 5: 代码审查后的并发窗口修正

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlDocumentMapper.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentMapper.xml`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentDeletionService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentDeletionServiceTest.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/DocumentDeletionMapperXmlTest.java`

- [ ] **Step 1: 写入失败测试**

调整协调服务测试，要求 `lockDocumentsForDeletion(ids)` 和 `removeDocuments(ids, operator)` 发生在同一 `TransactionTemplate` 中，并记录事务事件以断言 `commit` 早于第一次 `deleteObjectAfterMetadataDeletion`。新增 Mapper XML 契约测试，要求 `selectDocumentsForUpdate` 查询包含 `where id in (...)`、`del_flag = '0'` 和 `for update`。

- [ ] **Step 2: 确认 RED**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am '-Dtest=DocumentDeletionServiceTest,DocumentDeletionMapperXmlTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: `FAIL`，原因是事务管理构造器、`lockDocumentsForDeletion` 和行锁查询尚不存在。

- [ ] **Step 3: 实现事务行锁与提交后清理**

在 Mapper 中新增 `selectDocumentsForUpdate(Long[] ids)`，SQL 使用 `FOR UPDATE`；`DocumentService.lockDocumentsForDeletion` 校验返回数量；`DocumentDeletionService` 注入 `PlatformTransactionManager`，通过 `TransactionTemplate` 先锁定并快照文档、再逻辑删除，事务返回后执行 COS 清理。

为防止协调服务加入调用方的外层事务并在其提交前清理 COS，`TransactionTemplate` 必须显式使用 `TransactionDefinition.PROPAGATION_REQUIRES_NEW`；测试记录并断言该传播级别。

- [ ] **Step 4: 确认 GREEN**

重复 Step 2 命令。

Expected: 两个测试类共 5 项测试通过，数据库失败触发事务回滚，COS 删除只发生在提交之后。
