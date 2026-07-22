# Banner Document Search Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace manual banner document ID entry with a remote searchable selector limited to published documents in enabled categories.

**Architecture:** Add a banner-scoped lightweight document-option query through `DocumentService`, expose it from `LibraryBannerController`, and project document display/availability fields with banner management rows. Add a reusable Vue remote-select form control used by `SimpleList`, then configure the banner page to search, display, validate, and save the selected document ID.

**Tech Stack:** Java 8, Spring MVC/Security, MyBatis XML, JUnit 5/Mockito/MockMvc, Vue 2, Element UI.

---

### Task 1: Add the published-document option query

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DocumentOptionDto.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlDocumentMapper.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlDocumentMapper.xml`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java`

- [ ] **Step 1: Write failing service and mapper-contract tests**

Add a service test that calls:

```java
PageResult<DocumentOptionDto> result = service.listBannerDocumentOptions(" 质量%_ ", 0, 99);

verify(documentMapper).countBannerDocumentOptions("质量\\%\\_");
verify(documentMapper).selectBannerDocumentOptions("质量\\%\\_", 0L, 20);
assertEquals(1, result.getPageNum());
assertEquals(20, result.getPageSize());
```

Add mapper contract assertions for `countBannerDocumentOptions` and `selectBannerDocumentOptions`, and assert the generated select SQL contains published/category-enabled filters and does not contain `object_key`.

- [ ] **Step 2: Run the focused tests and confirm they fail**

Run with `JAVA_HOME=E:\JDK8` and IntelliJ bundled Maven:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-wechat-library' '-am' '-DskipTests=false' '-Dtest=DocumentServiceTest,LibraryMapperXmlContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
```

Expected: compilation or test failure because the DTO and mapper/service methods do not exist.

- [ ] **Step 3: Add the minimal DTO, mapper query, and service paging logic**

Create a DTO containing only:

```java
private Long id;
private String title;
private String categoryName;
private String fileFormat;
```

Add mapper methods:

```java
long countBannerDocumentOptions(@Param("keyword") String keyword);
List<DocumentOptionDto> selectBannerDocumentOptions(@Param("keyword") String keyword,
        @Param("offset") long offset, @Param("limit") int limit);
```

The SQL must join `wl_document` to `wl_category`, require `PUBLISHED`, non-deleted rows and category status `0`, search only `d.title`, sort by `d.publish_time desc, d.id desc`, and select only the four DTO fields.

Add `DocumentService.listBannerDocumentOptions` with page number default 1, page size default/max 20, escaped LIKE input, a count query, and no select query when total is zero.

- [ ] **Step 4: Run the focused tests and confirm they pass**

Run the Task 1 Maven command again. Expected: all selected tests pass.

### Task 2: Expose the option endpoint and banner association state

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/domain/WlBanner.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlBannerMapper.xml`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryBannerController.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java`

- [ ] **Step 1: Write failing controller and projection tests**

Add a standalone `LibraryBannerController` MockMvc test that stubs:

```java
when(documentService.listBannerDocumentOptions("质量", 1, 20))
        .thenReturn(new PageResult<>(Collections.singletonList(option), 1L, 1, 20));
```

Then assert `GET /library/banner/document-options?keyword=质量&pageNum=1&pageSize=20` returns `data.items[0].title`, `categoryName`, and `fileFormat`, while `originalObjectKey` is absent. Add a reflection assertion that `documentOptions` uses `@ss.hasAnyPermi('library:banner:add,library:banner:edit')`.

Extend the mapper contract test to assert banner management SQL contains `document_title` and `document_selectable`.

- [ ] **Step 2: Run the focused tests and confirm they fail**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-admin' '-am' '-DskipTests=false' '-Dtest=LibraryContentControllerTest,LibraryMapperXmlContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'test'
```

Expected: failure because the endpoint and banner projection fields are absent.

- [ ] **Step 3: Implement the endpoint and management projection**

Add these non-persisted management fields to `WlBanner`:

```java
private String documentTitle;
private String documentCategoryName;
private String documentFileFormat;
private Boolean documentSelectable;
```

