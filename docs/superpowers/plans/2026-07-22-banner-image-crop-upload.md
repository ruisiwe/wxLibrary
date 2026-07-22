# Banner Image Crop Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将管理端轮播图改为浏览器本地选图和固定比例裁剪，后端只接收并保存 1240×480 JPEG 到现有私有 COS，并为管理端和小程序签发短时预览地址。

**Architecture:** 前端使用 Element UI 与现有 `vue-cropper` 生成固定尺寸 JPEG，再以 multipart 的 `banner` JSON 和 `image` 文件提交。后端由图片处理器负责真实格式、尺寸、像素和临时目录安全校验，由轮播图管理服务协调 COS 上传、数据库事务和新旧对象补偿；数据库始终只保存 COS 对象键，查询时才签发短时 URL。历史 `http://`/`https://` 地址只读兼容，不回写签名地址，也不尝试从 COS 删除。

**Tech Stack:** Vue 2、Element UI、vue-cropper、Canvas、Spring Boot 2、Spring MVC multipart、Java 8 ImageIO、MyBatis、Spring TransactionTemplate、腾讯云 COS Java SDK、JUnit 5、Mockito、MockMvc、Node.js 源码契约测试。

---

## File map

- Create `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/BannerImageProcessor.java`: 校验并标准化上传的轮播图，管理受控临时目录。
- Create `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/BannerManagementService.java`: 协调图片处理、COS、事务、并发条件更新及补偿删除。
- Create `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/BannerImagePreviewResult.java`: 管理端预览接口返回对象。
- Modify `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`: 保留轮播图元数据校验，增加带旧图片键条件的更新、删除入口。
- Modify `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlBannerMapper.java`: 增加单条条件更新、条件删除方法。
- Modify `ruoyi-wechat-library/src/main/resources/mapper/library/WlBannerMapper.xml`: 用旧图片键作为并发条件，防止错误删除刚被替换的对象。
- Modify `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/HomeQueryService.java`: 对首页轮播图对象键签发短时 URL。
- Modify `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryBannerController.java`: 改成 multipart 新增/修改，提供预览接口并委托新管理服务。
- Create `ruoyi-ui/src/views/library/content/banner/BannerImageCropper.vue`: 固定 31:12 裁剪和 1240×480 JPEG 输出。
- Rewrite `ruoyi-ui/src/views/library/content/banner/index.vue`: 专用轮播图管理页、远程文档选择、裁剪预览和 multipart 保存。
- Modify `ruoyi-ui/src/api/library/content.js`: 构造 multipart 请求并新增图片预览 API。
- Create `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java`: 图片真实性、尺寸、大小和符号链接测试。
- Create `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/BannerManagementServiceTest.java`: 上传/事务/补偿/历史 URL 行为测试。
- Modify `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java`: 元数据校验和并发条件测试。
- Modify `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/HomeQueryServiceTest.java`: 首页轮播图签名测试。
- Create `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryBannerControllerTest.java`: 真实 multipart MockMvc 契约测试。
- Create `ruoyi-ui/tests/banner-image-crop-upload.test.js`: 前端组件和 API 源码契约测试。
- Modify `ruoyi-ui/package.json`: 注册轮播图专项测试脚本。

No database migration is required: `wl_banner.image_url` continues to store a string, but new records store an object key such as `banners/7d8f.../image.jpg`.

### Task 1: Secure banner image normalization

**Files:**
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/BannerImageProcessor.java`

- [ ] **Step 1: Write failing tests for the accepted JPEG contract**

Create tests that build images in memory and verify the public contract:

```java
@TempDir Path tempDirectory;

@Test
void normalizesExactBannerJpeg() throws Exception {
    BannerImageProcessor processor = processor(tempDirectory);
    MockMultipartFile image = jpeg("banner.jpg", 1240, 480);
    try (BannerImageProcessor.ProcessedBannerImage processed = processor.process(image)) {
        assertEquals("image/jpeg", processed.getContentType());
        assertTrue(processed.getSize() > 0L);
        BufferedImage normalized = ImageIO.read(processed.openStream());
        assertEquals(1240, normalized.getWidth());
        assertEquals(480, normalized.getHeight());
    }
}

@Test
void rejectsWrongDimensions() {
    ServiceException error = assertThrows(ServiceException.class,
        () -> processor(tempDirectory).process(jpeg("banner.jpg", 1200, 480)));
    assertEquals("轮播图尺寸必须为1240×480像素", error.getMessage());
}

