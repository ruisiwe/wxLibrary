<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['library:vip:code']" type="primary" plain size="mini" @click="openGenerate">批量生成会员码</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="$refs.vipCodeList.loadData()" />
    </el-row>
    <simple-list ref="vipCodeList" plain embedded title="会员码记录（仅显示掩码）" :loader="listVipCodes" :columns="columns"/>
    <el-dialog title="批量生成会员码" :visible.sync="visible" width="640px">
      <el-form label-width="100px">
        <el-form-item label="套餐编号" required><el-input-number v-model="form.planId" :min="1"/></el-form-item>
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
import { listVipCodes, generateVipCodes } from '@/api/library/vip'
export default {
  name: 'LibraryVipCode',
  components: { SimpleList },
  data() {
    return {
      listVipCodes,
      visible: false,
      form: { planId: 1, count: 10, expiresTime: null },
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
      this.form = { planId: 1, count: 10, expiresTime: null }
      this.visible = true
    },
    generate() {
      generateVipCodes(this.form).then(response => {
        this.codes = (response.data && response.data.plaintextCodes) || []
      })
    }
  }
}
</script>
<style scoped>.mt16{margin-top:16px}</style>
