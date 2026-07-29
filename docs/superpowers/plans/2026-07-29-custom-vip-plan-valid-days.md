# VIP 套餐自定义有效天数 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将会员套餐有效天数从固定 30/365 天改为可配置的 1～3650 天整数，并在套餐维护和会员开通两层统一校验。

**Architecture:** 数据库字段和订单快照结构保持不变。前端通过通用 `SimpleList` 数字控件输入有效天数，套餐服务负责写入前校验，会员权益服务负责使用前的防御性校验。

**Tech Stack:** Java 8、Spring Boot、JUnit 5、Mockito、Vue 2、Element UI、Node.js、Vue Template Compiler。

---

### Task 1：套餐维护服务支持自定义天数

**Files:**
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipPlanServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipPlanService.java`

- [ ] **Step 1：编写套餐有效天数范围测试**

新增 `VipPlanServiceTest`，通过公开的 `add` 方法验证私有校验逻辑：

```java
package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.mapper.WlVipPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VipPlanServiceTest
{
    private WlVipPlanMapper planMapper;
    private VipPlanService service;

    @BeforeEach
    void setUp()
    {
        planMapper = mock(WlVipPlanMapper.class);
        service = new VipPlanService(planMapper);
    }

    @Test
    void addAcceptsCustomAndBoundaryValidDays()
    {
        WlVipPlan oneDay = plan(1);
        WlVipPlan custom = plan(90);
        WlVipPlan tenYears = plan(3650);

        service.add(oneDay, "admin");
        service.add(custom, "admin");
        service.add(tenYears, "admin");

        verify(planMapper).insertPlan(oneDay);
        verify(planMapper).insertPlan(custom);
        verify(planMapper).insertPlan(tenYears);
    }

    @Test
    void addRejectsValidDaysOutsideConfiguredRange()
    {
        assertValidDaysMessage(plan(null));
        assertValidDaysMessage(plan(0));
        assertValidDaysMessage(plan(-1));
        assertValidDaysMessage(plan(3651));
    }

    private void assertValidDaysMessage(WlVipPlan plan)
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.add(plan, "admin"));
        assertEquals("会员套餐有效天数必须在1到3650天之间", exception.getMessage());
    }

    private WlVipPlan plan(Integer validDays)
    {
        WlVipPlan plan = new WlVipPlan();
        plan.setPlanCode("PLAN_" + validDays);
        plan.setPlanName("测试套餐");
        plan.setPriceCent(990L);
        plan.setValidDays(validDays);
        plan.setGiftPoints(0L);
        plan.setSortOrder(0);
        plan.setStatus("0");
        return plan;
    }
}
```

- [ ] **Step 2：运行测试并确认旧实现拒绝 90 天**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  '-Dtest=VipPlanServiceTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

预期：`addAcceptsCustomAndBoundaryValidDays` 因旧实现只允许 30/365 天而失败。

- [ ] **Step 3：实现套餐维护范围校验**

在 `VipPlanService` 中增加：

```java
private static final int MAX_VALID_DAYS = 3650;
```

把两段旧校验替换为：

```java
if (plan.getValidDays() == null || plan.getValidDays() < 1
        || plan.getValidDays() > MAX_VALID_DAYS)
    throw new ServiceException("会员套餐有效天数必须在1到3650天之间");
```

- [ ] **Step 4：复跑套餐服务测试**

重复 Step 2 命令，预期全部通过。

### Task 2：会员开通支持自定义套餐天数

**Files:**
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipEntitlementServiceTest.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipEntitlementService.java`

- [ ] **Step 1：增加自定义天数及越界测试**

在 `VipEntitlementServiceTest` 中导入：

```java
import com.ruoyi.common.exception.ServiceException;
import static org.junit.jupiter.api.Assertions.assertThrows;
```

新增：

```java
@Test
void customPlanExtendsMembershipByConfiguredDays()
{
    service.openOrRenew(1L, plan(90, 0L), "PAYMENT", "custom-90");

    assertEquals("2026-10-14T00:00:00Z", user.getVipExpireTime().toInstant().toString());
}

@Test
void planValidDaysOutsideConfiguredRangeAreRejected()
{
    assertPlanDaysMessage(plan(0, 0L));
    assertPlanDaysMessage(plan(3651, 0L));
}

private void assertPlanDaysMessage(WlVipPlan plan)
{
    ServiceException exception = assertThrows(ServiceException.class,
            () -> service.openOrRenew(1L, plan, "PAYMENT", "invalid-" + plan.getValidDays()));
    assertEquals("会员套餐有效天数必须在1到3650天之间", exception.getMessage());
}
```

将测试辅助方法中的套餐编码和名称改为支持任意天数：

```java
plan.setPlanCode("PLAN_" + validDays);
plan.setPlanName(validDays + "天套餐");
```

- [ ] **Step 2：运行权益服务测试并确认 90 天被旧实现拒绝**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  '-Dtest=VipEntitlementServiceTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

预期：自定义 90 天用例失败，错误为旧的“只能为30天或365天”。

- [ ] **Step 3：实现会员开通范围校验**

在 `VipEntitlementService` 中增加：

```java
private static final int MAX_PLAN_VALID_DAYS = 3650;
```

把 `validatePlan` 中旧的有效天数校验替换为：

