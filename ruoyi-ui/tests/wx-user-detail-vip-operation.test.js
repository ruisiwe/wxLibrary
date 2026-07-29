const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/library/user/index.vue'),
  'utf8'
)

assert(page.includes('<el-avatar'), '微信用户详情应显示用户头像')
assert(page.includes('avatarUrl(detail)'), '详情头像应使用微信头像公共访问地址')
assert(!page.includes('头像路径'), '详情弹窗不应展示内部头像存储路径')
assert(page.includes('detailVipState'), '详情弹窗应计算会员状态')
assert(page.includes('VIP会员'), '有效会员应显示VIP会员状态')
assert(page.includes('已过期'), '过期会员应显示已过期状态')
assert(page.includes('未开通'), '非会员应显示未开通状态')
assert(page.includes('开通会员'), '非会员详情应提供开通会员操作')
assert(page.includes('续期会员'), '有效会员详情应提供续期会员操作')
assert(page.includes("v-hasPermi=\"['library:vip:operation']\""),
  '会员操作按钮应沿用VIP操作权限')
assert(page.includes('formatDateTime'), '详情页应统一格式化日期时间')
assert(page.includes("parseTime(value, '{y}-{m}-{d}')"),
  '会员到期和最后登录时间应只显示年月日')
assert(page.includes('listVipPlans'), '开通会员时应查询启用套餐')
assert(page.includes('openVip'), '确认开通时应复用人工开通接口')
assert(page.includes('userIds: [this.detail.id]'), '会员操作应锁定当前详情用户')
assert(page.includes('batchNo: this.createBatchNo()'), '会员操作编号应由前端自动生成')
assert(page.includes('maxlength="500"'), '会员操作原因应限制为500字')
assert(page.includes(':loading="vipSubmitting"'), '会员提交期间应锁定确认按钮')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `微信用户详情页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('微信用户详情与会员操作契约测试通过')
