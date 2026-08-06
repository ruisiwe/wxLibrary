const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const apiPath = path.resolve(__dirname, '../src/api/library/qrConfig.js')
const pagePath = path.resolve(__dirname, '../src/views/library/qr/index.vue')

assert(fs.existsSync(apiPath), '应提供二维码管理 API')
assert(fs.existsSync(pagePath), '应提供二维码管理页面')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const method of [
  'listQrConfigs',
  'getQrConfig',
  'addQrConfig',
  'updateQrConfig',
  'deleteQrConfig',
  'uploadQrConfigImage',
  'clearQrConfigImage',
  'getQrConfigImage'
]) {
  assert(api.includes(method), `缺少 API：${method}`)
}

assert(api.includes("responseType: 'blob'"), '后台图片预览应使用带登录令牌的 blob 请求')
assert(page.includes('menuName'), '页面应维护菜单名称')
assert(page.includes('guideText'), '页面应维护引导文字')
assert(page.includes('sortOrder'), '页面应维护排序')
assert(page.includes('status'), '页面应维护状态')
assert(page.includes('imageConfigured'), '页面应展示图片配置状态')
assert(page.includes('JPEG、PNG 或 WebP'), '页面应提示允许的图片格式')
assert(page.includes('2MB'), '页面应限制图片不超过 2MB')
assert(page.includes('上传图片'), '页面应支持上传图片')
assert(page.includes('替换图片'), '页面应支持替换图片')
assert(page.includes('清空图片'), '页面应支持清空图片')
assert(!page.includes('<el-card'), '单功能页面不应使用最外层卡片边框')
assert(!/<h[1-6][^>]*>二维码管理<\/h[1-6]>/.test(page), '单功能页面不重复展示功能标题')

const component = compiler.parseComponent(page)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [], `二维码管理页面模板编译失败：${compiled.errors.join('；')}`)

console.log('二维码管理后台页面契约测试通过')
