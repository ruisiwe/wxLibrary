const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/library/agreement/index.vue'),
  'utf8'
)

assert(page.includes('@click="view(s.row)">查看</el-button>'),
  '所有协议行都应提供查看操作')
assert(page.includes("v-if=\"s.row.status === '0'\""),
  '修改和发布操作应继续仅对草稿显示')
assert(page.includes('readOnly: false'),
  '协议页面应维护只读查看状态')
assert(page.includes("readOnly ? '查看协议版本'"),
  '查看模式应显示查看协议版本标题')
assert(page.includes('view(row)'),
  '协议页面应提供查看方法')
assert(page.includes('this.readOnly = true'),
  '查看方法应启用只读模式')
assert(page.includes('this.readOnly = false'),
  '新增和修改时应退出只读模式')
assert((page.match(/:disabled="readOnly"/g) || []).length === 5,
  '查看模式应禁用全部五个协议表单控件')
assert(page.includes('v-if="readOnly" @click="visible = false">关闭</el-button>'),
  '查看模式应只提供关闭操作')
assert(page.includes('<template v-else>'),
  '非查看模式应保留取消和保存草稿操作')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `协议管理页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('协议操作列与只读查看契约测试通过')
