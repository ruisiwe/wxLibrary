<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['library:vip:operation']"
          type="primary"
          plain
          size="mini"
          @click="show('open')"
        >
          人工开通/续期
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['library:vip:operation']"
          type="warning"
          plain
          size="mini"
          @click="show('compensate')"
        >
          会员补偿
        </el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="load" />
    </el-row>

    <el-table :data="rows">
      <el-table-column prop="userId" label="用户编号" />
      <el-table-column prop="sourceType" label="来源" />
      <el-table-column prop="sourceBizNo" label="业务编号" min-width="220" />
      <el-table-column prop="oldExpireTime" label="原到期时间" />
      <el-table-column prop="newExpireTime" label="新到期时间" />
      <el-table-column prop="giftPoints" label="赠送积分" />
    </el-table>

    <el-dialog
      :title="mode === 'open' ? '人工开通/续期' : '会员补偿'"
      :visible.sync="visible"
      width="640px"
      @closed="resetDialog"
    >
      <el-form :model="form" label-width="110px">
        <el-form-item label="微信用户" required>
          <el-select
            v-model="form.userIds"
            multiple
            filterable
            remote
            reserve-keyword
            :multiple-limit="100"
            :remote-method="searchUsers"
            :loading="userLoading"
            placeholder="请输入昵称或用户编号"
            class="user-select"
            @change="handleUserChange"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="userLabel(user)"
              :value="user.id"
            >
              <div class="user-option">
                <el-avatar
                  :size="34"
                  :src="avatarUrl(user)"
                  icon="el-icon-user-solid"
                  @error="avatarFallback"
                />
                <div class="user-option__text">
                  <span>{{ user.nickname || '未设置昵称' }}</span>
                  <small>用户编号：{{ user.id }} · {{ expireLabel(user) }}</small>
                </div>
              </div>
            </el-option>
          </el-select>
          <div class="form-tip">支持按昵称或用户编号搜索，单次最多选择 100 位用户</div>
        </el-form-item>

        <el-form-item v-if="mode === 'open'" label="会员套餐" required>
          <el-select v-model="form.planId" placeholder="请选择会员套餐">
            <el-option
              v-for="plan in plans"
              :key="plan.id"
              :label="`${plan.planName}（赠送${plan.giftPoints}积分）`"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="补偿天数" required>
          <el-input-number v-model="form.days" :min="1" />
        </el-form-item>

        <el-form-item label="赠送积分">
          <el-tag>{{ mode === 'open' ? selectedGift : 0 }}</el-tag>
        </el-form-item>
        <el-form-item label="操作原因" required>
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入本次操作原因"
          />
        </el-form-item>
      </el-form>

      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  compensateVip,
  listEntitlements,
  listVipPlans,
  listVipUserOptions,
  openVip
} from '@/api/library/vip'

export default {
  name: 'LibraryVipEntitlement',
  data() {
    return {
      rows: [],
      plans: [],
      mode: 'open',
      visible: false,
      submitting: false,
      userLoading: false,
      userSearchToken: 0,
      userOptions: [],
      userOptionMap: {},
      form: this.emptyForm()
    }
  },
  computed: {
    selectedGift() {
      const plan = this.plans.find(item => item.id === this.form.planId)
      return plan ? plan.giftPoints : 0
    }
  },
  created() {
    this.load()
    listVipPlans({ status: '0', pageSize: 100 }).then(response => {
      this.plans = response.rows || []
    })
  },
  methods: {
    emptyForm() {
      return {
        userIds: [],
        planId: null,
        days: 1,
        batchNo: '',
        reason: ''
      }
    },
    load() {
      listEntitlements({ pageNum: 1, pageSize: 100 }).then(response => {
        this.rows = response.rows || []
      })
    },
    show(mode) {
      this.mode = mode
      this.form = this.emptyForm()
      this.form.batchNo = this.createBatchNo()
      this.userOptions = []
      this.userOptionMap = {}
      this.visible = true
      this.searchUsers('')
    },
    resetDialog() {
      this.userSearchToken++
      this.userLoading = false
      this.submitting = false
      this.userOptions = []
      this.userOptionMap = {}
      this.form = this.emptyForm()
    },
    searchUsers(keyword) {
      const token = ++this.userSearchToken
      this.userLoading = true
      listVipUserOptions({
        keyword: keyword || '',
        pageNum: 1,
        pageSize: 20
      }).then(response => {
        if (token !== this.userSearchToken) return
        this.mergeUserOptions(response.rows || [])
      }).finally(() => {
        if (token === this.userSearchToken) this.userLoading = false
      })
    },
    mergeUserOptions(users) {
      const options = {}
      this.form.userIds.forEach(id => {
        if (this.userOptionMap[id]) options[id] = this.userOptionMap[id]
      })
      users.forEach(user => {
        options[user.id] = user
      })
      this.userOptionMap = options
      this.userOptions = Object.keys(options).map(id => options[id])
    },
    handleUserChange() {
      this.mergeUserOptions(this.userOptions)
    },
    userLabel(user) {
      return `${user.nickname || '未设置昵称'}（用户编号：${user.id}）`
    },
    expireLabel(user) {
      if (!user.vipExpireTime) return '当前非VIP'
      const date = new Date(user.vipExpireTime)
      if (Number.isNaN(date.getTime())) return 'VIP到期时间未知'
      const pad = value => String(value).padStart(2, '0')
      return `VIP至 ${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
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
    createBatchNo() {
      const value = `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}00000000000000000000`
      return value.replace(/[^a-z0-9]/gi, '').slice(0, 20)
    },
    submit() {
      if (!this.form.userIds.length) {
        return this.$modal.msgError('请选择微信用户')
      }
      if (this.mode === 'open' && !this.form.planId) {
        return this.$modal.msgError('请选择会员套餐')
      }
      if (this.mode === 'compensate' && (!this.form.days || this.form.days < 1)) {
        return this.$modal.msgError('补偿天数必须大于0')
      }
      if (!this.form.reason || !this.form.reason.trim()) {
        return this.$modal.msgError('操作原因不能为空')
      }

      const payload = {
        userIds: this.form.userIds.slice(),
        batchNo: this.form.batchNo,
        reason: this.form.reason.trim()
      }
      if (this.mode === 'open') payload.planId = this.form.planId
      else payload.days = this.form.days

      this.submitting = true
      const request = this.mode === 'open' ? openVip : compensateVip
      request(payload).then(response => {
        const count = response.data && response.data.processedCount
          ? response.data.processedCount
          : payload.userIds.length
        this.$modal.msgSuccess(`已成功处理 ${count} 位用户`)
        this.visible = false
        this.load()
      }).finally(() => {
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

.user-select {
  width: 100%;
}

.user-option {
  display: flex;
  align-items: center;
  height: 48px;
}

.user-option__text {
  display: flex;
  flex-direction: column;
  margin-left: 10px;
  line-height: 20px;
}

.user-option__text small {
  color: #909399;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}

::v-deep .el-select-dropdown__item {
  height: auto;
}
</style>
