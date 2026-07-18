const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const sqlPath = path.join(root, 'sql', 'wechat_library_menu.sql')
assert.ok(fs.existsSync(sqlPath), '缺少微信文库菜单 SQL')
const sql = fs.readFileSync(sqlPath, 'utf8')
;[
  'library:banner:list', 'library:document:list', 'library:user:list',
  'library:points:rule', 'library:vip:refund', 'library:course:code'
].forEach(permission => assert.ok(sql.includes(permission), permission))

;['content', 'user', 'points', 'vip', 'course', 'agreement'].forEach(name => {
  assert.ok(fs.existsSync(path.join(root, 'ruoyi-ui', 'src', 'api', 'library', name + '.js')), '缺少 API：' + name)
})

;[
  'content/banner/index.vue', 'content/category/index.vue', 'content/document/index.vue',
  'content/conversion/index.vue', 'content/course/index.vue', 'content/video/index.vue',
  'user/index.vue', 'user/detail.vue', 'points/rule/index.vue', 'points/record/index.vue',
  'vip/plan/index.vue', 'vip/order/index.vue', 'vip/entitlement/index.vue',
  'vip/refund/index.vue', 'access/document/index.vue', 'access/courseCode/index.vue',
  'access/progress/index.vue', 'agreement/index.vue'
].forEach(view => {
  assert.ok(fs.existsSync(path.join(root, 'ruoyi-ui', 'src', 'views', 'library', view)), '缺少页面：' + view)
})

console.log('微信文库菜单与管理端 API 契约检查通过')
