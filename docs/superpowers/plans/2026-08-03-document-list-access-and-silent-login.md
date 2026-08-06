# Document List Access and Silent Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Return usable thumbnail URLs in My Documents, add uppercase file-format badges to homepage rows, let active VIP and zero-point documents enter the existing send flow without a manual redemption prompt, and silently restore existing users on homepage entry.

**Architecture:** Extract the existing cover signing code into `DocumentCoverUrlService` and inject it into both public document queries and unlocked-document queries. Preserve the unlock row as the original-file authorization boundary, but extend unlock requests with `freeOnly` so automatic free unlocks can never silently charge points. Keep homepage login independent from anonymous content loading and scope the new format badge to homepage use of the shared row component.

**Tech Stack:** Java 8, Spring Boot, MyBatis, JUnit 5, Mockito, WeChat Mini Program JavaScript/WXML/WXSS, Node.js built-in test runner.

---

### Task 1: Extract shared document cover URL signing

**Files:**
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentCoverUrlService.java`
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentCoverUrlServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/HomeQueryService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/HomeQueryServiceTest.java`

- [ ] **Step 1: Write failing shared-service tests**

Add tests that construct `DocumentCoverUrlService` with a mocked `ObjectProvider<PrivateFileUrlSigner>` and verify:

```java
@Test
void signsRelativeCoverForThirtyMinutes() throws Exception
{
    DocumentSummaryDto document = document("documents/session/thumbnail/v1.jpg");
    when(signer.signGetUrl("documents/session/thumbnail/v1.jpg", Duration.ofMinutes(30), null))
            .thenReturn(new URL("https://temporary.example/cover"));

    service.signCover(document);

    assertEquals("https://temporary.example/cover", document.getCoverUrl());
    verify(signer).signGetUrl("documents/session/thumbnail/v1.jpg", Duration.ofMinutes(30), null);
}
```

Also verify empty covers and `http://`/`https://` covers remain unchanged, and a missing signer produces `ServiceException` with `缩略图服务暂不可用，请稍后重试`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'; & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-wechat-library -am -DskipTests=false -Dtest=DocumentCoverUrlServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `DocumentCoverUrlService` does not exist.

- [ ] **Step 3: Implement the shared service**

Create a Spring `@Service` with `signCover(DocumentSummaryDto)` and `signCovers(List<DocumentSummaryDto>)`. Use a constant `Duration.ofMinutes(30)`, preserve empty and absolute URLs, and normalize all signing failures to the approved Chinese message.

- [ ] **Step 4: Switch HomeQueryService to the shared service**

Replace the cover-specific signer provider and private `signCover/signCovers` methods with constructor-injected `DocumentCoverUrlService`. Keep the existing banner signer provider and banner signing methods in `HomeQueryService` because their error message and DTO type differ.

Update `HomeQueryServiceTest` to mock `DocumentCoverUrlService`, inject it into the production constructor, and verify `signCovers`/`signCover` are called for home, search, and detail results. Move relative/absolute cover behavior assertions to `DocumentCoverUrlServiceTest`.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run the Maven command from Step 2 with:

```text
-Dtest=DocumentCoverUrlServiceTest,HomeQueryServiceTest
```

Expected: both test classes PASS.

### Task 2: Sign My Documents covers through the shared service

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentAccessService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentAccessServiceTest.java`

- [ ] **Step 1: Write a failing unlocked-list test**

Add a mocked `DocumentCoverUrlService` to test setup and verify the exact mapper result is passed through the signer:

```java
@Test
void unlockedDocumentsUseSharedCoverSigning()
{
    List<DocumentSummaryDto> documents = Collections.singletonList(new DocumentSummaryDto());
    when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
    when(unlockMapper.selectUnlockedDocuments(11L)).thenReturn(documents);

    assertEquals(documents, service.listUnlocked(11L));

    verify(coverUrlService).signCovers(documents);
}
```

- [ ] **Step 2: Run the test and verify RED**

Run the focused Maven command with `-Dtest=DocumentAccessServiceTest`.

Expected: FAIL because the production service does not call `DocumentCoverUrlService`.

- [ ] **Step 3: Implement minimal list signing**

Inject `DocumentCoverUrlService` into `DocumentAccessService`. In `listUnlocked`, validate the user, fetch the list, call `coverUrlService.signCovers(documents)`, and return the same list. Do not change `listFavorites` in this task.

- [ ] **Step 4: Re-run the focused test and verify GREEN**

Expected: `DocumentAccessServiceTest` PASS.

### Task 3: Add safe free-only document unlocks

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/DocumentUnlockRequest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentAccessService.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/wx/WxDocumentController.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentAccessServiceTest.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/PointAndDocumentControllerTest.java`

