<template>
  <div class="chart-card">
    <div class="chart-title">近12个月各分类付费文档</div>
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
  name: 'BarChart',
  mixins: [resize],
  props: {
    width: { type: String, default: '100%' },
    height: { type: String, default: '360px' },
    chartData: {
      type: Object,
      default: () => ({ months: [], series: [] })
    }
  },
  data() {
    return { chart: null }
  },
  computed: {
    hasData() {
      return this.chartData && Array.isArray(this.chartData.series) && this.chartData.series.length > 0
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
      const series = this.chartData.series.map(item => ({
        name: item.categoryName,
        type: 'bar',
        stack: 'paidDocuments',
        barMaxWidth: 48,
        emphasis: { focus: 'series' },
        itemStyle: { color: categoryColor(item.categoryId) },
        data: item.values || []
      }))
      this.chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { type: 'scroll', top: 4 },
        grid: { left: 12, right: 18, bottom: 12, top: 48, containLabel: true },
        xAxis: { type: 'category', data: this.chartData.months || [], axisTick: { alignWithLabel: true } },
        yAxis: { type: 'value', minInterval: 1 },
        series
      }, true)
    }
  }
}
</script>

<style scoped>
.chart-card { position: relative; min-height: 380px; }
.chart-title { color: #303133; font-size: 16px; font-weight: 600; }
.empty-state { display: flex; align-items: center; justify-content: center; height: 360px; color: #909399; }
</style>
