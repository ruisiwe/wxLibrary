const assert = require('assert')
const fs = require('fs')
const path = require('path')
const test = require('node:test')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function compile(file, source) {
  const component = compiler.parseComponent(source)
  const result = compiler.compile(component.template.content)
  assert.deepStrictEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

function assertBorderlessTables(file) {
  const source = read(file)
  const component = compiler.parseComponent(source)
  const tags = component.template.content.match(/<el-table\b[\s\S]*?>/g) || []
  assert(tags.length > 0, `${file} 必须包含业务表格`)
  tags.forEach(tag => {
    assert(!/\sborder(?:\s|=|>)/.test(tag), `${file} 表格不得启用 border`)
    assert(!/\sstripe(?:\s|=|>)/.test(tag), `${file} 表格不得启用 stripe`)
  })
  compile(file, source)
  return source
}

test('标准资料库列表页使用无边框表格和右侧工具栏', () => {
  const files = [
    'src/views/library/user/index.vue',
    'src/views/library/vip/entitlement/index.vue',
    'src/views/library/content/banner/index.vue',
    'src/views/library/content/document/index.vue'
  ]
  files.forEach(file => {
    const source = assertBorderlessTables(file)
    assert(source.includes('class="mb8"'), `${file} 必须使用标准操作行`)
    assert(source.includes('<right-toolbar'), `${file} 必须提供右侧刷新工具栏`)
  })
})

test('紧凑资料库列表页使用无边框表格和右侧工具栏', () => {
  const files = [
    'src/views/library/vip/refund/index.vue',
    'src/views/library/content/video/index.vue',
    'src/views/library/content/course/index.vue',
    'src/views/library/agreement/index.vue'
  ]
  files.forEach(file => {
    const source = assertBorderlessTables(file)
    assert(source.includes('class="mb8"'), `${file} 必须使用标准操作行`)
    assert(source.includes('<right-toolbar'), `${file} 必须提供右侧刷新工具栏`)
  })
})

test('VIP权益多配置区保留分区标题并移除表格边框', () => {
  const file = 'src/views/library/vip/benefit/index.vue'
  const source = assertBorderlessTables(file)
  assert(!/<h2[^>]*>VIP 权益介绍<\/h2>/.test(source), '多配置区页面不得保留重复总标题')
  assert(source.includes('<span>客服微信配置</span>'), '必须保留客服微信配置分区标题')
  assert(source.includes('<span>权益文字列表</span>'), '必须保留权益文字列表分区标题')
})
