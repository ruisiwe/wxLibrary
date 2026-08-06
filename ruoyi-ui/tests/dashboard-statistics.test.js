const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')
const { categoryColor } = require('../src/views/dashboard/categoryColors')

const root = path.resolve(__dirname, '..')
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8')
const api = read('src/api/library/dashboard.js')
const index = read('src/views/index.vue')
const panel = read('src/views/dashboard/PanelGroup.vue')
const line = read('src/views/dashboard/LineChart.vue')
const radar = read('src/views/dashboard/RaddarChart.vue')
const pie = read('src/views/dashboard/PieChart.vue')
const bar = read('src/views/dashboard/BarChart.vue')

assert(api.includes("url: '/library/dashboard'"), '首页 API 应请求文库统计聚合接口')
assert(index.includes('getDashboardStatistics'), '首页应加载真实聚合数据')
assert(index.includes('<panel-group :summary="dashboard.summary"'), '顶部指标应绑定真实数据')
assert(index.includes('<bar-chart :chart-data="dashboard.monthlyPaidExchanges"'), '中部应展示十二月堆叠柱')
assert(index.includes('<line-chart :chart-data="dashboard.sevenDayTrend"'), '下左应展示七日双折线')
assert(index.includes('<raddar-chart :chart-data="dashboard.categoryDocumentCounts"'), '下中应展示分类雷达图')
assert(index.includes('<pie-chart :chart-data="dashboard.categorySendShares"'), '下右应展示发送占比')
assert(index.indexOf('<bar-chart') < index.indexOf('<line-chart'), '十二月堆叠柱应位于七日趋势上方')
assert(index.indexOf('<line-chart') < index.indexOf('<raddar-chart'), '七日趋势应位于底部左侧')
assert(index.indexOf('<raddar-chart') < index.indexOf('<pie-chart'), '发送占比应位于底部右侧')
assert(index.includes("this.$modal.msgError('首页统计数据加载失败')"), '加载失败应显示简体中文提示')
assert(!index.includes('newVisitis') && !index.includes('expectedData'), '首页不得保留示例数据')

assert(panel.includes('用户数') && panel.includes('会员数') && panel.includes('文档数') &&
  panel.includes('付费文档数'), '顶部应显示四项业务指标')
assert(line.includes('兑换文档数') && line.includes('活跃用户数'), '双折线图例应使用已确认中文名称')
assert(line.includes('暂无数据'), '双折线无数据时应显示空状态')
assert(line.includes('handler() { this.$nextTick(this.renderChart) }'),
  '双折线从空数据切换到有数据时应等待图表容器渲染')
assert(radar.includes('各分类文档数'), '雷达图应显示中文标题')
assert(pie.includes('各分类文档发送占比') && pie.includes('暂无数据'), '饼图应包含标题和空状态')
assert(bar.includes('近12个月各分类付费文档') && bar.includes("stack: 'paidDocuments'"),
  '柱状图应按分类堆叠展示十二月付费文档')
assert.strictEqual(categoryColor(2), categoryColor(2), '同一分类应始终使用相同颜色')
assert.notStrictEqual(categoryColor(1), categoryColor(2), '相邻分类应使用不同颜色')
assert(bar.includes('categoryColor(item.categoryId)'), '柱图颜色应由分类编号决定')
assert(pie.includes('categoryColor(item.categoryId)'), '饼图颜色应由分类编号决定')
assert(radar.includes('categoryColor(item.categoryId)'), '雷达图分类标签颜色应由分类编号决定')

for (const [name, source] of [
  ['index.vue', index],
  ['PanelGroup.vue', panel],
  ['LineChart.vue', line],
  ['RaddarChart.vue', radar],
  ['PieChart.vue', pie],
  ['BarChart.vue', bar]
]) {
  const component = compiler.parseComponent(source)
  assert(component.template && component.template.content, `${name} 应包含模板`)
  const compiled = compiler.compile(component.template.content)
  assert.deepStrictEqual(compiled.errors, [], `${name} 模板编译失败：${compiled.errors.join('；')}`)
}

console.log('后台首页统计图表契约测试通过')
