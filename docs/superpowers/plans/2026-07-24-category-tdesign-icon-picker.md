# Category TDesign Icon Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让后台管理员通过真实 TDesign 图标预览选择分类图标，只保存图标名称，并让小程序按同一名称渲染。

**Architecture:** 精选图标目录以 JSON 资源作为唯一数据源，由后端目录类读取、管理接口返回并在分类保存时校验。Vue 2 管理端通过 `tdesign-icons-vue` 渲染同名图标，使用专用选择器和 `SimpleList` 通用插槽接入现有分类页面；小程序把分类宫格从图片地址切换为 `t-icon`。Node 契约测试读取同一份 JSON 和小程序本地 `icon.wxss`，防止跨端图标名称失配。

**Tech Stack:** Java 8、Spring Boot 2.5、Fastjson2、JUnit 5、Mockito、RuoYi-Vue、Vue 2.6、Element UI、`tdesign-icons-vue 0.4.2`、`tdesign-miniprogram 1.15.3`、Node.js test runner

---

## File map

### Create

- `ruoyi-wechat-library/src/main/resources/library/category-icons.json`：精选图标唯一数据源。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/CategoryIconOptionDto.java`：管理接口的图标选项 DTO。
- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/category/CategoryIconCatalog.java`：加载目录、提供只读列表、校验名称和缺省值。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/category/CategoryIconCatalogTest.java`：目录加载、顺序、只读性和名称校验测试。
- `ruoyi-ui/src/views/library/content/category/CategoryIconPicker.vue`：后台精选图标搜索选择器。
- `ruoyi-ui/tests/category-icon-picker.test.js`：管理端图标选择契约测试。
- `miniprogram/tests/category-icon.test.js`：小程序渲染和跨端名称契约测试。

### Modify

- `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`：暴露图标选项并校验分类图标。
- `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java`：分类图标保存校验测试。
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryCategoryController.java`：增加图标选项管理接口。
- `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`：接口响应和权限测试。
- `ruoyi-ui/package.json`：锁定 Vue 2 TDesign 图标依赖并增加测试脚本。
- `ruoyi-ui/package-lock.json`：记录精确依赖树。
- `ruoyi-ui/src/api/library/content.js`：增加图标选项请求。
- `ruoyi-ui/src/views/library/common/SimpleList.vue`：增加不绑定具体组件的字段和列作用域插槽。
- `ruoyi-ui/src/views/library/content/category/index.vue`：接入图标选择器和列表预览。
- `miniprogram/components/category-grid/index.json`：注册 `t-icon`。
- `miniprogram/components/category-grid/index.wxml`：按 `item.icon` 渲染 TDesign 图标。
- `miniprogram/components/category-grid/index.wxss`：适配图标组件布局。

---

### Task 1: 建立精选图标唯一目录

**Files:**

- Create: `ruoyi-wechat-library/src/main/resources/library/category-icons.json`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/CategoryIconOptionDto.java`
- Create: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/category/CategoryIconCatalog.java`
- Test: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/category/CategoryIconCatalogTest.java`

- [ ] **Step 1: 写目录加载失败测试**

创建 `CategoryIconCatalogTest.java`：

```java
package com.ruoyi.library.category;

