<template>
  <div class="chart-card">
    <div class="chart-title">各分类文档数</div>
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
  name: 'RaddarChart',
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
    hasData() { return Array.isArray(this.chartData) && this.chartData.length > 0 }
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
      const maximum = Math.max(1, ...this.chartData.map(item => Number(item.count) || 0))
      this.chart.setOption({
        color: ['#409EFF'],
        tooltip: { trigger: 'item' },
        radar: {
          radius: '62%',
          center: ['50%', '52%'],
          indicator: this.chartData.map(item => ({
            name: item.categoryName,
            max: maximum,
            color: categoryColor(item.categoryId)
          }))
        },
        series: [{
          name: '文档数',
          type: 'radar',
          data: [{ value: this.chartData.map(item => Number(item.count) || 0), name: '文档数' }],
          areaStyle: { opacity: 0.2 }
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
