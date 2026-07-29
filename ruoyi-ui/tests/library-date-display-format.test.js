const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function compile(file, source) {
  const component = compiler.parseComponent(source)
  const result = compiler.compile(component.template.content)
  assert.deepEqual(result.errors, [], `${file} 模板编译失败：${result.errors.join('；')}`)
}

test('微信用户与权益台账日期只显示年月日', () => {
  const user = read('src/views/library/user/index.vue')
  const detail = read('src/views/library/user/detail.vue')
  const entitlement = read('src/views/library/vip/entitlement/index.vue')

  assert.match(user, /parseTime\(scope\.row\.vipExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(user, /parseTime\(value,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(detail, /parseTime\(user\.vipExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(detail, /parseTime\(user\.lastLoginTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(entitlement, /parseTime\(scope\.row\.oldExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(entitlement, /parseTime\(scope\.row\.newExpireTime,\s*'\{y\}-\{m\}-\{d\}'\)/)

  compile('src/views/library/user/index.vue', user)
  compile('src/views/library/user/detail.vue', detail)
  compile('src/views/library/vip/entitlement/index.vue', entitlement)
})

test('协议和宣传图片列表日期只显示年月日', () => {
  const agreement = read('src/views/library/agreement/index.vue')
  const banner = read('src/views/library/content/banner/index.vue')

  assert.match(agreement, /parseTime\(s\.row\.effectiveTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(banner, /parseTime\(scope\.row\.startTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(banner, /parseTime\(scope\.row\.endTime,\s*'\{y\}-\{m\}-\{d\}'\)/)
  assert.match(agreement, /type="datetime"/)
  assert.match(banner, /type="datetime"/)

  compile('src/views/library/agreement/index.vue', agreement)
  compile('src/views/library/content/banner/index.vue', banner)
})
