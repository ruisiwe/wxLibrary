const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const userPage = fs.readFileSync(
  path.join(root, 'src/views/library/user/index.vue'),
  'utf8'
)
const viewRoot = path.join(root, 'src/views')
const vueFiles = []

function collect(directory) {
  fs.readdirSync(directory, { withFileTypes: true }).forEach(entry => {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) collect(target)
    else if (entry.isFile() && entry.name.endsWith('.vue')) vueFiles.push(target)
  })
}

collect(viewRoot)
vueFiles.forEach(file => {
  const source = fs.readFileSync(file, 'utf8')
  assert(
    !/<el-form-item[^>]+label=["'][^"']*业务(?:编号|编码)[^"']*["']/.test(source),
    `管理表单不得要求人工填写业务编号：${path.relative(root, file)}`
  )
})

assert(userPage.includes('batchNo'), '积分调整请求应提交隐藏批次标识')
assert(!userPage.includes('form.bizNo'), '积分调整页面不得保留人工业务编号字段')
assert(userPage.includes('createBatchNo'), '打开积分调整弹窗时应生成批次标识')
assert(userPage.includes(':loading="submitting"'), '提交期间应锁定确认按钮')
assert(
  userPage.includes('系统会自动记录本次操作编号'),
  '积分调整提示应说明操作编号由系统记录'
)

const component = compiler.parseComponent(userPage)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `微信用户页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('人工积分业务编号自动生成契约测试通过')
