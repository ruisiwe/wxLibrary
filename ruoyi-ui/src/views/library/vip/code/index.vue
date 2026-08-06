<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['library:vip:code']" type="primary" plain size="mini" @click="openGenerate">批量生成会员码</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="$refs.vipCodeList.loadData()" />
    </el-row>
    <simple-list ref="vipCodeList" plain embedded title="会员码记录（仅显示掩码）" :loader="listVipCodes" :columns="columns"/>
    <el-dialog title="批量生成会员码" :closeOnClickModal="false" :visible.sync="visible" width="640px">
      <el-form label-width="100px">
        <el-form-item label="会员套餐" required>
          <el-select
            v-model="form.planId"
            :loading="planLoading"
            placeholder="请选择会员套餐"
            class="plan-select"
          >
            <el-option
              v-for="plan in plans"
              :key="plan.id"
              :label="planOptionLabel(plan)"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量" required><el-input-number v-model="form.count" :min="1" :max="1000"/></el-form-item>
        <el-form-item label="过期时间"><el-date-picker v-model="form.expiresTime" type="datetime" placeholder="不填则长期有效"/></el-form-item>
      </el-form>
      <el-alert v-if="codes.length" title="以下明文只展示一次，请立即安全保存后再关闭弹窗。" type="warning" :closable="false"/>
      <el-input v-if="codes.length" :value="codes.join('\n')" type="textarea" :rows="10" readonly class="mt16"/>
      <span slot="footer">
        <el-button @click="visible=false">关闭</el-button>
        <el-button v-if="!codes.length" type="primary" @click="generate">生成</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import SimpleList from '@/views/library/common/SimpleList'
import { listVipCodes, generateVipCodes, listVipPlans } from '@/api/library/vip'
export default {
  name: 'LibraryVipCode',
  components: { SimpleList },
  data() {
    return {
      listVipCodes,
      visible: false,
      planLoading: false,
      plans: [],
      form: { planId: null, count: 10, expiresTime: null },
      codes: [],
      columns: [
        { prop: 'codeMask', label: '会员码掩码' },
        { prop: 'planId', label: '套餐编号' },
        { prop: 'batchNo', label: '批次号', width: 220 },
        { prop: 'status', label: '状态', options: [
          { value: 'UNUSED', label: '未使用' },
          { value: 'USED', label: '已使用' },
          { value: 'DISABLED', label: '已禁用' }
        ] },
        { prop: 'usedUserId', label: '使用用户' },
        { prop: 'vipEntitlementId', label: '权益编号' }
      ]
    }
  },
  methods: {
    openGenerate() {
      this.codes = []
      this.plans = []
      this.form = { planId: null, count: 10, expiresTime: null }
      this.visible = true
      this.planLoading = true
      listVipPlans({ status: '0', pageNum: 1, pageSize: 100 }).then(response => {
        this.plans = response.rows || []
      }).finally(() => {
        this.planLoading = false
      })
    },
    formatPlanPrice(value) {
      return (Number(value || 0) / 100).toFixed(2)
    },
    planOptionLabel(plan) {
      return `${plan.planName}（${plan.validDays}天，¥${this.formatPlanPrice(plan.priceCent)}，赠送${plan.giftPoints || 0}积分）`
    },
    generate() {
      if (!this.form.planId) return this.$modal.msgError('请选择会员套餐')
      generateVipCodes(this.form).then(response => {
        this.codes = (response.data && response.data.plaintextCodes) || []
      })
    }
  }
}
</script>
<style scoped>
.mt16 {
  margin-top: 16px;
}

.plan-select {
  width: 100%;
}
</style>
