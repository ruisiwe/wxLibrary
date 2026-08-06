<template>
  <div v-loading="loading" class="dashboard-editor-container">
    <panel-group :summary="dashboard.summary" />

    <el-row class="chart-row">
      <div class="chart-wrapper chart-wrapper--large">
        <bar-chart :chart-data="dashboard.monthlyPaidExchanges" />
      </div>
    </el-row>

    <el-row :gutter="24">
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <line-chart :chart-data="dashboard.sevenDayTrend" />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <raddar-chart :chart-data="dashboard.categoryDocumentCounts" />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <pie-chart :chart-data="dashboard.categorySendShares" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDashboardStatistics } from '@/api/library/dashboard'
import PanelGroup from './dashboard/PanelGroup'
import LineChart from './dashboard/LineChart'
import RaddarChart from './dashboard/RaddarChart'
import PieChart from './dashboard/PieChart'
import BarChart from './dashboard/BarChart'

function emptyDashboard() {
  return {
    summary: {
      userCount: 0,
      memberCount: 0,
      documentCount: 0,
      paidDocumentCount: 0
    },
    monthlyPaidExchanges: { months: [], series: [] },
    sevenDayTrend: { dates: [], paidExchangeCounts: [], activeUserCounts: [] },
    categoryDocumentCounts: [],
    categorySendShares: []
  }
}

export default {
  name: 'Index',
  components: {
    PanelGroup,
    LineChart,
    RaddarChart,
    PieChart,
    BarChart
  },
  data() {
    return {
      loading: false,
      dashboard: emptyDashboard()
    }
  },
  created() {
    this.loadDashboard()
  },
  methods: {
    loadDashboard() {
      this.loading = true
      getDashboardStatistics().then(response => {
        this.dashboard = response.data || emptyDashboard()
      }).catch(() => {
        this.dashboard = emptyDashboard()
        this.$modal.msgError('首页统计数据加载失败')
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-editor-container {
  min-height: calc(100vh - 84px);
  padding: 24px;
  background-color: #f0f2f5;

  .chart-row {
    margin-bottom: 24px;
  }

  .chart-wrapper {
    margin-bottom: 24px;
    padding: 16px;
    background: #fff;
    border-radius: 6px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  }

  .chart-wrapper--large {
    padding: 18px 20px 8px;
  }
}

@media (max-width: 1024px) {
  .dashboard-editor-container {
    padding: 16px;

    .chart-wrapper {
      padding: 12px;
    }
  }
}
</style>
