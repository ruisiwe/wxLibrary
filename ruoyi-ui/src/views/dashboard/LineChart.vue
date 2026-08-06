<template>
  <div class="chart-card">
    <div class="chart-title">近7日兑换文档数与活跃用户数</div>
    <div v-if="!hasData" class="empty-state">暂无数据</div>
    <div v-else ref="chart" :style="{ height: height, width: width }" />
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import resize from './mixins/resize'

export default {
  name: 'LineChart',
  mixins: [resize],
  props: {
    width: { type: String, default: '100%' },
    height: { type: String, default: '300px' },
    chartData: {
      type: Object,
      default: () => ({ dates: [], paidExchangeCounts: [], activeUserCounts: [] })
    }
  },
  data() {
    return { chart: null }
  },
  computed: {
    hasData() {
      return this.chartData && Array.isArray(this.chartData.dates) && this.chartData.dates.length > 0
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
      const data = this.chartData || {}
      this.chart.setOption({
        color: ['#409EFF', '#F56C6C'],
        tooltip: { trigger: 'axis' },
        legend: { top: 4, data: ['兑换文档数', '活跃用户数'] },
        grid: { left: 8, right: 12, bottom: 8, top: 46, containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: data.dates || [],
          axisTick: { show: false }
        },
        yAxis: { type: 'value', minInterval: 1, axisTick: { show: false } },
        series: [
          { name: '兑换文档数', type: 'line', smooth: true, data: data.paidExchangeCounts || [] },
          { name: '活跃用户数', type: 'line', smooth: true, data: data.activeUserCounts || [] }
        ]
      }, true)
    }
  }
}
</script>

<style scoped>
.chart-card { position: relative; }
.chart-title { color: #303133; font-size: 16px; font-weight: 600; }
.empty-state { display: flex; align-items: center; justify-content: center; height: 300px; color: #909399; }
</style>