import com.ruoyi.library.dto.CategoryIconOptionDto;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryIconCatalogTest
{
    @Test
    void loadsOrderedCuratedOptionsAndValidatesNames()
    {
        List<CategoryIconOptionDto> options = CategoryIconCatalog.listOptions();

        assertEquals(24, options.size());
        assertEquals("book", options.get(0).getName());
        assertEquals("图书", options.get(0).getLabel());
        assertTrue(CategoryIconCatalog.isSupported("time"));
        assertTrue(CategoryIconCatalog.isSupported(" time "));
        assertFalse(CategoryIconCatalog.isSupported("not-a-tdesign-icon"));
        assertFalse(CategoryIconCatalog.isSupported(null));
        assertEquals("file", CategoryIconCatalog.defaultIcon());
    }

    @Test
    void optionsAreReadOnly()
    {
        assertThrows(UnsupportedOperationException.class,
                () -> CategoryIconCatalog.listOptions().clear());
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

PowerShell：

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-wechat-library -am `
  '-Dtest=CategoryIconCatalogTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: FAIL，提示 `CategoryIconCatalog` 或 `CategoryIconOptionDto` 不存在。

- [ ] **Step 3: 创建精选目录 JSON**

创建 `category-icons.json`：

```json
[
  {"name":"book","label":"图书","keywords":"图书 书籍 文档"},
  {"name":"book-open","label":"打开的书","keywords":"阅读 图书 书籍"},
  {"name":"file","label":"文件","keywords":"文件 文档 资料"},
  {"name":"folder","label":"文件夹","keywords":"文件夹 目录 分类"},
  {"name":"article","label":"文章","keywords":"文章 资讯 文档"},
  {"name":"education","label":"教育","keywords":"教育 学习 学校"},
  {"name":"course","label":"课程","keywords":"课程 培训 学习"},
  {"name":"time","label":"时间","keywords":"时间 时钟 日期"},
  {"name":"video","label":"视频","keywords":"视频 录像 播放"},
  {"name":"chart","label":"图表","keywords":"图表 统计 数据"},
  {"name":"search","label":"搜索","keywords":"搜索 查询 查找"},
  {"name":"home","label":"首页","keywords":"首页 主页"},
  {"name":"image","label":"图片","keywords":"图片 图像 相册"},
  {"name":"music","label":"音频","keywords":"音频 音乐 声音"},
  {"name":"code","label":"代码","keywords":"代码 程序 开发"},
  {"name":"data","label":"数据","keywords":"数据 数据库 资料"},
  {"name":"user","label":"用户","keywords":"用户 人员 个人"},
  {"name":"star","label":"收藏","keywords":"收藏 星标 推荐"},
  {"name":"heart","label":"喜欢","keywords":"喜欢 关注 爱心"},
  {"name":"download","label":"下载","keywords":"下载 获取 文件"},
  {"name":"upload","label":"上传","keywords":"上传 提交 文件"},
  {"name":"link","label":"链接","keywords":"链接 地址 关联"},
  {"name":"lock-on","label":"权限","keywords":"权限 锁定 私有"},
  {"name":"tag","label":"标签","keywords":"标签 分类 标记"}
]
```

- [ ] **Step 4: 创建 DTO**

创建 `CategoryIconOptionDto.java`：

```java
package com.ruoyi.library.dto;

/** 后台分类图标选项。 */
public class CategoryIconOptionDto
{
    private final String name;
    private final String label;
    private final String keywords;

    public CategoryIconOptionDto(String name, String label, String keywords)
    {
        this.name = name;
        this.label = label;
        this.keywords = keywords;
    }

    public String getName() { return name; }
    public String getLabel() { return label; }
    public String getKeywords() { return keywords; }
}
```

- [ ] **Step 5: 实现只读目录**

创建 `CategoryIconCatalog.java`：

```java
package com.ruoyi.library.category;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.library.dto.CategoryIconOptionDto;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StreamUtils;

/** 分类可使用的 TDesign 精选图标目录。 */
public final class CategoryIconCatalog
{
    private static final String RESOURCE = "/library/category-icons.json";
    private static final String DEFAULT_ICON = "file";
    private static final List<CategoryIconOptionDto> OPTIONS = loadOptions();
    private static final Set<String> NAMES = loadNames(OPTIONS);

    private CategoryIconCatalog()
    {
    }

    public static List<CategoryIconOptionDto> listOptions()
    {
        return OPTIONS;
    }

    public static boolean isSupported(String name)
    {
        return name != null && NAMES.contains(name.trim());
    }

    public static String defaultIcon()
    {
        return DEFAULT_ICON;
    }

    private static List<CategoryIconOptionDto> loadOptions()
    {
        try (InputStream input = CategoryIconCatalog.class.getResourceAsStream(RESOURCE))
        {
            if (input == null)
                throw new IllegalStateException("分类图标目录不存在");
            String json = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
            JSONArray values = JSON.parseArray(json);
            List<CategoryIconOptionDto> options = new ArrayList<>(values.size());
            for (Object value : values)
            {
                JSONObject item = (JSONObject) value;
                options.add(new CategoryIconOptionDto(
                        item.getString("name"),
                        item.getString("label"),
                        item.getString("keywords")));
            }
            return Collections.unmodifiableList(options);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("分类图标目录读取失败", exception);
        }
    }

    private static Set<String> loadNames(List<CategoryIconOptionDto> options)
    {
        Set<String> names = new HashSet<>();
        for (CategoryIconOptionDto option : options)
        {
            if (!names.add(option.getName()))
                throw new IllegalStateException("分类图标名称重复：" + option.getName());
        }
        return Collections.unmodifiableSet(names);
    }
}
```

- [ ] **Step 6: 运行目录测试**

重复 Step 2 命令。

Expected: `CategoryIconCatalogTest` 2 tests PASS，Maven `BUILD SUCCESS`。

- [ ] **Step 7: 提交目录实现**

```powershell
git add -- `
  'ruoyi-wechat-library/src/main/resources/library/category-icons.json' `
  'ruoyi-wechat-library/src/main/java/com/ruoyi/library/dto/CategoryIconOptionDto.java' `
  'ruoyi-wechat-library/src/main/java/com/ruoyi/library/category/CategoryIconCatalog.java' `
  'ruoyi-wechat-library/src/test/java/com/ruoyi/library/category/CategoryIconCatalogTest.java'
git commit -m "feat: add curated category icon catalog"
```

---

### Task 2: 增加管理接口和保存校验

**Files:**

- Modify: `ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java`
- Modify: `ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java`
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryCategoryController.java`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java`

- [ ] **Step 1: 写分类图标校验失败测试**

向 `DocumentServiceTest` 增加：

```java
@Test
void categoryRequiresSupportedIcon()
{
    WlCategory missing = new WlCategory();
    missing.setName("质量管理");
    assertEquals("请选择分类图标", assertThrows(ServiceException.class,
            () -> service.addCategory(missing, "admin")).getMessage());

    WlCategory invalid = new WlCategory();
    invalid.setName("质量管理");
    invalid.setIcon("unknown-icon");
    assertEquals("请选择有效的分类图标", assertThrows(ServiceException.class,
            () -> service.addCategory(invalid, "admin")).getMessage());
}

@Test
void categoryTrimsAndStoresSupportedIcon()
{
    WlCategory category = new WlCategory();
    category.setName("质量管理");
    category.setIcon(" time ");
    when(categoryMapper.insertCategory(category)).thenReturn(1);

    assertEquals(1, service.addCategory(category, "admin"));
    assertEquals("time", category.getIcon());
    verify(categoryMapper).insertCategory(category);
}
```

- [ ] **Step 2: 写图标选项接口测试**

向 `LibraryContentControllerTest` 增加导入：

```java
import com.ruoyi.library.dto.CategoryIconOptionDto;
import java.util.Arrays;
```

增加测试：

```java
@Test
void categoryIconOptionsReturnNameLabelAndKeywords() throws Exception
{
    DocumentService documentService = mock(DocumentService.class);
    when(documentService.listCategoryIconOptions()).thenReturn(Arrays.asList(
            new CategoryIconOptionDto("time", "时间", "时间 时钟 日期")));
    MockMvc categoryMockMvc = MockMvcBuilders
            .standaloneSetup(new LibraryCategoryController(documentService)).build();

    categoryMockMvc.perform(get("/library/category/icon-options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("time"))
            .andExpect(jsonPath("$.data[0].label").value("时间"))
            .andExpect(jsonPath("$.data[0].keywords").value("时间 时钟 日期"));
}
```

在 `managementControllersExposeApprovedPermissions()` 中增加：

```java
assertPermission(LibraryCategoryController.class, "iconOptions", "library:category:list");
```

- [ ] **Step 3: 运行测试并确认失败**

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-admin -am `
  '-Dtest=DocumentServiceTest,LibraryContentControllerTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: FAIL，缺少 `listCategoryIconOptions()`、`iconOptions()`，且分类图标尚未校验。

- [ ] **Step 4: 在服务中暴露选项并校验**

向 `DocumentService.java` 增加导入：

```java
import com.ruoyi.library.category.CategoryIconCatalog;
import com.ruoyi.library.dto.CategoryIconOptionDto;
```

在分类查询方法附近增加：

```java
public List<CategoryIconOptionDto> listCategoryIconOptions()
{
    return CategoryIconCatalog.listOptions();
}
```

在 `validateCategory` 中保留“全部分类”判断，随后、在名称重复查询之前加入：

```java
requireText(category.getIcon(), "请选择分类图标");
category.setIcon(category.getIcon().trim());
if (!CategoryIconCatalog.isSupported(category.getIcon()))
    throw new ServiceException("请选择有效的分类图标");
```

完整校验顺序应为：

```java
private void validateCategory(WlCategory category, Long excludeId)
{
    if (category == null) throw new ServiceException("文档分类参数不能为空");
    requireText(category.getName(), "文档分类名称不能为空");
    category.setName(category.getName().trim());
    if ("全部分类".equals(category.getName()))
        throw new ServiceException("全部分类是固定入口，不能作为普通分类保存");
    requireMaxLength(category.getName(), 64, "文档分类名称不能超过64个字符");
    requireText(category.getIcon(), "请选择分类图标");
    category.setIcon(category.getIcon().trim());
    if (!CategoryIconCatalog.isSupported(category.getIcon()))
        throw new ServiceException("请选择有效的分类图标");
    if (categoryMapper.countCategoryName(category.getName(), excludeId) > 0)
        throw new ServiceException("文档分类名称已存在");
}
```

- [ ] **Step 5: 增加管理接口**

向 `LibraryCategoryController.java` 增加：

```java
/** 查询后台可选择的分类图标。 */
@PreAuthorize("@ss.hasPermi('library:category:list')")
@GetMapping("/icon-options")
public AjaxResult iconOptions()
{
    return success(documentService.listCategoryIconOptions());
}
```

- [ ] **Step 6: 运行后端测试**

重复 Step 3 命令。

Expected: `DocumentServiceTest` 和 `LibraryContentControllerTest` PASS，Maven `BUILD SUCCESS`。

- [ ] **Step 7: 提交后端接口和校验**

```powershell
git add -- `
  'ruoyi-wechat-library/src/main/java/com/ruoyi/library/service/DocumentService.java' `
  'ruoyi-wechat-library/src/test/java/com/ruoyi/library/service/DocumentServiceTest.java' `
  'ruoyi-admin/src/main/java/com/ruoyi/web/controller/library/LibraryCategoryController.java' `
  'ruoyi-admin/src/test/java/com/ruoyi/web/controller/library/LibraryContentControllerTest.java'
git commit -m "feat: validate and expose category icons"
```

---

### Task 3: 创建后台 TDesign 图标选择器

**Files:**

- Modify: `ruoyi-ui/package.json`
- Modify: `ruoyi-ui/package-lock.json`
- Modify: `ruoyi-ui/src/api/library/content.js`
- Create: `ruoyi-ui/src/views/library/content/category/CategoryIconPicker.vue`
- Create: `ruoyi-ui/tests/category-icon-picker.test.js`

- [ ] **Step 1: 写管理端选择器契约测试**

创建 `ruoyi-ui/tests/category-icon-picker.test.js`：

```javascript
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const pickerPath = path.join(root, 'src/views/library/content/category/CategoryIconPicker.vue')
const apiPath = path.join(root, 'src/api/library/content.js')
const packagePath = path.join(root, 'package.json')

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

assert(fs.existsSync(pickerPath), '应提供分类 TDesign 图标选择器')
const source = read(pickerPath)
const component = compiler.parseComponent(source)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [], `图标选择器模板编译失败：${compiled.errors.join('；')}`)
assert(source.includes("from 'tdesign-icons-vue'"), '选择器应使用 Vue 2 TDesign 图标包')
assert(source.includes('v-for="item in filteredOptions"'), '选择器应遍历精选图标')
assert(source.includes(':name="item.name"'), '预览图标应使用目录中的 TDesign name')
assert(source.includes("this.$emit('input', item.name)"), '选中后应只回传图标名称')
assert(source.includes('重新加载'), '目录失败时应提供中文重试入口')

const api = read(apiPath)
assert(api.includes('/library/category/icon-options'), '管理端应请求分类图标目录接口')

const pkg = JSON.parse(read(packagePath))
assert.strictEqual(pkg.dependencies['tdesign-icons-vue'], '0.4.2', '应精确锁定 Vue 2 图标版本')

console.log('分类 TDesign 图标选择器契约测试通过')
```

- [ ] **Step 2: 增加测试脚本并确认失败**

向 `ruoyi-ui/package.json` 的 `scripts` 增加：

```json
"test:category-icon": "node tests/category-icon-picker.test.js"
```

运行：

```powershell
npm run test:category-icon
```

Working directory: `ruoyi-ui`

Expected: FAIL，提示选择器文件或依赖不存在。

- [ ] **Step 3: 精确安装 Vue 2 图标依赖**

```powershell
npm install tdesign-icons-vue@0.4.2 --save-exact
```

Working directory: `ruoyi-ui`

Expected: `package.json` 和 `package-lock.json` 记录 `tdesign-icons-vue` 版本 `0.4.2`。

- [ ] **Step 4: 增加图标目录 API**

向 `ruoyi-ui/src/api/library/content.js` 增加：

```javascript
export const listCategoryIconOptions = () => request({
  url: '/library/category/icon-options',
  method: 'get'
})
```

- [ ] **Step 5: 实现图标选择器**

创建 `CategoryIconPicker.vue`：

```vue
<template>
  <div class="category-icon-picker">
    <el-button class="category-icon-picker__trigger" @click="open">
      <icon :name="displayName" size="24px" />
      <span>{{ selectedOption ? selectedOption.label : '请选择图标' }}</span>
      <small>{{ value || '' }}</small>
      <i class="el-icon-arrow-down" />
    </el-button>

    <el-dialog
      title="选择分类图标"
      :visible.sync="visible"
      width="620px"
      append-to-body
    >
      <el-input
        v-model.trim="keyword"
        clearable
        prefix-icon="el-icon-search"
        placeholder="搜索中文名称、图标名称或关键词"
      />
      <div v-loading="loading" class="category-icon-picker__body">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          :closable="false"
          show-icon
        >
          <el-button type="text" @click="loadOptions">重新加载</el-button>
        </el-alert>
        <div v-else class="category-icon-picker__grid">
          <button
            v-for="item in filteredOptions"
            :key="item.name"
            type="button"
            class="category-icon-picker__item"
            :class="{ 'is-selected': item.name === value }"
            @click="select(item)"
          >
            <icon :name="item.name" size="30px" />
            <span>{{ item.label }}</span>
            <small>{{ item.name }}</small>
          </button>
        </div>
        <div v-if="!loading && !error && !filteredOptions.length" class="category-icon-picker__empty">
          没有匹配的图标
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Icon } from 'tdesign-icons-vue'
import { listCategoryIconOptions } from '@/api/library/content'

export default {
  name: 'CategoryIconPicker',
  components: { Icon },
  props: {
    value: { type: String, default: '' }
  },
  data() {
    return {
      visible: false,
      loading: false,
      loaded: false,
      error: '',
      keyword: '',
      options: []
    }
  },
  computed: {
    selectedOption() {
      return this.options.find(item => item.name === this.value) || null
    },
    displayName() {
      return this.selectedOption ? this.selectedOption.name : 'file'
    },
    filteredOptions() {
      const keyword = this.keyword.toLowerCase()
      if (!keyword) return this.options
      return this.options.filter(item => [item.name, item.label, item.keywords]
        .filter(Boolean)
        .some(value => value.toLowerCase().includes(keyword)))
    }
  },
  created() {
    this.loadOptions()
  },
  methods: {
    open() {
      this.visible = true
      if (!this.loaded) this.loadOptions()
    },
    loadOptions() {
      this.loading = true
      this.error = ''
      listCategoryIconOptions().then(response => {
        this.options = response.data || []
        this.loaded = true
      }).catch(() => {
        this.error = '分类图标加载失败，请重试'
      }).finally(() => {
        this.loading = false
      })
    },
    select(item) {
      this.$emit('input', item.name)
      this.visible = false
    }
  }
}
</script>

<style scoped>
.category-icon-picker__trigger{width:100%;display:flex;align-items:center;gap:10px}
.category-icon-picker__trigger span{flex:1;text-align:left}
.category-icon-picker__trigger small{color:#909399}
.category-icon-picker__body{min-height:260px;margin-top:16px}
.category-icon-picker__grid{display:grid;grid-template-columns:repeat(6,1fr);gap:12px;max-height:360px;overflow:auto}
.category-icon-picker__item{display:flex;flex-direction:column;align-items:center;gap:6px;padding:12px 6px;border:1px solid #dcdfe6;border-radius:6px;background:#fff;cursor:pointer}
.category-icon-picker__item:hover,.category-icon-picker__item.is-selected{color:#409eff;border-color:#409eff;background:#ecf5ff}
.category-icon-picker__item small{color:#909399;max-width:82px;overflow:hidden;text-overflow:ellipsis}
.category-icon-picker__empty{text-align:center;color:#909399;padding:80px 0}
</style>
```

- [ ] **Step 6: 运行选择器契约测试**

```powershell
npm run test:category-icon
```

Working directory: `ruoyi-ui`

Expected: 输出“分类 TDesign 图标选择器契约测试通过”。

- [ ] **Step 7: 提交选择器**

```powershell
git add -- `
  'ruoyi-ui/package.json' `
  'ruoyi-ui/package-lock.json' `
  'ruoyi-ui/src/api/library/content.js' `
  'ruoyi-ui/src/views/library/content/category/CategoryIconPicker.vue' `
  'ruoyi-ui/tests/category-icon-picker.test.js'
git commit -m "feat: add category icon picker"
```

---

### Task 4: 接入分类管理页面并保持 SimpleList 通用

**Files:**

- Modify: `ruoyi-ui/src/views/library/common/SimpleList.vue`
- Modify: `ruoyi-ui/src/views/library/content/category/index.vue`
- Modify: `ruoyi-ui/tests/category-icon-picker.test.js`

- [ ] **Step 1: 扩展失败的页面接入测试**

向 `category-icon-picker.test.js` 增加：

```javascript
const simpleListPath = path.join(root, 'src/views/library/common/SimpleList.vue')
const categoryPagePath = path.join(root, 'src/views/library/content/category/index.vue')

for (const file of [simpleListPath, categoryPagePath]) {
  const parsed = compiler.parseComponent(read(file))
  const result = compiler.compile(parsed.template.content)
  assert.deepStrictEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

const simpleList = read(simpleListPath)
assert(simpleList.includes(":name=\"'column-' + column.prop\""), 'SimpleList 应支持按列名称注入预览')
assert(simpleList.includes(":name=\"'field-' + field.prop\""), 'SimpleList 应支持按字段名称注入输入组件')

const categoryPage = read(categoryPagePath)
assert(categoryPage.includes('slot="column-icon"'), '分类列表应注入图标预览')
assert(categoryPage.includes('slot="field-icon"'), '分类表单应注入图标选择器')
assert(categoryPage.includes('<category-icon-picker'), '分类表单应使用专用图标选择器')
assert(!categoryPage.includes("label:'图标地址'"), '分类表单不再把图标作为地址输入')
assert(categoryPage.includes('图标配置已失效'), '历史失效图标应给出明确提示')
```

- [ ] **Step 2: 运行测试并确认失败**

```powershell
npm run test:category-icon
```

Working directory: `ruoyi-ui`

Expected: FAIL，提示缺少 `column-icon`、`field-icon` 或分类页面选择器。

- [ ] **Step 3: 为 SimpleList 增加通用插槽**

将表格列的默认内容：

```vue
<template slot-scope="scope">{{ displayValue(scope.row[column.prop], column) }}</template>
```

替换为：

```vue
<template slot-scope="scope">
  <slot
    :name="'column-' + column.prop"
    :row="scope.row"
    :value="scope.row[column.prop]"
    :column="column"
  >
    {{ displayValue(scope.row[column.prop], column) }}
  </slot>
</template>
```

将 `el-form-item` 内原有控件分支整体放入动态字段插槽：

```vue
<el-form-item
  v-for="field in formFields"
  :key="field.prop"
  :label="field.label"
  :required="field.required"
>
  <slot :name="'field-' + field.prop" :form="form" :field="field">
    <el-input-number
      v-if="field.type === 'number'"
      v-model="form[field.prop]"
      :min="field.min === undefined ? 0 : field.min"
      :max="field.max"
      controls-position="right"
    />
    <remote-select
      v-else-if="field.type === 'remote-select'"
      v-model="form[field.prop]"
      :field="field"
      :row="form"
      @selection-change="onRemoteSelectionChange(field, $event)"
    />
    <el-select
      v-else-if="field.type === 'select'"
      v-model="form[field.prop]"
      style="width:100%"
    >
      <el-option
        v-for="option in field.options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    <el-date-picker
      v-else-if="field.type === 'datetime'"
      v-model="form[field.prop]"
      type="datetime"
      value-format="yyyy-MM-dd HH:mm:ss"
      style="width:100%"
    />
    <el-input
      v-else
      v-model="form[field.prop]"
      :type="field.type === 'textarea' ? 'textarea' : 'text'"
      :rows="field.rows || 4"
      :maxlength="field.maxlength"
      show-word-limit
    />
  </slot>
</el-form-item>
```

- [ ] **Step 4: 重写分类页面接入选择器**

将 `category/index.vue` 改为：

```vue
<template>
  <simple-list
    title="文档分类"
    :loader="listCategories"
    :creator="addCategory"
    :updater="updateCategory"
    :remover="deleteCategory"
    :columns="columns"
    :form-fields="fields"
    :default-form="defaults"
    :permissions="permissions"
  >
    <template slot="column-icon" slot-scope="{ row }">
      <div class="category-icon-cell">
        <icon :name="isValidIcon(row.icon) ? row.icon : 'file'" size="22px" />
        <span>{{ row.icon || 'file' }}</span>
        <el-tag v-if="iconOptionsLoaded && !isValidIcon(row.icon)" size="mini" type="danger">
          图标配置已失效
        </el-tag>
      </div>
    </template>
    <template slot="field-icon" slot-scope="{ form }">
      <category-icon-picker
        :value="form.icon"
        @input="$set(form, 'icon', $event)"
      />
    </template>
  </simple-list>
</template>

<script>
import { Icon } from 'tdesign-icons-vue'
import SimpleList from '@/views/library/common/SimpleList'
import CategoryIconPicker from './CategoryIconPicker'
import {
  listCategories,
  listCategoryIconOptions,
  addCategory,
  updateCategory,
  deleteCategory
} from '@/api/library/content'

export default {
  name: 'LibraryCategory',
  components: { Icon, SimpleList, CategoryIconPicker },
  data() {
    return {
      listCategories,
      addCategory,
      updateCategory,
      deleteCategory,
      iconOptionsLoaded: false,
      validIconNames: [],
      permissions: {
        add: 'library:category:add',
        edit: 'library:category:edit',
        remove: 'library:category:remove'
      },
      defaults: { name: '', icon: '', sortOrder: 0, status: '0' },
      columns: [
        { prop: 'id', label: '编号' },
        { prop: 'name', label: '分类名称' },
        { prop: 'icon', label: '图标', width: 220 },
        { prop: 'sortOrder', label: '排序' },
        {
          prop: 'status',
          label: '状态',
          options: [
            { value: '0', label: '启用' },
            { value: '1', label: '停用' }
          ]
        }
      ],
      fields: [
        { prop: 'name', label: '分类名称', required: true, maxlength: 64 },
        {
          prop: 'icon',
          label: '分类图标',
          required: true,
          requiredMessage: '请选择分类图标'
        },
        { prop: 'sortOrder', label: '排序', type: 'number', required: true },
        {
          prop: 'status',
          label: '状态',
          type: 'select',
          required: true,
          options: [
            { value: '0', label: '启用' },
            { value: '1', label: '停用' }
          ]
        }
      ]
    }
  },
  created() {
    listCategoryIconOptions().then(response => {
      this.validIconNames = (response.data || []).map(item => item.name)
      this.iconOptionsLoaded = true
    }).catch(() => {
      this.iconOptionsLoaded = false
    })
  },
  methods: {
    isValidIcon(name) {
      return Boolean(name) && this.validIconNames.includes(name)
    }
  }
}
</script>

<style scoped>
.category-icon-cell{display:flex;align-items:center;gap:8px}
</style>
```

- [ ] **Step 5: 运行管理端测试**

```powershell
npm run test:category-icon
npm run test:banner-select
```

Working directory: `ruoyi-ui`

Expected:

- 输出“分类 TDesign 图标选择器契约测试通过”。
- 现有宣传图片远程选择契约测试通过，证明 `SimpleList` 默认字段行为未回归。

- [ ] **Step 6: 提交分类页面接入**

```powershell
git add -- `
  'ruoyi-ui/src/views/library/common/SimpleList.vue' `
  'ruoyi-ui/src/views/library/content/category/index.vue' `
  'ruoyi-ui/tests/category-icon-picker.test.js'
git commit -m "feat: integrate category icon selection"
```

---

### Task 5: 小程序切换为 t-icon 并增加跨端契约检查

**Files:**

- Modify: `miniprogram/components/category-grid/index.json`
- Modify: `miniprogram/components/category-grid/index.wxml`
- Modify: `miniprogram/components/category-grid/index.wxss`
- Create: `miniprogram/tests/category-icon.test.js`

- [ ] **Step 1: 写小程序失败契约测试**

创建 `miniprogram/tests/category-icon.test.js`：

```javascript
const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const componentJson = JSON.parse(fs.readFileSync(
  path.join(root, 'components/category-grid/index.json'), 'utf8'))
const template = fs.readFileSync(
  path.join(root, 'components/category-grid/index.wxml'), 'utf8')
const options = JSON.parse(fs.readFileSync(
  path.resolve(root, '../ruoyi-wechat-library/src/main/resources/library/category-icons.json'),
  'utf8'))
const iconCss = fs.readFileSync(
  path.join(root, 'miniprogram_npm/tdesign-miniprogram/icon/icon.wxss'), 'utf8')

test('分类宫格使用接口 icon 字段渲染 TDesign 图标', () => {
  assert.equal(componentJson.usingComponents['t-icon'], 'tdesign-miniprogram/icon/icon')
  assert.match(template, /<t-icon/)
  assert.match(template, /name="\{\{item\.icon \|\| 'file'\}\}"/)
  assert.doesNotMatch(template, /item\.iconUrl/)
  assert.doesNotMatch(template, /<image[^>]+grid__icon/)
})

test('所有后台精选图标都存在于当前小程序 TDesign 版本', () => {
  assert.equal(options.length, 24)
  for (const option of options) {
    const escaped = option.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    assert.match(iconCss, new RegExp(`\\.t-icon-${escaped}:before\\{`),
      `小程序当前 TDesign 版本缺少图标：${option.name}`)
  }
})
```

- [ ] **Step 2: 运行测试并确认失败**

```powershell
npm test
```

Working directory: `miniprogram`

Expected: 新增测试 FAIL，分类宫格仍使用 `item.iconUrl` 和 `<image>`。

- [ ] **Step 3: 注册并使用 t-icon**

将 `category-grid/index.json` 改为：

```json
{
  "component": true,
  "usingComponents": {
    "t-icon": "tdesign-miniprogram/icon/icon"
  }
}
```

将 `category-grid/index.wxml` 改为：

```xml
<view class="grid">
  <view wx:for="{{items}}" wx:key="id" class="grid__item" data-id="{{item.id}}" bindtap="open">
    <t-icon class="grid__icon" name="{{item.icon || 'file'}}" size="64rpx" />
    <text>{{item.name}}</text>
  </view>
</view>
```

将 `category-grid/index.wxss` 调整为可读的多行样式，并保留图标占位：

```css
.grid{display:grid;grid-template-columns:repeat(4,1fr);grid-template-rows:repeat(2,150rpx);gap:16rpx}
.grid__item{display:flex;flex-direction:column;align-items:center;justify-content:center;background:#fff;border-radius:20rpx;font-size:24rpx}
.grid__icon{display:block;width:64rpx;height:64rpx;margin-bottom:12rpx;color:#2457d6}
```

- [ ] **Step 4: 运行小程序全部测试**

```powershell
npm test
```

Working directory: `miniprogram`

Expected: 所有 `tests/*.test.js` PASS，新契约测试确认 24 个名称都存在。

- [ ] **Step 5: 提交小程序改动**

```powershell
git add -- `
  'miniprogram/components/category-grid/index.json' `
  'miniprogram/components/category-grid/index.wxml' `
  'miniprogram/components/category-grid/index.wxss' `
  'miniprogram/tests/category-icon.test.js'
git commit -m "feat: render category TDesign icons"
```

---

### Task 6: 全量相关验证和交付检查

**Files:**

- Verify only; do not edit generated output.

- [ ] **Step 1: 运行后端相关测试**

```powershell
$env:JAVA_HOME='E:\JDK8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -pl ruoyi-admin -am `
  '-Dtest=CategoryIconCatalogTest,DocumentServiceTest,LibraryContentControllerTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 指定测试全部 PASS，Maven `BUILD SUCCESS`。

- [ ] **Step 2: 运行管理端契约测试和生产构建**

```powershell
npm run test:category-icon
npm run test:banner-select
npm run build:prod
```

Working directory: `ruoyi-ui`

Expected: 两项契约测试 PASS，生产构建成功。不得暂存或提交 `ruoyi-ui/dist`。

- [ ] **Step 3: 运行小程序全部测试**

```powershell
npm test
```

Working directory: `miniprogram`

Expected: 所有 Node tests PASS。

- [ ] **Step 4: 检查变更质量和禁止文件**

```powershell
git diff --check
git status --short
git diff --name-only
```

Expected:

- `git diff --check` 无输出且退出码为 0。
- 变更中不包含 `.env`、`application.yml`、`application-druid.yml`、`target` 或 `ruoyi-ui/dist`。
- 当前任务只包含计划列出的源文件、测试文件、依赖锁文件和文档；用户原有未提交改动保持不变。
