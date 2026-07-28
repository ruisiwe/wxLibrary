# Automatic Manual Point Business Number Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the business-number input from manual point adjustments and generate an idempotent per-user business number from a hidden client batch identifier.

**Architecture:** The management page creates one 20-character batch identifier when the adjustment dialog opens and retains it across failed submissions. The backend validates that identifier, combines it with the target WeChat user ID as `MANUAL_POINT:<batchNo>:<userId>`, and reuses the existing transaction, row lock, balance snapshot, and idempotency checks.

**Tech Stack:** Java 8, Spring Boot 2.5, Spring transactions, JUnit 5, Mockito, Vue 2, Element UI, Node.js contract tests.

---

## File Structure

### New file

- `ruoyi-ui/tests/manual-point-business-number.test.js`
  - Prevents management forms from requesting a business number and verifies the hidden batch contract.

### Modified files

- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/PointAdjustmentRequest.java`
  - Replaces the externally supplied `bizNo` with a hidden `batchNo`.
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/LibraryWxUserService.java`
  - Validates the request, generates the final business number, and retains transactional idempotency.
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/LibraryWxUserServiceTest.java`
  - Covers automatic numbering, validation, retries, conflicts, locking, and balance safety.
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryWxUserController.java`
  - Updates the Simplified Chinese API comment to describe system-generated operation numbers.
- `ruoyi-ui/src/views/library/user/index.vue`
  - Removes the business-number field and manages the hidden batch identifier and submit loading state.

No database file or SQL migration is required.

---

### Task 1: Change the Backend Request Contract and Automatic Numbering

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/LibraryWxUserServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/PointAdjustmentRequest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/LibraryWxUserService.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryWxUserController.java`

- [ ] **Step 1: Rewrite the service tests against the new request contract**

Change the test request helper to:

```java
private PointAdjustmentRequest request(Long amount, String batchNo)
{
    PointAdjustmentRequest request = new PointAdjustmentRequest();
    request.setAmount(amount);
    request.setBatchNo(batchNo);
    request.setDescription("人工调整");
    return request;
}

private void assertMessage(String expected, Runnable action)
{
    assertEquals(expected,
            assertThrows(ServiceException.class, action::run).getMessage());
}
```

Use 20-character alphanumeric batch values such as
`A1B2C3D4E5F6G7H8I9J0`, and assert the inserted record:

```java
assertEquals("MANUAL_POINT:A1B2C3D4E5F6G7H8I9J0:7",
        captor.getValue().getBizNo());
