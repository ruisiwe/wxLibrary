# Spring Service Constructor Injection Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Spring 正确创建三个含测试专用重载构造器的 Service，消除应用启动时寻找无参构造器的异常。

**Architecture:** 保留生产构造器委托测试构造器的现有结构，只用 `@Autowired` 明确唯一的 Spring 注入入口。使用轻量 `AnnotationConfigApplicationContext` 和模拟协作者验证真实 Spring 构造器选择，不加载数据库或应用敏感配置。

**Tech Stack:** Java 8、Spring Boot 2.5.15、Spring Framework 5.3.39、JUnit 5、Mockito、Maven

---

### Task 1: 增加 Spring 构造器选择回归测试

**Files:**
- Create: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/SpringServiceConstructorInjectionTest.java`

- [ ] **Step 1: 写入失败测试**

```java
package com.ruoyi.library.service;

import com.ruoyi.library.mapper.WlCourseMapper;
import com.ruoyi.library.mapper.WlPointMapper;
import com.ruoyi.library.mapper.WlPointRecordMapper;
import com.ruoyi.library.mapper.WlUserCourseMapper;
import com.ruoyi.library.mapper.WlVideoProgressMapper;
import com.ruoyi.library.mapper.WlVipEntitlementMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SpringServiceConstructorInjectionTest
{
    @Test
    void createsPointServiceThroughItsProductionConstructor()
    {
        assertServiceCanBeCreated(PointService.class,
                mock(WlPointMapper.class), mock(WlPointRecordMapper.class), mock(WlWxUserMapper.class));
    }

    @Test
    void createsVipEntitlementServiceThroughItsProductionConstructor()
    {
        assertServiceCanBeCreated(VipEntitlementService.class,
                mock(WlVipEntitlementMapper.class), mock(WlWxUserMapper.class), mock(PointService.class));
    }

    @Test
    void createsVideoPlaybackServiceThroughItsProductionConstructor()
    {
        assertServiceCanBeCreated(VideoPlaybackService.class,
                mock(WlCourseMapper.class), mock(WlUserCourseMapper.class), mock(WlWxUserMapper.class),
                mock(WlVideoProgressMapper.class), mock(PrivateFileUrlSigner.class));
    }

    private void assertServiceCanBeCreated(Class<?> serviceClass, Object... dependencies)
    {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
        {
            for (int index = 0; index < dependencies.length; index++)
            {
                context.getBeanFactory().registerSingleton("dependency" + index, dependencies[index]);
            }
            context.register(serviceClass);
            context.refresh();
            assertNotNull(context.getBean(serviceClass));
        }
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path='E:\JDK8\bin;' + $env:Path
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-wechat-library' '-am' '-Dtest=SpringServiceConstructorInjectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 三个测试因对应 Service 没有可选择的注入构造器而报 `No default constructor found`。

### Task 2: 明确三个 Service 的生产构造器

**Files:**
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/PointService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VipEntitlementService.java`
- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/VideoPlaybackService.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/SpringServiceConstructorInjectionTest.java`

- [ ] **Step 1: 给 PointService 生产构造器添加注解**

```java
import org.springframework.beans.factory.annotation.Autowired;

@Autowired
public PointService(WlPointMapper pointMapper, WlPointRecordMapper recordMapper,
        WlWxUserMapper userMapper)
```

- [ ] **Step 2: 给 VipEntitlementService 生产构造器添加注解**

```java
import org.springframework.beans.factory.annotation.Autowired;

@Autowired
public VipEntitlementService(WlVipEntitlementMapper entitlementMapper, WlWxUserMapper userMapper,
        PointService pointService)
```

- [ ] **Step 3: 给 VideoPlaybackService 生产构造器添加注解**

```java
import org.springframework.beans.factory.annotation.Autowired;

@Autowired
public VideoPlaybackService(WlCourseMapper c, WlUserCourseMapper g, WlWxUserMapper u,
        WlVideoProgressMapper p, PrivateFileUrlSigner s)
```

- [ ] **Step 4: 重新运行回归测试并确认 GREEN**

Run:

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path='E:\JDK8\bin;' + $env:Path
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' '-pl' 'ruoyi-wechat-library' '-am' '-Dtest=SpringServiceConstructorInjectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: `Tests run: 3, Failures: 0, Errors: 0` 和 `BUILD SUCCESS`。

### Task 3: 全量验证

**Files:**
- Verify: all Java reactor modules

- [ ] **Step 1: 运行 Java 全量测试**

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path='E:\JDK8\bin;' + $env:Path
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' clean test
```

Expected: 八个 Reactor 模块均为 `SUCCESS`，最终输出 `BUILD SUCCESS`。

- [ ] **Step 2: 检查差异和格式**

```powershell
git diff --check
git status --short
git diff -- ruoyi-wechat-library/src/main/java ruoyi-wechat-library/src/test/java
```

Expected: `git diff --check` 无输出；差异只包含三个构造器注解、一个回归测试及已批准的设计/计划文档，不包含构建产物或敏感配置。
