const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

function loadComponent() {
  const source = fs.readFileSync(
    path.resolve(__dirname, '../components/promotion-strip/index.js'),
    'utf8'
  )
  let definition
  const navigations = []
  vm.runInNewContext(source, {
    Component: value => { definition = value },
    wx: { navigateTo: options => navigations.push(options) }
  })
  return { definition, navigations }
}

test('点击轮播图跳转到关联文档详情页', () => {
  const { definition, navigations } = loadComponent()
  definition.methods.open({
    currentTarget: { dataset: { item: { documentId: 12 } } }
  })
  assert.equal(navigations.length, 1)
  assert.equal(navigations[0].url, '/pages/document-detail/document-detail?id=12')
})

test('关联文档编号无效时不发起跳转', () => {
  const invalidValues = [undefined, null, '', 0, -1, 'abc']
  invalidValues.forEach(documentId => {
    const { definition, navigations } = loadComponent()
    definition.methods.open({
      currentTarget: { dataset: { item: { documentId } } }
    })
    assert.equal(navigations.length, 0)
  })
})
