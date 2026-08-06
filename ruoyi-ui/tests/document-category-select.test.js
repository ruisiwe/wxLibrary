const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const pagePath = path.join(root, 'src/views/library/content/document/index.vue')
const apiPath = path.join(root, 'src/api/library/content.js')
const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert(
  page.includes('<el-table-column prop="categoryName" label="分类名称"'),
  '文档列表应显示分类名称'
)
assert(
  !page.includes('<el-table-column prop="categoryId" label="分类编号"'),
  '文档列表不应继续显示分类编号'
)
assert(
  page.includes('<el-table-column prop="sortOrder" label="排序"'),
  '文档列表应显示排序字段'
)
assert(
  page.includes('<el-form-item label="文档分类" required>'),
  '文档表单应显示文档分类字段'
)
const categorySelect = page.match(/<el-select[\s\S]*?v-model="form\.categoryId"[\s\S]*?>/)
assert(categorySelect, '文档表单应通过下拉框选择分类')
assert(
  !page.includes('<el-input-number v-model="form.categoryId"'),
  '文档表单不应继续手工输入分类编号'
)
assert(
  !categorySelect[0].includes('filterable'),
  '分类数量较少，文档分类下拉框不应启用搜索'
)
assert(
  page.includes(':label="categoryOptionLabel(category)"') &&
    page.includes(':value="category.id"'),
  '分类下拉选项应显示分类名称并提交分类编号'
)
assert(
  page.includes(':disabled="category.status !== \'0\'"'),
  '修改文档时回显的停用分类不得重新选择'
)
assert(
  page.includes('listDocumentCategoryOptions'),
  '文档页面应调用文档模块分类选项接口'
)
assert(
  page.includes("this.$modal.msgError('请选择文档分类')"),
  '未选择分类时应显示简体中文提示'
)
assert(
  api.includes("url: '/library/document/category-options'"),
  '前端 API 应提供文档分类选项接口'
)

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `文档页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('文档分类下拉选择契约测试通过')