- [ ] **Step 1: Write failing service tests for free-only behavior**

Add separate tests proving:

```java
@Test
void zeroPointDocumentUnlocksWithoutPointRecord()
{
    WlDocument document = document();
    document.setPointPrice(0L);
    // arrange enabled and locked user, no existing unlock, and successful insert

    DocumentUnlockResult result = service.unlock(11L, 22L, "request-free", true);

    assertEquals(0L, result.getSpentPoints());
    verify(pointService, never()).deductAfterLock(any(), any(), any(), any(), any());
}
```

```java
@Test
void freeOnlyRequestNeverChargesExpiredMember()
{
    WlDocument document = document();
    document.setAccessType("VIP_FREE");
    // arrange a user without an active vip expiration

    ServiceException exception = assertThrows(ServiceException.class,
            () -> service.unlock(11L, 22L, "request-expired", true));

    assertEquals("当前文档不再满足免费获取条件，请刷新后重试", exception.getMessage());
    verify(pointService, never()).deductAfterLock(any(), any(), any(), any(), any());
    verify(unlockMapper, never()).insertUnlock(any());
}
```

Retain the existing non-VIP ordinary unlock test to prove `freeOnly = false` still deducts points.

- [ ] **Step 2: Write a failing controller propagation test**

Post `{"requestId":"request-free","freeOnly":true}` and verify:

```java
verify(documentAccessService).unlock(11L, 22L, "request-free", true);
```

Also keep the existing request without `freeOnly` and verify it passes `false`.

- [ ] **Step 3: Run service and controller tests and verify RED**

Run IntelliJ Maven from the repository root with:

```text
-pl ruoyi-admin -am -DskipTests=false -Dtest=DocumentAccessServiceTest,PointAndDocumentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the DTO field and four-argument service method do not exist.

- [ ] **Step 4: Implement DTO and controller propagation**

Add `Boolean freeOnly` plus `isFreeOnly()` that returns `Boolean.TRUE.equals(freeOnly)` to `DocumentUnlockRequest`. Update the controller to pass `request != null && request.isFreeOnly()` and keep the existing Chinese API comment.

- [ ] **Step 5: Implement free-only service behavior**

Keep the three-argument `unlock` method as a compatibility delegate to `unlock(userId, documentId, requestId, false)`. In the four-argument method, after locking the user and rechecking idempotency:

```java
long price = document.getPointPrice() == null ? 0L : document.getPointPrice();
boolean free = price == 0L || isVipFreeForActiveVip(document, lockedUser);
if (free) return insertFreeUnlock(userId, documentId, lockedUser);
if (freeOnly) throw new ServiceException("当前文档不再满足免费获取条件，请刷新后重试");
```

Only the remaining ordinary path may call `pointService.deductAfterLock`.

- [ ] **Step 6: Re-run focused tests and verify GREEN**

Expected: `DocumentAccessServiceTest` and `PointAndDocumentControllerTest` PASS.

### Task 4: Add homepage-only uppercase file type badges

**Files:**
- Modify: `miniprogram/components/document-row/index.js`
- Modify: `miniprogram/components/document-row/index.wxml`
- Modify: `miniprogram/components/document-row/index.wxss`
- Modify: `miniprogram/pages/index/index.wxml`
- Modify: `miniprogram/tests/document-access.test.js`

- [ ] **Step 1: Add failing mini-program contract assertions**

Assert the homepage passes `show-file-type="{{true}}"`, the shared component defaults the property to `false`, normalizes `fileFormat` with `.toUpperCase()`, and renders the badge before the title. Assert WXSS includes red background, white color, and border radius for `.row__file-type`.

- [ ] **Step 2: Run the focused Node test and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/document-access.test.js
```

Expected: FAIL because the property, normalized type, markup, and style do not exist.

- [ ] **Step 3: Implement the component property and normalized value**

Add `showFileType: { type: Boolean, value: false }`, a `fileType` data field, and a `document` property observer that stores `(document.fileFormat || '').trim().toUpperCase()`.

- [ ] **Step 4: Implement markup and styling**

Wrap the badge and title in `.row__title-line`; conditionally render `.row__file-type` before `.row__title`. Use red background, white text, a small radius, and `flex: none`. Add `min-width: 0` to the title line/title so long titles remain contained. Enable the property only in `pages/index/index.wxml`.

- [ ] **Step 5: Re-run the focused Node test and verify GREEN**

