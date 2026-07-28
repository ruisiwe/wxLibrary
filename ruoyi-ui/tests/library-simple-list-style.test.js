const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const componentPath = 'src/views/library/common/SimpleList.vue'
const plainPages = [
  'src/views/library/points/record/index.vue',
  'src/views/library/points/rule/index.vue',
  'src/views/library/vip/order/index.vue',
  'src/views/library/vip/plan/index.vue',
  'src/views/library/content/category/index.vue',
  'src/views/library/vip/code/index.vue',
  'src/views/library/access/courseCode/index.vue'
]
const embeddedPages = new Set([
  'src/views/library/vip/code/index.vue',
  'src/views/library/access/courseCode/index.vue'
])

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function compile(file, source) {
  const component = compiler.parseComponent(source)
  const result = compiler.compile(component.template.content)
  assert.deepStrictEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

function simpleListTag(source) {
  const match = source.match(/<simple-list\b[\s\S]*?>/i)
  assert(match, '页面必须包含 simple-list')
  return match[0]
}

const component = read(componentPath)
assert(component.includes("plain: { type: Boolean, default: false }"), 'SimpleList 必须提供显式 plain 开关')
assert(component.includes("embedded: { type: Boolean, default: false }"), 'SimpleList 必须提供显式 embedded 开关')
assert(component.includes(":is=\"plain ? 'div' : 'el-card'\""), 'plain 模式必须去掉最外层卡片')
assert(component.includes(':border="!plain"'), 'plain 模式必须关闭表格外框')
assert(component.includes(':stripe="!plain"'), 'plain 模式必须关闭斑马纹')
assert(component.includes('v-if="!plain" slot="header"'), 'plain 模式不得显示页面功能标题')
assert(component.includes('<right-toolbar :search="false" @queryTable="loadData"'), 'plain 模式必须使用右侧刷新工具栏')
compile(componentPath, component)

plainPages.forEach(file => {
  const source = read(file)
  const tag = simpleListTag(source)
  assert(/\splain(?:\s|\/|>)/.test(tag), `${file} 必须显式启用 plain`)
  if (embeddedPages.has(file)) {
    assert(/\sembedded(?:\s|\/|>)/.test(tag), `${file} 必须显式启用 embedded`)
    assert(source.includes('class="mb8"'), `${file} 必须使用标准操作行`)
    assert(source.includes('<right-toolbar'), `${file} 必须在页面操作行提供刷新按钮`)
  }
  compile(file, source)
})

console.log('资料库 SimpleList 逐页样式契约测试通过')
