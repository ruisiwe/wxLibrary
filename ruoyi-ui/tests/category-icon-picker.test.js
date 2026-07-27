const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const pickerPath = path.join(root, 'src/views/library/content/category/CategoryIconPicker.vue')
const simpleListPath = path.join(root, 'src/views/library/common/SimpleList.vue')
const categoryPagePath = path.join(root, 'src/views/library/content/category/index.vue')
const apiPath = path.join(root, 'src/api/library/content.js')
const packagePath = path.join(root, 'package.json')

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

assert(fs.existsSync(pickerPath), '应提供分类 TDesign 图标选择器')
const source = read(pickerPath)
const parsed = compiler.parseComponent(source)
const result = compiler.compile(parsed.template.content)

assert.deepStrictEqual(result.errors, [], `图标选择器模板编译失败：${result.errors.join('；')}`)
assert(source.includes("import { Icon } from 'tdesign-icons-vue'"), '选择器应使用 TDesign Vue 图标组件')
assert(source.includes('listCategoryIconOptions'), '选择器应加载后台图标目录')
assert(source.includes("this.$emit('input', item.name)"), '选择器只应回传图标名称字符串')
assert(source.includes('filteredOptions'), '选择器应支持按名称、标签和关键词过滤')
assert(source.includes("displayName()"), '选择器应提供默认图标预览')
assert(source.includes("return this.selectedOption ? this.selectedOption.name : 'file'"), '未选择时应使用 file 作为预览兜底')

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

const api = read(apiPath)
assert(api.includes('listCategoryIconOptions'), '管理端应提供分类图标目录 API 方法')
assert(api.includes('/library/category/icon-options'), '管理端应请求分类图标目录接口')

const pkg = JSON.parse(read(packagePath))
assert.strictEqual(pkg.dependencies['tdesign-icons-vue'], '0.4.2', '应精确锁定 Vue 2 图标版本')

console.log('分类 TDesign 图标选择器契约测试通过')
