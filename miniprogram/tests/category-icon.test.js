const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const componentJson = JSON.parse(fs.readFileSync(
  path.join(root, 'components/category-grid/index.json'), 'utf8'))
const template = fs.readFileSync(
  path.join(root, 'components/category-grid/index.wxml'), 'utf8')
const options = JSON.parse(fs.readFileSync(
  path.resolve(root, '../ruoyi-wechat-library/src/main/resources/library/category-icons.json'),
  'utf8'))

function readIconCss() {
  const candidates = [
    path.join(root, 'miniprogram_npm/tdesign-miniprogram/icon/icon.wxss'),
    path.join(root, 'node_modules/tdesign-miniprogram/miniprogram_dist/icon/icon.wxss')
  ]
  const iconCssPath = candidates.find(file => fs.existsSync(file))
  assert.ok(iconCssPath, '应能读取当前小程序 TDesign 图标样式文件')
  return fs.readFileSync(iconCssPath, 'utf8')
}

test('分类宫格使用接口 icon 字段渲染 TDesign 图标', () => {
  assert.equal(componentJson.usingComponents['t-icon'], 'tdesign-miniprogram/icon/icon')
  assert.match(template, /<t-icon/)
  assert.match(template, /grid__icon-shell/)
  assert.match(template, /name="\{\{item\.icon \|\| 'file'\}\}"/)
  assert.doesNotMatch(template, /item\.iconUrl/)
  assert.doesNotMatch(template, /<image[^>]+grid__icon/)
})

test('分类图标按设计草图使用精致图标外壳和网格分割', () => {
  const style = fs.readFileSync(
    path.join(root, 'components/category-grid/index.wxss'), 'utf8')
  assert.match(style, /\.grid__icon-shell/)
  assert.match(style, /box-shadow/)
  assert.match(style, /border-right/)
  assert.match(style, /border-bottom/)
  assert.match(style, /\.grid__label/)
})

test('所有后台精选图标都存在于当前小程序 TDesign 版本', () => {
  const iconCss = readIconCss()
  assert.equal(options.length, 24)
  for (const option of options) {
    const escaped = option.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    assert.match(iconCss, new RegExp(`\\.t-icon-${escaped}:before\\s*\\{`),
      `小程序当前 TDesign 版本缺少图标：${option.name}`)
  }
})
