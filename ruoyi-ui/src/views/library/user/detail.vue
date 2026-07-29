<template>
  <div class="app-container">
    <el-page-header content="微信用户详情" @back="$router.back()" />
    <el-descriptions v-loading="loading" border :column="2" class="mt16">
      <el-descriptions-item label="用户编号">{{ user.id }}</el-descriptions-item>
      <el-descriptions-item label="OpenID（脱敏）">{{ user.openidMasked }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
      <el-descriptions-item label="积分余额">{{ user.pointBalance }}</el-descriptions-item>
      <el-descriptions-item label="会员到期时间">
        {{ user.vipExpireTime ? parseTime(user.vipExpireTime, '{y}-{m}-{d}') : '未开通' }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">{{ user.status === '0' ? '启用' : '停用' }}</el-descriptions-item>
      <el-descriptions-item label="最后登录时间">
        {{ user.lastLoginTime ? parseTime(user.lastLoginTime, '{y}-{m}-{d}') : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="头像路径">{{ user.avatarPath }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>
<script>
import { getUser } from '@/api/library/user'
export default {
  name: 'LibraryWxUserDetail',
  data() { return { loading: false, user: {} } },
  created() { this.loading = true; getUser(this.$route.params.id).then(res => { this.user = res.data || {} }).finally(() => { this.loading = false }) }
}
</script>
<style scoped>.mt16{margin-top:16px}</style>