Change only banner management list/detail selects to left join the document and category and compute `documentSelectable` from published/non-deleted/enabled state. Keep insert/update and the public banner query behavior unchanged.

Add the controller method:

```java
/** 搜索宣传图片可关联的已发布文档。 */
@PreAuthorize("@ss.hasAnyPermi('library:banner:add,library:banner:edit')")
@GetMapping("/document-options")
public AjaxResult documentOptions(@RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize)
{
    return success(documentService.listBannerDocumentOptions(keyword, pageNum, pageSize));
}
```

Place `/document-options` before the `/{id}` mapping for readability; Spring selects the exact path correctly either way.

- [ ] **Step 4: Run the focused backend tests and confirm they pass**

Run the Task 2 Maven command again. Expected: all selected tests pass.

### Task 3: Add the reusable remote selector and configure banners

**Files:**
- Create: `ruoyi-ui/src/views/library/common/RemoteSelect.vue`
- Modify: `ruoyi-ui/src/views/library/common/SimpleList.vue`
- Modify: `ruoyi-ui/src/api/library/content.js`
- Modify: `ruoyi-ui/src/views/library/content/banner/index.vue`

- [ ] **Step 1: Add the banner option API function**

Add:

```javascript
export const listBannerDocumentOptions = query => request({
  url: '/library/banner/document-options', method: 'get', params: query
})
```

- [ ] **Step 2: Implement `RemoteSelect.vue`**

The component must accept `value`, `field`, and `row`; use Element UI remote filtering; load recent options when opened; debounce typed searches with `field.debounce || 300`; merge the currently selected option into refreshed results; show `field.emptyText` or `field.loadErrorText`; and emit both `input` and `selection-change`. It must clear its pending timer in `beforeDestroy`.

Option behavior must be configured through these field callbacks/properties:

```javascript
remoteLoader(keyword)        // Promise<Array<option>>
optionValue                  // defaults to 'value'
optionLabel(option)          // display string
optionDisabled(option)       // disabled state
initialOption(row)           // edit-form seed option
```

- [ ] **Step 3: Integrate remote-select validation into `SimpleList.vue`**

Register `RemoteSelect` and render it when `field.type === 'remote-select'`. On selection, update `field.selectableProp` on the form. Before calling the creator/updater, evaluate optional field validation:

```javascript
const invalid = this.formFields.find(field => field.validate && !field.validate(this.form))
if (invalid) return this.$modal.msgError(invalid.validationMessage)
```

Keep all existing number/select/datetime/text field behavior unchanged.

- [ ] **Step 4: Replace banner document-number entry with search selection**

Configure the banner field with label `关联文档`, type `remote-select`, and a loader that calls the option endpoint with `pageNum: 1, pageSize: 20`. Format each option as `标题 / 分类 / 文件格式`, keep `id` as the value, seed edit forms from the banner projection fields, disable unselectable seed options, and validate `documentSelectable !== false` with message `原关联文档已下架，请重新选择`.

Change the banner list column from `documentId` to `documentTitle`.

- [ ] **Step 5: Compile the Vue application**

Run:

```powershell
npm run build:prod
```

from `ruoyi-ui`. Expected: production build completes without Vue template or JavaScript errors. Do not stage `ruoyi-ui/dist`.

### Task 4: Final verification and scoped commit

**Files:** All files listed above plus this plan.

- [ ] **Step 1: Run all focused backend tests**

Run the Task 2 Maven command and confirm there are no failures or errors.

- [ ] **Step 2: Run frontend production compilation**

Run `npm run build:prod` from `ruoyi-ui` and confirm success.

- [ ] **Step 3: Review the exact feature diff**

Run `git diff --check` for only the feature paths, confirm no generated output or sensitive configuration is included, and verify existing unrelated staged/unstaged changes remain untouched.

- [ ] **Step 4: Commit only this feature**

Stage and commit only the DTO, mapper, service, controller, focused tests, Vue API/component/page files, design, and plan. Use commit message:

```text
feat: add searchable banner document selection
```

Do not push.
