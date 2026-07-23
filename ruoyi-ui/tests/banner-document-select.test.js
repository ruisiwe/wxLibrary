const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const remoteSelectPath = path.join(root, 'src/views/library/common/RemoteSelect.vue')
const simpleListPath = path.join(root, 'src/views/library/common/SimpleList.vue')
const bannerPath = path.join(root, 'src/views/library/content/banner/index.vue')
const documentPath = path.join(root, 'src/views/library/content/document/index.vue')
const apiPath = path.join(root, 'src/api/library/content.js')

assert(fs.existsSync(remoteSelectPath), '应提供通用远程搜索选择组件')

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

function assertVueCompiles(file) {
  const source = read(file)
  const component = compiler.parseComponent(source)
  assert(component.template, `${file} 缺少模板`)
  const result = compiler.compile(component.template.content)
  assert.deepStrictEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

assertVueCompiles(remoteSelectPath)
assertVueCompiles(simpleListPath)
assertVueCompiles(bannerPath)
assertVueCompiles(documentPath)

const remoteSelect = read(remoteSelectPath)
assert(remoteSelect.includes('remoteLoader'), '远程选择组件应调用配置的搜索函数')
assert(remoteSelect.includes('beforeDestroy'), '远程选择组件销毁时应清理延迟任务')
assert(remoteSelect.includes('selection-change'), '远程选择组件应通知表单所选选项')
assert(/seedInitialOption\(\)\s*{[\s\S]*?this\.requestSequence \+= 1/.test(remoteSelect), '切换表单时应使旧搜索请求失效')
assert(remoteSelect.includes('remote-select__error'), '远程搜索失败信息应在选择框下方可见')

const simpleList = read(simpleListPath)
assert(simpleList.includes("field.type === 'remote-select'"), '通用表单应支持远程选择字段')
assert(simpleList.includes('field.validate'), '通用表单应执行字段级有效性校验')
assert(simpleList.includes('missing.requiredMessage'), '通用表单应支持选择类字段的必填提示')

const banner = read(bannerPath)
assert(banner.includes("import RemoteSelect from '@/views/library/common/RemoteSelect'"), '宣传图片页应直接使用远程搜索组件')
assert(banner.includes('<remote-select'), '宣传图片表单应使用远程搜索选择')
assert(banner.includes('documentTitle'), '宣传图片列表应显示关联文档标题')
assert(banner.includes('documentSelectable'), '宣传图片编辑应校验原关联文档是否仍可用')
assert(banner.includes('请选择关联文档'), '未选择关联文档时应使用选择提示')
assert(banner.includes('searchDocumentOptions'), '宣传图片页应调用分页文档搜索')
assert(!banner.includes('documentSelectable: true'), '前端不得把所有搜索结果强制标记为可关联')
assert(banner.includes('documentAvailabilityText'), '文档搜索结果应显示中文可关联状态')
assert(banner.includes("DRAFT: '草稿，暂不可关联'"), '草稿状态提示不得承诺发布后一定可关联')
assert(banner.includes("CATEGORY_DISABLED: '分类已停用'"), '分类停用状态应显示中文')

const documentPage = read(documentPath)
assert(documentPage.includes('conversionStatusText'), '文档处理状态应转换为中文显示')
assert(documentPage.includes('publishStatusText'), '文档发布状态应转换为中文显示')
assert(documentPage.includes("PENDING: '待处理'"), '待处理状态应显示中文')
assert(documentPage.includes("PUBLISHED: '已发布'"), '已发布状态应显示中文')
assert(!documentPage.includes('<el-table-column prop="conversionStatus" label="处理状态" />'), '不得直接显示英文处理状态码')
assert(!documentPage.includes('<el-table-column prop="publishStatus" label="发布状态" />'), '不得直接显示英文发布状态码')

const api = read(apiPath)
assert(api.includes('/library/banner/document-options'), '前端应调用宣传图片文档选项接口')

console.log('宣传图片关联文档远程选择契约测试通过')
