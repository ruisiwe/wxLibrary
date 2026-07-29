const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/library/vip/code/index.vue'),
  'utf8'
)

assert(page.includes('label="会员套餐"'), '会员码生成弹窗应显示会员套餐字段')
assert(page.includes('<el-select'), '会员码生成弹窗应使用下拉框选择套餐')
assert(
  !page.includes('<el-input-number v-model="form.planId"'),
  '会员码生成弹窗不应继续手工输入套餐编号'
)
assert(page.includes('listVipPlans'), '会员码生成弹窗应复用会员套餐列表接口')
assert(
  page.includes("listVipPlans({ status: '0', pageNum: 1, pageSize: 100 })"),
  '会员码生成弹窗应只查询启用套餐'
)
assert(
  page.includes('form: { planId: null, count: 10, expiresTime: null }'),
  '会员码生成弹窗不应默认选中固定套餐编号'
)
assert(
  page.includes("this.$modal.msgError('请选择会员套餐')"),
  '未选择套餐时应显示简体中文提示'
)
assert(page.includes('planOptionLabel(plan)'), '套餐选项应显示完整套餐摘要')
assert(page.includes('generateVipCodes(this.form)'), '生成请求应继续提交原有planId字段')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `会员码生成页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('会员码生成套餐选择契约测试通过')