@Test
void rejectsPngRenamedAsJpeg() {
    MockMultipartFile image = new MockMultipartFile("image", "banner.jpg", "image/jpeg", png(1240, 480));
    ServiceException error = assertThrows(ServiceException.class,
        () -> processor(tempDirectory).process(image));
    assertEquals("轮播图必须为真实的JPEG图片", error.getMessage());
}
```

Also cover empty input, original filename other than `.jpg`/`.jpeg`, MIME other than `image/jpeg`, content over 5 MiB, decoded pixel count over the configured safety ceiling, cleanup on close, and a symbolic-link root/month/session path when the platform supports symbolic links.

- [ ] **Step 2: Run the focused test and verify RED**

Run from the repository root:

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path='E:\JDK8\bin;' + $env:Path
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dcheckstyle.skip=true -Dtest=BannerImageProcessorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `BannerImageProcessor` does not exist.

- [ ] **Step 3: Implement the processor and its closeable result**

Implement these exact boundaries:

```java
@Component
public class BannerImageProcessor {
    static final int WIDTH = 1240;
    static final int HEIGHT = 480;
    static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    static final long MAX_PIXELS = 1240L * 480L;

    public ProcessedBannerImage process(MultipartFile image) { /* validate, copy bounded, inspect, decode, re-encode */ }

    public static final class ProcessedBannerImage implements AutoCloseable {
        public InputStream openStream() throws IOException;
        public long getSize();
        public String getContentType(); // always image/jpeg
        @Override public void close(); // recursively deletes only the validated session directory
    }
}
```

Use `DocumentConversionProperties.getTempDirectory()` and create only `<configured>/banner-images/wl-banner-<32 lowercase hex>`. Resolve every path with `toAbsolutePath().normalize()`, require `session.startsWith(container)`, and call `Files.isSymbolicLink` for the configured root, `banner-images` container, session directory, input and normalized output before use. Copy through a counted buffer and stop at `MAX_UPLOAD_BYTES + 1`; never trust `MultipartFile.getSize()` alone. Use an `ImageReader` selected from the input stream, require format name `JPEG`, read width/height before decode, reject pixel multiplication beyond `MAX_PIXELS`, decode once, and re-encode with `ImageIO.write(..., "jpg", ...)` to `normalized.jpg`.

- [ ] **Step 4: Run the processor tests and verify GREEN**

Run the Step 2 command again. Expected: all `BannerImageProcessorTest` cases PASS.

- [ ] **Step 5: Commit the processor**

```powershell
git add ruoyi-wechat-library/src/main/java/com/ruoyi/library/storage/BannerImageProcessor.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/storage/BannerImageProcessorTest.java
git commit -m "feat: validate banner image uploads"
```

### Task 2: Optimistic banner metadata mutations

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlBannerMapper.java`
- Modify: `ruoyi-wechat-library/src/main/resources/mapper/library/WlBannerMapper.xml`

- [ ] **Step 1: Write failing service and mapper contract tests**

Add tests proving the client cannot silently race a replacement:

```java
@Test
void updateBannerRequiresExpectedImageKey() {
    WlBanner current = banner(4L, "banners/old/image.jpg");
    WlBanner changed = banner(4L, "banners/new/image.jpg");
    when(bannerMapper.selectBannerById(4L)).thenReturn(current);
    when(documentMapper.selectDocumentById(changed.getDocumentId())).thenReturn(publishedDocument());
    when(bannerMapper.updateBannerWithExpectedImage(changed, "banners/old/image.jpg")).thenReturn(0);
    ServiceException error = assertThrows(ServiceException.class,
        () -> service.updateBanner(changed, "banners/old/image.jpg", "admin"));
    assertEquals("轮播图已发生变化，请刷新后重试", error.getMessage());
}
```

The XML contract test must assert both mutations contain `image_url = #{expectedImageUrl}` in the `where` clause and that the update still checks the associated document is published.

- [ ] **Step 2: Run focused tests and verify RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dcheckstyle.skip=true -Dtest=DocumentServiceTest,LibraryMapperXmlContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the conditional mapper methods and overloaded service methods do not exist.

- [ ] **Step 3: Add exact mapper methods and SQL**

Use these signatures:

```java
int updateBannerWithExpectedImage(@Param("banner") WlBanner banner,
        @Param("expectedImageUrl") String expectedImageUrl);
int deleteBannerWithExpectedImage(@Param("id") Long id,
        @Param("expectedImageUrl") String expectedImageUrl,
        @Param("operator") String operator);
```

