<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <right-toolbar :search="false" @queryTable="load" />
    </el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="openidMasked" label="OpenID（脱敏）" min-width="160" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="pointBalance" label="积分" />
      <el-table-column label="会员到期时间" min-width="180">
        <template slot-scope="scope">
          {{ scope.row.vipExpireTime ? parseTime(scope.row.vipExpireTime, '{y}-{m}-{d}') : '未开通' }}
        </template>
      </el-table-column>
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
    <el-dialog
      title="微信用户详情"
      :visible.sync="detailVisible"
      width="720px"
      append-to-body
    >
      <div class="detail-profile">
        <el-avatar
          :size="80"
          :src="avatarUrl(detail)"
          icon="el-icon-user-solid"
          @error="avatarFallback"
        />
        <div class="detail-profile__meta">
          <div class="detail-nickname">{{ detail.nickname || '未设置昵称' }}</div>
          <div class="detail-number">用户编号：{{ detail.id || '-' }}</div>
        </div>
        <el-tag :type="detail.status === '0' ? 'success' : 'info'" effect="plain">
          {{ detail.status === '0' ? '启用' : '停用' }}
        </el-tag>
      </div>

      <el-descriptions border :column="2" class="detail-descriptions">
        <el-descriptions-item label="OpenID（脱敏）">
          {{ detail.openidMasked || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="积分余额">
          {{ detail.pointBalance == null ? 0 : detail.pointBalance }}
        </el-descriptions-item>
        <el-descriptions-item label="会员状态">
          <div class="member-status">
            <el-tag :type="detailVipTagType" size="small">
              {{ detailVipLabel }}
            </el-tag>
            <el-button
              v-if="detail.status === '0'"
              v-hasPermi="['library:vip:operation']"
              type="text"
              class="member-action"
              @click="openVipDialog"
            >
              {{ detailVipActionText }}
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="会员到期">
          {{ formatVipExpire(detail.vipExpireTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="最后登录" :span="2">
          {{ formatDateTime(detail.lastLoginTime) }}
        </el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :title="detailVipState === 'active' ? '续期会员' : '开通会员'"
      :visible.sync="vipVisible"
      width="560px"
      append-to-body
      @closed="resetVipDialog"
    >
      <div class="vip-user-summary">
        <el-avatar
          :size="48"
          :src="avatarUrl(detail)"
          icon="el-icon-user-solid"
          @error="avatarFallback"
        />
        <div>
          <div class="vip-user-summary__name">{{ detail.nickname || '未设置昵称' }}</div>
          <div class="vip-user-summary__id">用户编号：{{ detail.id }}</div>
        </div>
      </div>

      <el-form :model="vipForm" label-width="90px">
        <el-form-item label="会员套餐" required>
          <el-select
            v-model="vipForm.planId"
            :loading="vipPlanLoading"
            placeholder="请选择会员套餐"
            class="vip-plan-select"
          >
            <el-option
              v-for="plan in vipPlans"
              :key="plan.id"
              :label="planOptionLabel(plan)"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedVipPlan" label="套餐权益">
          <div class="vip-plan-summary">
            <span>有效期 {{ selectedVipPlan.validDays }} 天</span>
            <span>赠送 {{ selectedVipPlan.giftPoints || 0 }} 积分</span>
            <span>¥{{ formatPlanPrice(selectedVipPlan.priceCent) }}</span>
          </div>
        </el-form-item>
        <el-form-item label="操作原因" required>
          <el-input
            v-model="vipForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入本次开通或续期原因"
          />
        </el-form-item>
      </el-form>

      <span slot="footer">
        <el-button @click="vipVisible = false">取消</el-button>
        <el-button type="primary" :loading="vipSubmitting" @click="submitVip">
          确认{{ detailVipState === 'active' ? '续期' : '开通' }}
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import { listUsers, getUser, changeUserStatus, adjustUserPoints } from '@/api/library/user'
import { listVipPlans, openVip } from '@/api/library/vip'

export default {
  name: 'LibraryWxUser',
  data() {
    return {
      loading: false,
      submitting: false,
      vipSubmitting: false,
      vipPlanLoading: false,
      rows: [],
      total: 0,
      query: { pageNum: 1, pageSize: 10 },
      visible: false,
      detailVisible: false,
      vipVisible: false,
      detail: {},
      selected: null,
      form: {},
      vipPlans: [],
      vipForm: {
        userIds: [],
        planId: null,
        batchNo: '',
        reason: ''
      }
    }
  },
  computed: {
    detailVipState() {
      if (!this.detail.vipExpireTime) return 'inactive'
      const expireTime = new Date(this.detail.vipExpireTime).getTime()
      if (Number.isNaN(expireTime)) return 'inactive'
      return expireTime > Date.now() ? 'active' : 'expired'
    },
    detailVipLabel() {
      if (this.detailVipState === 'active') return 'VIP会员'
      if (this.detailVipState === 'expired') return '已过期'
      return '未开通'
    },
    detailVipTagType() {
      if (this.detailVipState === 'active') return 'success'
      if (this.detailVipState === 'expired') return 'warning'
      return 'info'
    },
    detailVipActionText() {
      return this.detailVipState === 'active' ? '续期会员' : '开通会员'
    },
    selectedVipPlan() {
      return this.vipPlans.find(plan => plan.id === this.vipForm.planId) || null
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      listUsers(this.query).then(res => {
        this.rows = res.rows || []
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    showDetail(row) {
      getUser(row.id).then(res => {
        this.detail = res.data || {}
        this.detailVisible = true
      })
    },
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
    avatarUrl(user) {
      if (!user || !user.avatarPath) return ''
      const path = String(user.avatarPath)
        .split('/')
        .map(part => encodeURIComponent(part))
        .join('/')
      return `${process.env.VUE_APP_BASE_API}/wx/public/avatar/${path}`
    },
    avatarFallback() {
      return true
    },
    formatDateTime(value) {
      return value ? this.parseTime(value, '{y}-{m}-{d}') : '-'
    },
    formatVipExpire(value) {
      return value ? this.formatDateTime(value) : '未开通'
    },
    formatPlanPrice(value) {
      return (Number(value || 0) / 100).toFixed(2)
    },
    planOptionLabel(plan) {
      return `${plan.planName}（${plan.validDays}天，¥${this.formatPlanPrice(plan.priceCent)}，赠送${plan.giftPoints || 0}积分）`
    },
    openVipDialog() {
      this.vipForm = {
        userIds: [this.detail.id],
        planId: null,
        batchNo: this.createBatchNo(),
        reason: ''
      }
      this.vipVisible = true
      this.vipPlanLoading = true
      listVipPlans({ status: '0', pageNum: 1, pageSize: 100 }).then(response => {
        this.vipPlans = response.rows || []
      }).finally(() => {
        this.vipPlanLoading = false
      })
    },
    resetVipDialog() {
      this.vipSubmitting = false
      this.vipPlanLoading = false
      this.vipPlans = []
      this.vipForm = {
        userIds: [],
        planId: null,
        batchNo: '',
        reason: ''
      }
    },
    submitVip() {
      if (!this.vipForm.planId) return this.$modal.msgError('请选择会员套餐')
      if (!this.vipForm.reason || !this.vipForm.reason.trim()) {
        return this.$modal.msgError('操作原因不能为空')
      }

      const payload = {
        userIds: this.vipForm.userIds.slice(),
        planId: this.vipForm.planId,
        batchNo: this.vipForm.batchNo,
        reason: this.vipForm.reason.trim()
      }
      this.vipSubmitting = true
      openVip(payload).then(() => {
        this.$modal.msgSuccess(this.detailVipState === 'active' ? '会员续期成功' : '会员开通成功')
        this.vipVisible = false
        return getUser(this.detail.id)
      }).then(response => {
        this.detail = response.data || {}
        this.load()
      }).finally(() => {
        this.vipSubmitting = false
      })
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
<style scoped>
.mt16 {
  margin-top: 16px;
}

.detail-profile {
  display: flex;
  align-items: center;
  padding: 4px 4px 20px;
}

.detail-profile__meta {
  flex: 1;
  min-width: 0;
  margin-left: 16px;
}

.detail-nickname {
  overflow: hidden;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
  line-height: 30px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-number,
.vip-user-summary__id {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.detail-descriptions {
  margin-top: 4px;
}

.member-status {
  display: flex;
  align-items: center;
}

.member-action {
  margin-left: 10px;
  padding: 0;
}

.vip-user-summary {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.vip-user-summary > div {
  margin-left: 12px;
}

.vip-user-summary__name {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.vip-plan-select {
  width: 100%;
}

.vip-plan-summary {
  display: flex;
  flex-wrap: wrap;
  color: #606266;
  line-height: 24px;
}

.vip-plan-summary span {
  margin-right: 18px;
}
</style>
