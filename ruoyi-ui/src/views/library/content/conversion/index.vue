<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="任务编号" />
      <el-table-column prop="documentId" label="文档编号" />
      <el-table-column prop="taskVersion" label="版本" />
      <el-table-column prop="taskStatus" label="状态" />
      <el-table-column prop="pageCount" label="页数" />
      <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template slot-scope="scope">
          <el-button v-if="scope.row.taskStatus === 'PENDING'" v-hasPermi="['library:document:convert']" type="text" @click="run(scope.row, false)">执行</el-button>
          <el-button v-if="scope.row.taskStatus === 'FAILED'" v-hasPermi="['library:document:convert']" type="text" @click="run(scope.row, true)">重试</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
  </div>
</template>
<script>
import { listConversions, executeConversion, retryConversion } from '@/api/library/content'
export default {
  name: 'LibraryConversion',
  data() { return { loading: false, rows: [], total: 0, query: { pageNum: 1, pageSize: 10 } } },
  created() { this.load() },
  methods: {
    load() { this.loading = true; listConversions(this.query).then(res => { this.rows = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    run(row, retry) {
      const action = retry ? '重试' : '执行'
      this.$modal.confirm(`确认${action}转换任务 ${row.id} 吗？`).then(() => (retry ? retryConversion : executeConversion)(row.id)).then(() => { this.$modal.msgSuccess(`${action}请求已完成`); this.load() })
    }
  }
}
</script>
