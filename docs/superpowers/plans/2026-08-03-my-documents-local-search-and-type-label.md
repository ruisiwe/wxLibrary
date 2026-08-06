# My Documents Local Search and Type Label Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the shared file-format badge in My Documents and category lists, and filter the already-loaded My Documents list by title on every input change without backend requests.

**Architecture:** Add a pure `filterDocumentsByTitle(items, keyword)` helper to the existing document service so title matching is testable outside WeChat. My Documents stores both the complete response and the visible filtered array; WXML only renders the visible array. Existing `document-row` badge markup and styling are reused through its `showFileType` property.

**Tech Stack:** WeChat Mini Program JavaScript, WXML, WXSS, Node.js built-in test runner.

---

### Task 1: Define the local title-filter contract

**Files:**
- Create: `miniprogram/tests/my-documents-search.test.js`
- Modify: `miniprogram/services/document.js`

- [ ] **Step 1: Write the failing pure-function tests**

Create a Node test that imports `filterDocumentsByTitle` and verifies case-insensitive substring matching, trimmed keywords, empty keyword restoration, safe empty titles, stable order, and no mutation:

```javascript
test('我的文档按标题实时本地过滤并保留原顺序', () => {
  const source = [
    { id: 1, title: 'Quality Manual' },
    { id: 2, title: '实验室质量控制' },
    { id: 3, title: null }
  ];
  const snapshot = source.slice();

  assert.deepEqual(filterDocumentsByTitle(source, ' quality '), [source[0]]);
  assert.deepEqual(filterDocumentsByTitle(source, '质量'), [source[1]]);
  assert.deepEqual(filterDocumentsByTitle(source, ''), source);
  assert.deepEqual(source, snapshot);
  assert.notEqual(filterDocumentsByTitle(source, ''), source);
});
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/my-documents-search.test.js
```

Expected: FAIL because `filterDocumentsByTitle` is not exported.

- [ ] **Step 3: Implement the minimal pure helper**

Add this behavior to `miniprogram/services/document.js` and export it:

```javascript
function filterDocumentsByTitle(items, keyword) {
  const source = Array.isArray(items) ? items : [];
  const normalizedKeyword = String(keyword || '').trim().toLowerCase();
  if (!normalizedKeyword) return source.slice();
  return source.filter(item => String(item && item.title || '')
    .toLowerCase().includes(normalizedKeyword));
}
```

- [ ] **Step 4: Re-run the focused test and verify GREEN**

Expected: the pure-function test PASS.

### Task 2: Add realtime My Documents search state and UI

**Files:**
- Modify: `miniprogram/pages/my-documents/my-documents.js`
- Modify: `miniprogram/pages/my-documents/my-documents.wxml`
- Modify: `miniprogram/pages/my-documents/my-documents.wxss`
- Modify: `miniprogram/tests/my-documents-search.test.js`

- [ ] **Step 1: Add failing page-contract assertions**

Read the page source files and assert:

```javascript
assert.match(page, /allItems:\s*\[\]/);
assert.match(page, /searchKeyword:\s*''/);
assert.equal((page.match(/documents\.unlocked\(\)/g) || []).length, 1);
assert.match(page, /onSearchInput[\s\S]*filterDocumentsByTitle\(this\.data\.allItems/);
assert.match(template, /bindinput="onSearchInput"/);
assert.doesNotMatch(template, /bindtap="search"|>搜索<\/button>/);
assert.match(template, /未找到相关文档/);
assert.match(template, /还没有兑换文档/);
```

- [ ] **Step 2: Run the focused test and verify RED**

Expected: FAIL because the page has no local search state or input.

- [ ] **Step 3: Implement My Documents state and filtering**

Update page data to include `allItems` and `searchKeyword`. On load success, store the complete response and derive visible `items` with the current keyword:

```javascript
const allItems = items || [];
this.setData({
  allItems,
  items: documents.filterDocumentsByTitle(allItems, this.data.searchKeyword),
  loading: false
});
```

Add the input handler:

```javascript
onSearchInput(event) {
  const searchKeyword = event.detail.value || '';
  this.setData({
    searchKeyword,
    items: documents.filterDocumentsByTitle(this.data.allItems, searchKeyword)
  });
}
```

- [ ] **Step 4: Add the input and empty states**

Place a rounded input above the list:

```xml
<view class="local-search">
  <input value="{{searchKeyword}}" placeholder="搜索我的文档" bindinput="onSearchInput" />
</view>
```

Keep `items` as the loop source and set:

```xml
empty-text="{{allItems.length ? '未找到相关文档' : '还没有兑换文档'}}"
```

Style `.local-search` and its input with a 68rpx height, pale gray background, horizontal padding, and pill-shaped radius. Do not add a search button.

- [ ] **Step 5: Re-run the focused test and verify GREEN**

Expected: My Documents search tests PASS.

### Task 3: Enable the shared type badge in both requested lists

**Files:**
- Modify: `miniprogram/pages/my-documents/my-documents.wxml`
- Modify: `miniprogram/pages/category/category.wxml`
- Modify: `miniprogram/tests/my-documents-search.test.js`

- [ ] **Step 1: Add failing badge-scope assertions**

Assert both page templates contain `show-file-type="{{true}}"` on `document-row`, and the shared component remains the only owner of `.row__file-type` markup and styling.

- [ ] **Step 2: Run the focused test and verify RED**

Expected: FAIL because neither requested page enables the property.

- [ ] **Step 3: Enable the existing component property**

Change only the two `document-row` calls:

```xml
<document-row wx:for="{{items}}" wx:key="id" document="{{item}}" show-file-type="{{true}}" />
```

- [ ] **Step 4: Re-run the focused test and verify GREEN**

Expected: My Documents search and badge tests PASS.

### Task 4: Regression verification

**Files:**
- Verify only; do not modify backend or sensitive configuration.

- [ ] **Step 1: Run the new and related mini-program tests together**

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test miniprogram/tests/my-documents-search.test.js miniprogram/tests/document-access.test.js miniprogram/tests/document-thumbnail-display.test.js
```

Expected: all tests PASS.

- [ ] **Step 2: Check the final diff**

Run `git diff --check` and inspect the exact task files. Verify there is no new request call in `onSearchInput`, no backend change, no build output, and no sensitive configuration change.

Because this working tree already contains overlapping user changes, do not stage or commit implementation files unless the user explicitly requests a code commit.