assertEquals("MANUAL", captor.getValue().getEventType());
assertEquals("人工调整", captor.getValue().getDescription());
```

Add validation tests:

```java
@Test
void invalidRequestFieldsAreRejected()
{
    assertMessage("积分调整请求不能为空",
            () -> service.adjustPoints(7L, null, "admin"));
    assertMessage("微信用户编号不正确",
            () -> service.adjustPoints(0L,
                    request(1L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));
    assertMessage("积分调整数量不能为0",
            () -> service.adjustPoints(7L,
                    request(0L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));
    assertMessage("积分调整批次编号不正确",
            () -> service.adjustPoints(7L, request(1L, "short"), "admin"));

    PointAdjustmentRequest blankReason =
            request(1L, "A1B2C3D4E5F6G7H8I9J0");
    blankReason.setDescription(" ");
    assertMessage("积分调整原因不能为空",
            () -> service.adjustPoints(7L, blankReason, "admin"));

    PointAdjustmentRequest longReason =
            request(1L, "A1B2C3D4E5F6G7H8I9J0");
    longReason.setDescription(String.join("",
            java.util.Collections.nCopies(201, "原")));
    assertMessage("积分调整原因不能超过200个字符",
            () -> service.adjustPoints(7L, longReason, "admin"));
}
```

Add an idempotency test in which `selectByBizNo` returns an existing
`MANUAL` record with the same user and change amount. Add a conflict test
where the existing record has another user, amount, or event type, and assert
`积分调整业务编号已被其他操作使用`.

- [ ] **Step 2: Run the backend test and verify RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  '-Dtest=LibraryWxUserServiceTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: test compilation fails because `PointAdjustmentRequest` does not
have `setBatchNo`, or assertions fail because the service still requires an
external business number.

- [ ] **Step 3: Replace the DTO field**

Implement `PointAdjustmentRequest` as:

```java
/** 管理员人工调整积分请求。 */
public class PointAdjustmentRequest
{
    private Long amount;
    private String batchNo;
    private String description;

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

- [ ] **Step 4: Generate and validate the backend business number**

At the start of `LibraryWxUserService.adjustPoints`, validate in this order:

```java
if (request == null) throw new ServiceException("积分调整请求不能为空");
if (userId == null || userId <= 0) throw new ServiceException("微信用户编号不正确");
if (request.getAmount() == null || request.getAmount() == 0)
    throw new ServiceException("积分调整数量不能为0");
if (request.getBatchNo() == null
        || !request.getBatchNo().matches("[A-Za-z0-9]{20}"))
    throw new ServiceException("积分调整批次编号不正确");
if (request.getDescription() == null
        || request.getDescription().trim().isEmpty())
    throw new ServiceException("积分调整原因不能为空");
String description = request.getDescription().trim();
if (description.length() > 200)
    throw new ServiceException("积分调整原因不能超过200个字符");
String bizNo = "MANUAL_POINT:" + request.getBatchNo() + ":" + userId;
```

Replace every `request.getBizNo().trim()` lookup and assignment with `bizNo`.
Extract the repeated existing-record validation:

```java
private WlPointRecord requireMatchingAdjustment(WlPointRecord existing,
        Long userId, Long amount)
{
    if (!userId.equals(existing.getUserId())
            || !amount.equals(existing.getChangePoints())
            || !"MANUAL".equals(existing.getEventType()))
        throw new ServiceException("积分调整业务编号已被其他操作使用");
    return existing;
}
```

Set `record.setDescription(description)`. Retain the transaction annotation,
the pre-lock lookup, user row lock, post-lock lookup, exact arithmetic,
nonnegative balance rule, snapshot fields, and operator.

- [ ] **Step 5: Update the API comment**

Change the controller comment to:

```java
/** 人工调整积分，操作业务编号由系统生成并用于幂等控制。 */
```

- [ ] **Step 6: Run the backend test and verify GREEN**

Run the Step 2 command.

Expected: all `LibraryWxUserServiceTest` methods pass.

- [ ] **Step 7: Commit the backend contract**

Stage only:

```powershell
git add -- `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/PointAdjustmentRequest.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/LibraryWxUserService.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/LibraryWxUserServiceTest.java `
  ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryWxUserController.java
git commit -m "feat: generate manual point business numbers"
```

---

### Task 2: Remove the Management Form Input

**Files:**
- Create: `ruoyi-ui/tests/manual-point-business-number.test.js`
- Modify: `ruoyi-ui/src/views/library/user/index.vue`

- [ ] **Step 1: Add the failing management-page contract test**

Create:

```javascript
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const userPage = fs.readFileSync(
  path.join(root, 'src/views/library/user/index.vue'),
  'utf8'
)
const viewRoot = path.join(root, 'src/views')
const vueFiles = []

function collect(directory) {
  fs.readdirSync(directory, { withFileTypes: true }).forEach(entry => {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) collect(target)
    else if (entry.isFile() && entry.name.endsWith('.vue')) vueFiles.push(target)
  })
}

collect(viewRoot)
vueFiles.forEach(file => {
  const source = fs.readFileSync(file, 'utf8')
  assert(!/<el-form-item[^>]+label=["'][^"']*业务(?:编号|编码)[^"']*["']/.test(source),
    `管理表单不得要求人工填写业务编号：${path.relative(root, file)}`)
})

assert(userPage.includes('batchNo'), '积分调整请求应提交隐藏批次标识')
assert(!userPage.includes('form.bizNo'), '积分调整页面不得保留人工业务编号字段')
assert(userPage.includes('createBatchNo'), '打开积分调整弹窗时应生成批次标识')
assert(userPage.includes(':loading="submitting"'), '提交期间应锁定确认按钮')
assert(userPage.includes('系统会自动记录本次操作编号'),
  '积分调整提示应说明操作编号由系统记录')

const component = compiler.parseComponent(userPage)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [],
  `微信用户页面模板编译失败：${compiled.errors.join('；')}`)

console.log('人工积分业务编号自动生成契约测试通过')
```

This pattern checks only form items. Read-only business-number columns remain
allowed.

- [ ] **Step 2: Run the contract test and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  ruoyi-ui/tests/manual-point-business-number.test.js
```

Expected: failure reports the existing business-number form item.

- [ ] **Step 3: Update the dialog and component state**

In `ruoyi-ui/src/views/library/user/index.vue`:

- Remove the business-number `el-form-item`.
- Change the warning to:

```vue
<el-alert
  title="扣减积分不能使余额小于 0，系统会自动记录本次操作编号。"
  type="warning"
  :closable="false"
/>
```

- Add `submitting: false` to component state.
- Replace `openPoints` with:

```javascript
openPoints(row) {
  this.selected = row
  this.form = {
    amount: 0,
    batchNo: this.createBatchNo(),
    description: ''
  }
  this.visible = true
}
```

- Add:

```javascript
createBatchNo() {
  const value = `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}00000000000000000000`
  return value.replace(/[^a-z0-9]/gi, '').slice(0, 20)
}
```

- Replace `submitPoints` with validation that requires only amount and
  description. Set `submitting` before the request and clear it in `finally`:

```javascript
submitPoints() {
  if (!this.form.amount || !this.form.description
      || !this.form.description.trim()) {
    return this.$modal.msgError('请完整填写调整数量和调整原因')
  }
  this.$modal.confirm(`确认将用户积分调整 ${this.form.amount} 吗？`)
    .then(() => {
      this.submitting = true
      return adjustUserPoints(this.selected.id, this.form)
    })
    .then(() => {
      this.$modal.msgSuccess('积分调整成功')
      this.visible = false
      this.load()
    })
    .finally(() => {
      this.submitting = false
    })
}
```

Set the confirm button to:

```vue
<el-button type="primary" :loading="submitting" @click="submitPoints">
  确认调整
</el-button>
```

Do not regenerate `batchNo` in error handling or `finally`.

- [ ] **Step 4: Run the contract test and verify GREEN**

Run the Step 2 command.

Expected: `人工积分业务编号自动生成契约测试通过`.

- [ ] **Step 5: Run the Vue production build**

Run:

```powershell
npm run build:prod
```

Working directory: `ruoyi-ui`.

Expected: build succeeds. Existing asset-size warnings are acceptable; new
template or JavaScript errors are not.

- [ ] **Step 6: Commit the management page**

Stage only:

```powershell
git add -- `
  ruoyi-ui/src/views/library/user/index.vue `
  ruoyi-ui/tests/manual-point-business-number.test.js
git commit -m "feat: hide manual point business number input"
```

---

### Task 3: Cross-Feature Verification

**Files:**
- Verify only; do not stage `target`, `dist`, environment files, or unrelated user changes.

- [ ] **Step 1: Run focused Java tests**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library,ruoyi-admin -am `
  '-Dtest=LibraryWxUserServiceTest,PointServiceTest,LibraryContentControllerTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: all specified tests pass.

- [ ] **Step 2: Run all management UI contract tests**

Run each `ruoyi-ui/tests/*.test.js` with the bundled Node runtime. The new
manual-point test and the existing VIP batch test must pass. Record the
already-known banner `112:55` versus current `952:550` contract failure
separately without changing banner behavior.

- [ ] **Step 3: Verify the production build**

Run `npm run build:prod` from `ruoyi-ui`.

Expected: build succeeds with no new errors.

- [ ] **Step 4: Check request and UI boundaries**

Run:

```powershell
rg -n -i 'getBizNo|setBizNo|form\.bizNo|label=["'']业务(?:编号|编码)' ruoyi-ui/src/views/library/user/index.vue ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/PointAdjustmentRequest.java ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/LibraryWxUserService.java
```

Expected: no external/manual business-number field remains in these three
files. Internal `WlPointRecord.bizNo`, mapper fields, generated workflows, and
read-only table columns remain unchanged.

- [ ] **Step 5: Check repository hygiene**

Run:

```powershell
git diff --check
git status --short
git status --ignored --short ruoyi-ui/dist
```

Confirm:

- no `.env`, `application.yml`, or `application-druid.yml` was read or staged;
- no `target` or `ruoyi-ui/dist` output is staged;
- the pre-existing mini-program, VIP plan, and `DocumentService` changes remain
  outside task commits;
- no database migration or historical-data operation was performed.