Bind update properties through `#{banner.title}` etc. and use:

```xml
where id = #{banner.id} and del_flag = '0' and image_url = #{expectedImageUrl}
  and exists (select 1 from wl_document d where d.id = #{banner.documentId}
    and d.publish_status = 'PUBLISHED' and d.del_flag = '0')
```

The delete statement must update one row only with `id`, `del_flag = '0'`, and `image_url = #{expectedImageUrl}`.

- [ ] **Step 4: Expose narrow service operations**

Keep `validateBanner` as the single metadata validator, but make these operations available:

```java
public int updateBanner(WlBanner banner, String expectedImageUrl, String operator) {
    requireId(...);
    validateBanner(banner);
    normalizeBanner(banner, operator);
    int rows = bannerMapper.updateBannerWithExpectedImage(banner, expectedImageUrl);
    if (rows != 1) throw new ServiceException("轮播图已发生变化，请刷新后重试");
    return rows;
}

public int removeBanner(Long id, String expectedImageUrl, String operator) {
    int rows = bannerMapper.deleteBannerWithExpectedImage(id, expectedImageUrl, operator);
    if (rows != 1) throw new ServiceException("轮播图已发生变化，请刷新后重试");
    return rows;
}
```

Do not retain a bulk SQL delete path for controller use; batch behavior will be transactionally looped by `BannerManagementService` so each row carries its expected image key.

- [ ] **Step 5: Run tests and commit**

Run the Step 2 command. Expected: PASS.

```powershell
git add ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/mapper/WlBannerMapper.java ruoyi-wechat-library/src/main/resources/mapper/library/WlBannerMapper.xml ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/mapper/LibraryMapperXmlContractTest.java
git commit -m "fix: guard banner image mutations"
```

### Task 3: COS lifecycle and transactional compensation

**Files:**
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/BannerManagementServiceTest.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/BannerManagementService.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/BannerImagePreviewResult.java`

- [ ] **Step 1: Write failing lifecycle tests**

Use mocks for `BannerImageProcessor`, `CosPrivateStorageService`, `DocumentService`, and `PlatformTransactionManager`. Cover all of these independent outcomes:

```java
@Test void addDeletesNewObjectWhenDatabaseInsertFails();
@Test void updateWithoutImagePreservesExistingObjectKey();
@Test void updateDeletesNewObjectWhenTransactionFails();
@Test void updateDeletesOldObjectOnlyAfterCommit();
@Test void updateNeverDeletesLegacyHttpImage();
@Test void deleteRollsBackAllRowsWhenOneExpectedKeyChanged();
@Test void deleteRemovesCosObjectsOnlyAfterCommit();
@Test void previewSignsObjectKeyForThirtyMinutes();
@Test void previewReturnsLegacyHttpImageWithoutSigning();
```

For the commit-order test, record events from the transaction manager and COS mock and assert `commit` occurs before `deleteObjectAfterMetadataDeletion(oldKey)`.

- [ ] **Step 2: Run the focused test and verify RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dcheckstyle.skip=true -Dtest=BannerManagementServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the service and DTO do not exist.

- [ ] **Step 3: Implement the management service**

Expose this API:

```java
@Service
public class BannerManagementService {
    public int add(WlBanner banner, MultipartFile image, String operator);
    public int update(WlBanner banner, MultipartFile image, String operator);
    public int remove(Long[] ids, String operator);
    public BannerImagePreviewResult preview(Long id);
}

