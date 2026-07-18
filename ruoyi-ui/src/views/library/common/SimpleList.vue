<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix"><span>{{ title }}</span><el-button class="pull-right" size="mini" icon="el-icon-refresh" @click="loadData">刷新</el-button></div>
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column v-for="column in columns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.width || 120" show-overflow-tooltip />
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="loadData" />
    </el-card>
  </div>
</template>
<script>
export default {
  name: 'LibrarySimpleList',
  props: { title: { type: String, required: true }, loader: { type: Function, required: true }, columns: { type: Array, required: true } },
  data() { return { loading: false, rows: [], total: 0, query: { pageNum: 1, pageSize: 10 } } },
  created() { this.loadData() },
  methods: {
    loadData() { this.loading = true; this.loader(this.query).then(res => { this.rows = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) }
  }
}
</script>
<style scoped>.pull-right { float: right; }</style>
