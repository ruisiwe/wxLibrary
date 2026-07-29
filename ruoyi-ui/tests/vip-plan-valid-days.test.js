const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const planPath = path.join(root, 'src/views/library/vip/plan/index.vue')
const simpleListPath = path.join(root, 'src/views/library/common/SimpleList.vue')
const planPage = fs.readFileSync(planPath, 'utf8')
const simpleList = fs.readFileSync(simpleListPath, 'utf8')

assert(
  /\{prop:'validDays',label:'有效天数（天）',type:'number',required:true,min:1,max:3650,precision:0\}/.test(planPage),
  '会员套餐有效天数应配置为1到3650天的整数输入'
)
assert(
  !/prop:'validDays'[^}]+type:'select'/.test(planPage),
  '会员套餐有效天数不应继续使用固定下拉选项'
)
assert(
  simpleList.includes(':precision="field.precision"'),
  '通用数字控件应支持字段级整数精度'
)

const component = compiler.parseComponent(planPage)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(
  compiled.errors,
  [],
  `会员套餐页面模板编译失败：${compiled.errors.join('；')}`
)

console.log('VIP套餐自定义有效天数契约测试通过')