public class BannerImagePreviewResult {
    private String imageUrl;
    public BannerImagePreviewResult(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageUrl() { return imageUrl; }
}
```

Generate keys only as `banners/<UUID without hyphens>/image.jpg`. For add, discard any client `imageUrl`, process and upload first, then run `documentService.addBanner` inside `TransactionTemplate.execute`; any exception after upload deletes the new key and rethrows the original safe exception. For update, load the current row first, preserve the old key when the multipart image is absent/empty, otherwise upload a new key and call `documentService.updateBanner(banner, oldKey, operator)` in the transaction. Delete the new key on rollback; delete the old key only after successful commit.

For remove, load all current rows before the transaction, reject duplicate/invalid IDs, then call `documentService.removeBanner(id, expectedKey, operator)` for every row inside one transaction. Only after commit iterate old keys and delete COS objects. Treat `http://` and `https://` values as legacy external URLs: return them directly for preview and never send them to the COS delete method. Cleanup failures after commit must be logged with the object key and must not turn a committed database operation into a reported failure.

Sign preview keys using `Duration.ofMinutes(30)` with `downloadFileName = null`. Use Chinese `ServiceException` messages such as `轮播图图片不能为空`, `轮播图保存失败，请重试`, and `轮播图图片服务暂不可用，请稍后重试`.

- [ ] **Step 4: Run tests and commit**

Run the Step 2 command. Expected: PASS.

```powershell
git add ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/BannerManagementService.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/BannerImagePreviewResult.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/BannerManagementServiceTest.java
git commit -m "feat: manage banner image lifecycle"
```

### Task 4: Multipart management controller

**Files:**
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryBannerControllerTest.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryBannerController.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`

- [ ] **Step 1: Write real MockMvc multipart tests**

Build standalone MockMvc with a mocked `BannerManagementService`. Verify `banner` deserializes as JSON and `image` arrives as a file:

```java
mockMvc.perform(multipart("/library/banner")
        .file(new MockMultipartFile("banner", "", "application/json",
            "{\"title\":\"首页推荐\",\"documentId\":9}".getBytes(StandardCharsets.UTF_8)))
        .file(new MockMultipartFile("image", "banner.jpg", "image/jpeg", jpegBytes)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200));
verify(service).add(argThat(b -> "首页推荐".equals(b.getTitle()) && b.getDocumentId() == 9L),
    any(MultipartFile.class), anyString());
```

Add a PUT request using `.with(request -> { request.setMethod("PUT"); return request; })` with no `image` part and verify update receives `null`; add GET `/library/banner/4/image` returning `data.imageUrl`; add a missing required POST image test expecting a safe 4xx response.

- [ ] **Step 2: Run the controller test and verify RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am -Dcheckstyle.skip=true -Dtest=LibraryBannerControllerTest,LibraryContentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the controller still consumes JSON and has no preview method.

- [ ] **Step 3: Change the controller contract**

Inject both `DocumentService` for list/detail/document options and `BannerManagementService` for mutations/preview. Use exact endpoint shapes:

```java
/** 新增首页轮播图，并上传裁剪后的本地图片。 */
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public AjaxResult add(@RequestPart("banner") WlBanner banner,
        @RequestPart("image") MultipartFile image) {
    return toAjax(bannerManagementService.add(banner, image, getUsername()));
}

/** 修改首页轮播图；未上传新图片时保留原图。 */
@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public AjaxResult edit(@RequestPart("banner") WlBanner banner,
        @RequestPart(value = "image", required = false) MultipartFile image) { ... }

/** 获取首页轮播图短时预览地址。 */
@GetMapping("/{id}/image")
@PreAuthorize("@ss.hasAnyPermi('library:banner:list,library:banner:edit')")
public AjaxResult image(@PathVariable Long id) { ... }
```

Keep existing add/edit/remove permissions and Chinese `@Log` titles. Update the reflection permission test to assert the new preview expression.

- [ ] **Step 4: Run tests and commit**

Run the Step 2 command. Expected: PASS.

```powershell
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryBannerController.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryBannerControllerTest.java ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java
git commit -m "feat: accept multipart banner images"
```

### Task 5: Sign banner URLs for the mini program

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/HomeQueryServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/HomeQueryService.java`

- [ ] **Step 1: Write failing signing tests**

```java
@Test
void homeSignsPrivateBannerKeysWithoutChangingLegacyUrls() throws Exception {
    BannerDto privateBanner = banner("banners/a/image.jpg");
    BannerDto legacyBanner = banner("https://old.example/banner.jpg");
    when(bannerMapper.selectPublicBanners(any(Date.class)))
        .thenReturn(Arrays.asList(privateBanner, legacyBanner));
    when(signer.signGetUrl(eq("banners/a/image.jpg"), eq(Duration.ofMinutes(30)), isNull()))
        .thenReturn(new URL("https://signed.example/banner.jpg"));
    HomeData home = service.getHome(1, 10);
    assertEquals("https://signed.example/banner.jpg", home.getBanners().get(0).getImageUrl());
    assertEquals("https://old.example/banner.jpg", home.getBanners().get(1).getImageUrl());
}
```

Also assert a missing signer or null signed URL for a private key throws `轮播图图片服务暂不可用，请稍后重试`.

- [ ] **Step 2: Run test and verify RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -Dcheckstyle.skip=true -Dtest=HomeQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `getHome` returns raw banner keys.

- [ ] **Step 3: Sign banners before creating HomeData**

Use one local list to avoid querying twice:

```java
List<BannerDto> banners = bannerMapper.selectPublicBanners(new Date());
signBanners(banners);
return new HomeData(banners, categoryMapper.selectPublicCategories(), documents);
```

`signBanner` must mirror the cover behavior: skip blank and `http(s)` values, call the existing `PrivateFileUrlSigner` for object keys with 30-minute TTL, assign the returned URL to the DTO only, and never persist it.

- [ ] **Step 4: Run test and commit**

Run the Step 2 command. Expected: PASS.

```powershell
git add ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/HomeQueryService.java ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/HomeQueryServiceTest.java
git commit -m "feat: sign mini program banner urls"
```

### Task 6: Frontend multipart API and fixed crop component

**Files:**
- Create: `ruoyi-ui/tests/banner-image-crop-upload.test.js`
- Modify: `ruoyi-ui/package.json`
- Modify: `ruoyi-ui/src/api/library/content.js`
- Create: `ruoyi-ui/src/views/library/content/banner/BannerImageCropper.vue`

- [ ] **Step 1: Write a failing frontend source contract test**

The Node test must read the component/API sources and assert these non-negotiable contracts:

```js
assert.match(cropper, /fixed-number=["']\[31,\s*12\]["']/)
assert.match(cropper, /canvas\.width\s*=\s*1240/)
assert.match(cropper, /canvas\.height\s*=\s*480/)
assert.match(cropper, /toBlob\([^]*['"]image\/jpeg['"]/)
assert.match(api, /new Blob\([^]*application\/json/)
assert.match(api, /formData\.append\(['"]banner['"]/)
assert.match(api, /formData\.append\(['"]image['"]/)
assert.match(api, /\/library\/banner\/\$\{id\}\/image/)
assert.doesNotMatch(page, /图片地址/)
```

Add `"test:banner-image": "node tests/banner-image-crop-upload.test.js"` to scripts.

- [ ] **Step 2: Run the frontend test and verify RED**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' ruoyi-ui/tests/banner-image-crop-upload.test.js
```

Expected: FAIL because the crop component and multipart API are absent.

- [ ] **Step 3: Implement API helpers**

Use a single helper so POST and PUT serialize consistently:

```js
const bannerFormData = (banner, image) => {
  const formData = new FormData()
  formData.append('banner', new Blob([JSON.stringify(banner)], { type: 'application/json' }))
  if (image) formData.append('image', image, 'banner.jpg')
  return formData
}

export const addBanner = (banner, image) => request({
  url: '/library/banner', method: 'post', data: bannerFormData(banner, image),
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
})
export const updateBanner = (banner, image) => request({
  url: '/library/banner', method: 'put', data: bannerFormData(banner, image),
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
})
export const getBannerImage = id => request({ url: `/library/banner/${id}/image`, method: 'get' })
```

- [ ] **Step 4: Implement the crop component**

Use `el-upload` with `:auto-upload="false"`, `:show-file-list="false"`, and `accept="image/jpeg,image/png"`. `beforeUpload`/change validation must allow only JPG/PNG and at most 5 MiB. Feed `URL.createObjectURL(file.raw)` to `VueCropper` with `:fixed="true"`, `:fixed-number="[31, 12]"`, `:fixed-box="true"`, and no free scaling.

On confirmation call `getCropBlob`, load it into an `Image`, draw to a new Canvas whose width is 1240 and height is 480, then call `canvas.toBlob(callback, 'image/jpeg', 0.9)`. Emit `change` with `{ blob, previewUrl }`; revoke every replaced source/result URL and all remaining URLs in `beforeDestroy`. Display `输出尺寸：1240×480，比例31:12` in simplified Chinese.

- [ ] **Step 5: Run test and commit**

Run the Step 2 command. Expected: PASS.

```powershell
git add ruoyi-ui/src/api/library/content.js ruoyi-ui/src/views/library/content/banner/BannerImageCropper.vue ruoyi-ui/tests/banner-image-crop-upload.test.js ruoyi-ui/package.json
git commit -m "feat: crop banner images in browser"
```

### Task 7: Dedicated banner management page

**Files:**
- Modify: `ruoyi-ui/tests/banner-image-crop-upload.test.js`
- Rewrite: `ruoyi-ui/src/views/library/content/banner/index.vue`
- Reuse: `ruoyi-ui/src/views/library/common/RemoteSelect.vue`

- [ ] **Step 1: Extend the failing page contract**

Assert the page imports `BannerImageCropper`, calls `getBannerImage` during edit, requires a crop blob only for new records, uses the existing paged remote document search, and calls `addBanner(this.form, this.bannerBlob)` / `updateBanner(this.form, this.bannerBlob)`. Assert it does not render a free-text `imageUrl` input.

- [ ] **Step 2: Run the source test and verify RED**

Run the Task 6 Step 2 command. Expected: FAIL on the page-specific assertions.

- [ ] **Step 3: Build the dedicated page**

Retain existing RuoYi list behaviors: search by title/status, pagination, add/edit/delete permissions, loading state, selection and confirmation messages. The dialog form fields are title, associated published document via searchable `RemoteSelect`, sort order, status, start time, end time, and the crop component.

Maintain these states explicitly:

```js
data() {
  return {
    form: emptyForm(), bannerBlob: null, bannerPreviewUrl: '',
    existingImageUrl: '', saving: false, documentOptions: [], documentLoading: false
  }
}
```

On add, clear all image state and require `bannerBlob`. On edit, load banner metadata and call `getBannerImage(id)` to show the current signed preview; a new crop replaces only local preview state until save succeeds. On submit, validate the form, block duplicate clicks with `saving`, and keep the dialog, blob and form untouched when the request fails so the administrator can retry. Close/reset/revoke local URLs only after success or an explicit cancel. The request body must omit/ignore `imageUrl`; the backend owns that field.

- [ ] **Step 4: Run frontend tests and production build**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' ruoyi-ui/tests/banner-image-crop-upload.test.js
npm --prefix ruoyi-ui run build:prod
```

Expected: source test PASS and Vue production build exits 0. Do not add `ruoyi-ui/dist` to git.

- [ ] **Step 5: Commit the page**

```powershell
git add ruoyi-ui/src/views/library/content/banner/index.vue ruoyi-ui/tests/banner-image-crop-upload.test.js
git commit -m "feat: upload cropped banner images"
```

### Task 8: End-to-end verification and quality review

**Files:**
- Review all files changed in Tasks 1–7.
- Do not modify or stage pre-existing unrelated files: `miniprogram/app.js`, `miniprogram/project.config.json`, `ruoyi-ui/vue.config.js`, or `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/TencentFile.java` unless a fresh failure proves one is directly required.

- [ ] **Step 1: Run the complete related Maven suite on Java 8**

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path='E:\JDK8\bin;' + $env:Path
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am -Dcheckstyle.skip=true -Dtest=BannerImageProcessorTest,BannerManagementServiceTest,DocumentServiceTest,LibraryMapperXmlContractTest,HomeQueryServiceTest,LibraryBannerControllerTest,LibraryContentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: BUILD SUCCESS with every named test passing.

- [ ] **Step 2: Run all frontend source tests and build**

```powershell
Get-ChildItem -LiteralPath 'ruoyi-ui\tests' -Filter '*.test.js' | ForEach-Object { & 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' $_.FullName; if ($LASTEXITCODE -ne 0) { throw "前端测试失败: $($_.Name)" } }
npm --prefix ruoyi-ui run build:prod
```

Expected: every Node test exits 0 and the Vue production build succeeds.

- [ ] **Step 3: Review security, transaction and compatibility invariants**

Inspect the final diff and verify:

- no endpoint accepts a client-supplied stored `imageUrl`;
- only real 1240×480 JPEG reaches COS;
- upload copy is bounded and temp paths reject symbolic links;
- add/update rollback deletes the newly uploaded object;
- update/delete remove old objects only after database commit;
- expected old image keys protect concurrent replacement;
- legacy HTTP(S) images remain previewable and are never COS-deleted;
- signed URLs appear only in response DTOs and are never persisted;
- all API comments, response messages and errors are simplified Chinese without Unicode escapes;
- no environment file, build output, migration, deployment or data deletion is included.

- [ ] **Step 4: Check formatting and the exact staged scope**

```powershell
git diff --check
git status --short
git diff --stat
```

Expected: no whitespace errors; only Task 1–7 files plus the already-known unrelated dirty files are shown. Remove build outputs from the working tree without deleting user files, and stage only the feature paths.

- [ ] **Step 5: Commit any review fixes, then report**

If the quality review required source/test fixes, stage only those exact feature files and commit:

```powershell
git commit -m "fix: harden banner image upload"
```

If no fixes were needed, do not create an empty commit. Report the Maven result, Node test count, build result, commits created, unchanged pre-existing dirty files, and confirm that no push, deployment, migration or data deletion was performed.