```java
if (plan.getValidDays() == null || plan.getValidDays() < 1
        || plan.getValidDays() > MAX_PLAN_VALID_DAYS)
    throw new ServiceException("会员套餐有效天数必须在1到3650天之间");
```

不要修改 `validateGrant`，避免改变会员补偿业务的既有规则。

- [ ] **Step 4：运行两组后端测试**

```powershell
$env:JAVA_HOME='E:\JDK8'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  '-Dtest=VipPlanServiceTest,VipEntitlementServiceTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

预期：全部通过。

- [ ] **Step 5：提交后端改动**

```powershell
git add -- `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipPlanService.java `
  ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipEntitlementService.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipPlanServiceTest.java `
  ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/VipEntitlementServiceTest.java
git commit -m "feat: support custom VIP plan valid days"
```

### Task 3：会员套餐页面改为整数天数输入

**Files:**
- Create: `ruoyi-ui/tests/vip-plan-valid-days.test.js`
- Modify: `ruoyi-ui/src/views/library/common/SimpleList.vue`
- Modify: `ruoyi-ui/src/views/library/vip/plan/index.vue`

注意：`vip/plan/index.vue` 已包含用户现有的价格“分转元”未提交改动。实现时保留这些内容，提交时仅暂存本任务的有效天数差异，不覆盖或夹带价格改动。

- [ ] **Step 1：新增页面契约测试**

```javascript
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const planPath = path.join(root, 'src/views/library/vip/plan/index.vue')
const simpleListPath = path.join(root, 'src/views/library/common/SimpleList.vue')
const planPage = fs.readFileSync(planPath, 'utf8')
const simpleList = fs.readFileSync(simpleListPath, 'utf8')

assert(
  /\{prop:'validDays',label:'有效天数（天）',type:'number',required:true,min:1,max:3650,precision:0\}/.test(planPage),
  '会员套餐有效天数应配置为1到3650天的整数输入'
)
assert(!/prop:'validDays'[^}]+type:'select'/.test(planPage),
  '会员套餐有效天数不应继续使用固定下拉选项')
assert(simpleList.includes(':precision="field.precision"'),
  '通用数字控件应支持字段级整数精度')

const component = compiler.parseComponent(planPage)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [],
  `会员套餐页面模板编译失败：${compiled.errors.join('；')}`)

console.log('VIP套餐自定义有效天数契约测试通过')
```

- [ ] **Step 2：运行契约测试并确认旧页面失败**

```powershell
node ruoyi-ui/tests/vip-plan-valid-days.test.js
```

预期：旧页面仍配置 `type:'select'`，测试失败。

- [ ] **Step 3：让通用数字控件支持精度**

将 `SimpleList.vue` 数字控件修改为：

```vue
<el-input-number
  v-if="field.type === 'number'"
  v-model="form[field.prop]"
  :min="field.min === undefined ? 0 : field.min"
  :max="field.max"
  :precision="field.precision"
  controls-position="right"
/>
```

未配置 `precision` 的其他数字字段保持原行为。

- [ ] **Step 4：修改套餐有效天数字段**

在 `vip/plan/index.vue` 的 `fields` 中将有效天数配置替换为：

```javascript
{prop:'validDays',label:'有效天数（天）',type:'number',required:true,min:1,max:3650,precision:0}
```

保留默认值 `validDays:30`，删除固定月卡、年卡选项。

- [ ] **Step 5：运行前端契约与风格回归**

```powershell
node ruoyi-ui/tests/vip-plan-valid-days.test.js
node ruoyi-ui/tests/library-simple-list-style.test.js
```

预期：全部通过。

- [ ] **Step 6：运行生产构建**

工作目录：`ruoyi-ui`

```powershell
npm run build:prod
```

预期：构建成功；`dist` 不暂存、不提交。

- [ ] **Step 7：选择性提交前端改动**

先暂存干净文件：

```powershell
git add -- ruoyi-ui/src/views/library/common/SimpleList.vue ruoyi-ui/tests/vip-plan-valid-days.test.js
```

对 `ruoyi-ui/src/views/library/vip/plan/index.vue` 只暂存“有效天数”字段配置，不暂存用户原有的价格“分转元”差异。提交前必须使用：

```powershell
git diff --cached -- ruoyi-ui/src/views/library/vip/plan/index.vue
```

确认缓存区只包含 `validDays` 从固定下拉改为 1～3650 整数输入后，再提交：

```powershell
git commit -m "feat: allow custom VIP plan valid days"
```

### Task 4：最终验证

**Files:**
- Verify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipPlanService.java`
- Verify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipEntitlementService.java`
- Verify: `ruoyi-ui/src/views/library/common/SimpleList.vue`
- Verify: `ruoyi-ui/src/views/library/vip/plan/index.vue`

- [ ] **Step 1：运行后端定向测试**

运行 Task 2 Step 4 的 Maven 命令，确认无失败。

- [ ] **Step 2：运行前端定向测试**

```powershell
node ruoyi-ui/tests/vip-plan-valid-days.test.js
node ruoyi-ui/tests/library-simple-list-style.test.js
```

- [ ] **Step 3：检查范围和工作区**

```powershell
git diff --check HEAD
git status --short
git log -3 --oneline
```

确认：

- 本任务没有修改数据库或敏感配置。
- `ruoyi-ui/dist` 未进入提交。
- `vip/plan/index.vue` 中用户原有价格“分转元”改动仍保留在工作区，没有被本任务提交。
- 其他既有未提交文件未被覆盖。

