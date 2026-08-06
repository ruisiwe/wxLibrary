const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const menu = fs.readFileSync(
  path.resolve(__dirname, '../../sql/wechat_library_menu.sql'),
  'utf8'
)

assert(menu.includes("'VIP 权益介绍'"), '会员管理下应新增“VIP 权益介绍”菜单')
assert(menu.includes("'library/vip/benefit/index'"), '菜单应指向VIP权益介绍维护页')
assert(menu.includes("'library:vip:benefit:list'"), '菜单应包含权益查询权限')
assert(menu.includes("'library:vip:benefit:add'"), '菜单应包含权益新增权限')
assert(menu.includes("'library:vip:benefit:edit'"), '菜单应包含权益修改权限')
assert(menu.includes("'library:vip:benefit:remove'"), '菜单应包含权益删除权限')
assert(menu.includes("'library:vip:page-config:query'"), '菜单应包含客服配置查询权限')
assert(menu.includes("'library:vip:page-config:edit'"), '菜单应包含客服配置修改权限')

const apiPath = path.resolve(__dirname, '../src/api/library/vipBenefit.js')
const pagePath = path.resolve(__dirname, '../src/views/library/vip/benefit/index.vue')
assert(fs.existsSync(apiPath), '应新增VIP权益介绍后台API')
assert(fs.existsSync(pagePath), '应新增VIP权益介绍后台页面')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')
assert(!/<h2[^>]*>VIP 权益介绍<\/h2>/.test(page), '页面不应重复显示“VIP 权益介绍”总标题')
assert(page.includes('<span>客服微信配置</span>'), '页面应保留客服微信配置分区标题')
assert(page.includes('<span>权益文字列表</span>'), '页面应保留权益文字列表分区标题')
assert(page.includes('客服微信图片'), '页面应维护客服微信图片')
assert(page.includes('开通 VIP 请添加客服微信'), '页面应提供默认客服提示语')
assert(page.includes('权益文字'), '页面应维护权益文字')
assert(page.includes("v-hasPermi=\"['library:vip:benefit:add']\""), '新增按钮应校验新增权限')
assert(page.includes("v-hasPermi=\"['library:vip:page-config:edit']\""), '客服保存按钮应校验配置修改权限')
assert(page.includes('el-upload'), '客服微信图片应使用本地文件上传')
assert(api.includes("url: '/library/vip-benefit/list'"), 'API应查询权益列表')
assert(api.includes("url: '/library/vip-page-config'"), 'API应查询客服配置')
assert(api.includes("formData.append('config'"), '客服配置应以JSON multipart部件提交')
assert(api.includes("formData.append('image'"), '客服图片应以multipart图片部件提交')
assert(api.includes('repeatSubmit: false'), '客服图片上传应关闭重复提交拦截')

assert(api.includes('clearVipPageConfigImage'), 'API应提供清空客服微信图片方法')
assert(api.includes("url: '/library/vip-page-config/image'"), '清空客服微信图片应调用独立接口')
assert(page.includes('@click="clearCustomerServiceImage"'), '已上传客服微信图片时应提供清空按钮')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [], `VIP权益介绍页面模板编译失败：${compiled.errors.join('；')}`)

console.log('VIP权益介绍后台页面契约测试通过')