Expected: `document-access.test.js` PASS.

### Task 5: Send free documents without a redemption prompt

**Files:**
- Modify: `miniprogram/services/document.js`
- Modify: `miniprogram/pages/document-detail/document-detail.js`
- Modify: `miniprogram/pages/document-detail/document-detail.wxml`
- Modify: `miniprogram/tests/document-access.test.js`

- [ ] **Step 1: Add failing tests for button and request behavior**

Add contract assertions that:

- `documents.unlock(id, requestId, { freeOnly: true })` sends `freeOnly: true`.
- detail data contains `canSendOriginal`.
- the state is true for `unlocked`, zero-point documents, and active VIP `VIP_FREE` documents.
- the send button binds to a method that calls free-only unlock when not already unlocked.
- the ordinary `unlock()` method still shows the existing confirmation modal.

- [ ] **Step 2: Run the focused Node test and verify RED**

Expected: FAIL on the missing free-only payload and detail state.

- [ ] **Step 3: Extend the mini-program document service**

Change `unlock` to accept an optional options object and send:

```javascript
const unlock = (id, requestId, options = {}) => request({
  url: `/wx/documents/${id}/unlock`,
  method: 'POST',
  data: { requestId, freeOnly: options.freeOnly === true }
});
```

- [ ] **Step 4: Compute and refresh direct-send state**

Add a focused `canSendOriginal(document, unlocked, vipActive)` helper on the page. Recompute it after document metadata loads, after unlocked/VIP state loads, and after a successful unlock.

- [ ] **Step 5: Add automatic free unlock before existing send flow**

Bind the direct-send button to `shareAvailableOriginal`. Require login first. If already unlocked, call the existing `shareOriginal`; otherwise call `documents.unlock` with a unique request ID and `{ freeOnly: true }`, set `unlocked`/`canSendOriginal`, then call `shareOriginal`. Do not show an unlock confirmation modal on this path.

- [ ] **Step 6: Update WXML conditions**

Show the ordinary redemption button only when `!canSendOriginal`; show “发送原文件” when `canSendOriginal`. Preserve the existing loading state and disclaimer dialog.

- [ ] **Step 7: Re-run the focused Node test and verify GREEN**

Expected: `document-access.test.js` PASS.

### Task 6: Perform one non-blocking silent login check on homepage load

**Files:**
- Modify: `miniprogram/pages/index/index.js`
- Modify: `miniprogram/tests/silent-login.test.js`

- [ ] **Step 1: Add failing homepage silent-login assertions**

Assert the index page imports `auth` and `session`, invokes a dedicated `checkLogin()` exactly from `onLoad`, returns immediately when `session.getToken()` exists, and otherwise calls `auth.silentLogin().catch(() => {})`. Assert `onShow` does not call it.

- [ ] **Step 2: Run the focused Node test and verify RED**

Run the bundled Node executable with `--test miniprogram/tests/silent-login.test.js`.

Expected: FAIL because homepage has no login check.

- [ ] **Step 3: Implement minimal non-blocking homepage check**

Import `auth` and `session`. In `onLoad`, call `this.load()` and `this.checkLogin()`. Implement:

```javascript
checkLogin() {
  if (session.getToken()) return Promise.resolve();
  return auth.silentLogin().catch(() => {});
}
```

Do not add login UI state or a toast to homepage.

- [ ] **Step 4: Re-run the focused Node test and verify GREEN**

Expected: `silent-login.test.js` PASS.

### Task 7: Focused regression verification

**Files:**
- Verify only; do not modify unrelated files or sensitive configuration.

- [ ] **Step 1: Run all relevant Java tests together**

```powershell
$env:JAVA_HOME='E:\JDK8'; & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl ruoyi-admin -am -DskipTests=false -Dtest=DocumentCoverUrlServiceTest,HomeQueryServiceTest,DocumentAccessServiceTest,PointAndDocumentControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: BUILD SUCCESS and all named tests PASS.

- [ ] **Step 2: Run relevant mini-program tests together**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/document-access.test.js miniprogram/tests/silent-login.test.js miniprogram/tests/document-thumbnail-display.test.js
```

Expected: all tests PASS.

- [ ] **Step 3: Check formatting and protected workspace state**

Run `git diff --check`, inspect `git diff --` for every task file, and verify no `.env`, `application.yml`, `application-druid.yml`, build output, database migration, or unrelated file is included.

Because this dirty worktree already contains user changes in overlapping mini-program files, do not stage or commit implementation files unless the user explicitly asks for a code commit.
