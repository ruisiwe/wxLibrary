<template>
  <div class="chart-card">
    <div class="chart-title">各分类文档发送占比</div>
    <div v-if="!hasData" class="empty-state">暂无数据</div>
    <div v-else ref="chart" :style="{ height: height, width: width }" />
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import resize from './mixins/resize'
const { categoryColor } = require('./categoryColors')

export default {
  name: 'PieChart',
  mixins: [resize],
  props: {
    width: { type: String, default: '100%' },
    height: { type: String, default: '300px' },
    chartData: { type: Array, default: () => [] }
  },
  data() {
    return { chart: null }
  },
  computed: {
    hasData() {
      return Array.isArray(this.chartData) && this.chartData.some(item => Number(item.count) > 0)
    }
  },
  watch: {
    chartData: {
      deep: true,
      handler() { this.$nextTick(this.renderChart) }
    }
  },
  mounted() {
    this.$nextTick(this.renderChart)
  },
  beforeDestroy() {
    if (this.chart) this.chart.dispose()
    this.chart = null
  },
  methods: {
    renderChart() {
      if (!this.hasData || !this.$refs.chart) {
        if (this.chart) this.chart.dispose()
        this.chart = null
        return
      }
      if (!this.chart) this.chart = echarts.init(this.$refs.chart, 'macarons')
      const values = this.chartData
        .filter(item => Number(item.count) > 0)
        .map(item => ({
          name: item.categoryName,
          value: Number(item.count),
          itemStyle: { color: categoryColor(item.categoryId) }
        }))
      this.chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 次（{d}%）' },
        legend: { type: 'scroll', bottom: 0 },
        series: [{
          name: '发送次数',
          type: 'pie',
          radius: ['38%', '68%'],
          center: ['50%', '46%'],
          label: { formatter: '{b}\n{d}%' },
          data: values
        }]
      }, true)
    }
  }
}
</script>

<style scoped>
.chart-card { position: relative; min-height: 320px; }
.chart-title { color: #303133; font-size: 16px; font-weight: 600; }
.empty-state { display: flex; align-items: center; justify-content: center; height: 300px; color: #909399; }
</style>
