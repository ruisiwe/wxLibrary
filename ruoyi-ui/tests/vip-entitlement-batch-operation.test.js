const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/library/vip/entitlement/index.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/library/vip.js'), 'utf8')

assert((page.match(/\bmultiple\b/g) || []).length >= 1, '会员操作弹窗应支持用户多选')
assert(page.includes('remote-method'), '用户选择器应支持远程搜索')
assert(page.includes('el-avatar'), '用户选项应显示微信头像')
assert(page.includes('avatarFallback'), '头像加载失败应显示默认占位')
assert(!page.includes('<el-form-item label="业务编号"'), '弹窗不应要求人工填写业务编号')
assert(page.includes('userIds'), '请求应提交用户编号数组')
assert(page.includes('batchNo'), '请求应提交隐藏批次标识')
assert(page.includes(':loading="submitting"'), '提交期间应禁用确认按钮')
assert(api.includes("url: '/library/vip-operation/user-options'"),
  '应调用VIP专用用户候选接口')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [],
  `权益台账页面模板编译失败：${compiled.errors.join('；')}`)

console.log('VIP权益批量操作页面契约测试通过')
