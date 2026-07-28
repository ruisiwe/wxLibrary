<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="openidMasked" label="OpenID（脱敏）" min-width="160" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="pointBalance" label="积分" />
      <el-table-column prop="vipExpireTime" label="会员到期时间" min-width="180" />
      <el-table-column prop="status" label="状态"><template slot-scope="scope">{{ scope.row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template slot-scope="scope">
          <el-button v-hasPermi="['library:wxUser:query']" type="text" @click="showDetail(scope.row)">详情</el-button>
          <el-button v-hasPermi="['library:wxUser:edit']" type="text" @click="toggle(scope.row)">{{ scope.row.status === '0' ? '停用' : '启用' }}</el-button>
          <el-button v-hasPermi="['library:wxUser:points']" type="text" @click="openPoints(scope.row)">调整积分</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />

    <el-dialog title="人工调整积分" :visible.sync="visible" width="520px">
      <el-alert title="扣减积分不能使余额小于 0，系统会自动记录本次操作编号。" type="warning" :closable="false" />
      <el-form :model="form" label-width="100px" class="mt16">
        <el-form-item label="调整数量" required><el-input-number v-model="form.amount" /></el-form-item>
        <el-form-item label="调整原因" required><el-input v-model="form.description" type="textarea" maxlength="200" /></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitPoints">确认调整</el-button></span>
    </el-dialog>
    <el-dialog title="微信用户详情" :visible.sync="detailVisible" width="650px">
      <el-descriptions border :column="2">
        <el-descriptions-item label="用户编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="OpenID（脱敏）">{{ detail.openidMasked }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="积分余额">{{ detail.pointBalance }}</el-descriptions-item>
        <el-descriptions-item label="会员到期">{{ detail.vipExpireTime || '未开通' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === '0' ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ detail.lastLoginTime }}</el-descriptions-item>
        <el-descriptions-item label="头像路径">{{ detail.avatarPath }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
<script>
import { listUsers, getUser, changeUserStatus, adjustUserPoints } from '@/api/library/user'
export default {
  name: 'LibraryWxUser',
  data() { return { loading: false, submitting: false, rows: [], total: 0, query: { pageNum: 1, pageSize: 10 }, visible: false, detailVisible: false, detail: {}, selected: null, form: {} } },
  created() { this.load() },
  methods: {
    load() { this.loading = true; listUsers(this.query).then(res => { this.rows = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    showDetail(row) { getUser(row.id).then(res => { this.detail = res.data || {}; this.detailVisible = true }) },
    toggle(row) {
      const status = row.status === '0' ? '1' : '0'
      const action = status === '1' ? '停用' : '启用'
      this.$modal.confirm(`确认${action}用户“${row.nickname || row.id}”吗？`).then(() => changeUserStatus(row.id, status)).then(() => { this.$modal.msgSuccess(`${action}成功`); this.load() })
    },
    openPoints(row) {
      this.selected = row
      this.form = {
        amount: 0,
        batchNo: this.createBatchNo(),
        description: ''
      }
      this.visible = true
    },
    createBatchNo() {
      const value = `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}00000000000000000000`
      return value.replace(/[^a-z0-9]/gi, '').slice(0, 20)
    },
    submitPoints() {
      if (!this.form.amount || !this.form.description || !this.form.description.trim()) return this.$modal.msgError('请完整填写调整数量和调整原因')
      this.$modal.confirm(`确认将用户积分调整 ${this.form.amount} 吗？`)
        .then(() => {
          this.submitting = true
          return adjustUserPoints(this.selected.id, this.form)
        })
        .then(() => {
          this.$modal.msgSuccess('积分调整成功')
          this.visible = false
          this.load()
        })
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>
<style scoped>.mt16{margin-top:16px}</style>
