<template>
  <div class="app-container">
    <el-card v-loading="configLoading" class="config-card" shadow="never">
      <div slot="header">
        <span>客服微信配置</span>
      </div>
      <el-form
        ref="configForm"
        :model="configForm"
        :rules="configRules"
        label-width="120px"
      >
        <el-form-item label="客服微信图片">
          <div class="image-editor">
            <div v-if="displayImageUrl" class="image-preview">
              <img :src="displayImageUrl" alt="客服微信图片">
            </div>
            <div v-else class="image-placeholder">尚未上传客服微信图片</div>
            <el-upload
              ref="imageUpload"
              action="#"
              accept=".jpg,.jpeg,.png,.webp"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleImageChange"
            >
              <el-button size="small" type="primary" plain>
                {{ displayImageUrl ? '更换图片' : '选择图片' }}
              </el-button>
            </el-upload>
            <div class="upload-tip">支持 JPEG、PNG、WebP，图片大小不能超过 2 MB</div>
          </div>
        </el-form-item>
        <el-form-item label="客服提示语" prop="customerServiceTip">
          <el-input
            v-model="configForm.customerServiceTip"
            maxlength="100"
            show-word-limit
            placeholder="开通 VIP 请添加客服微信"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            v-hasPermi="['library:vip:page-config:edit']"
            type="primary"
            :loading="configSaving"
            @click="saveConfig"
          >保存客服配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="benefit-card" shadow="never">
      <div slot="header" class="card-header">
        <span>权益文字列表</span>
        <el-button
          v-hasPermi="['library:vip:benefit:add']"
          type="primary"
          size="mini"
          icon="el-icon-plus"
          @click="openAdd"
        >新增权益</el-button>
      </div>

      <el-table v-loading="benefitLoading" :data="benefits">
        <el-table-column prop="benefitText" label="权益文字" min-width="260" />
        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
              {{ scope.row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="scope">
            <el-button
              v-hasPermi="['library:vip:benefit:edit']"
              type="text"
              @click="openEdit(scope.row)"
            >修改</el-button>
            <el-button
              v-hasPermi="['library:vip:benefit:remove']"
              type="text"
              class="danger"
              @click="removeBenefit(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="benefitForm.id ? '修改 VIP 权益' : '新增 VIP 权益'"
      :visible.sync="benefitVisible"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="benefitForm"
        :model="benefitForm"
        :rules="benefitRules"
        label-width="90px"
      >
        <el-form-item label="权益文字" prop="benefitText">
          <el-input
            v-model="benefitForm.benefitText"
            maxlength="100"
            show-word-limit
            placeholder="请输入权益文字"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="benefitForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="benefitForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="benefitVisible = false">取消</el-button>
        <el-button type="primary" :loading="benefitSaving" @click="saveBenefit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listVipBenefits,
  addVipBenefit,
  updateVipBenefit,
  deleteVipBenefit,
  getVipPageConfig,
  updateVipPageConfig
} from '@/api/library/vipBenefit'

const defaultConfig = () => ({
  customerServiceTip: '开通 VIP 请添加客服微信',
  customerServiceImageUrl: ''
})

const defaultBenefit = () => ({
  id: null,
  benefitText: '',
  sortOrder: 0,
  status: '0'
})

export default {
  name: 'LibraryVipBenefitIntroduction',
  data() {
    return {
      configLoading: false,
      configSaving: false,
      configForm: defaultConfig(),
      selectedImage: null,
      localImageUrl: '',
      benefits: [],
      benefitLoading: false,
      benefitVisible: false,
      benefitSaving: false,
      benefitForm: defaultBenefit(),
      configRules: {
        customerServiceTip: [
          { required: true, message: '请输入客服提示语', trigger: 'blur' },
          { max: 100, message: '客服提示语不能超过100个字符', trigger: 'blur' }
        ]
      },
      benefitRules: {
        benefitText: [
          { required: true, message: '请输入权益文字', trigger: 'blur' },
          { max: 100, message: '权益文字不能超过100个字符', trigger: 'blur' }
        ],
        sortOrder: [{ required: true, message: '请输入排序', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    displayImageUrl() {
      return this.localImageUrl || this.configForm.customerServiceImageUrl
    }
  },
  created() {
    this.loadConfig()
    this.loadBenefits()
  },
  beforeDestroy() {
    this.releaseLocalImage()
  },
  methods: {
    loadConfig() {
      this.configLoading = true
      return getVipPageConfig()
        .then(response => {
          this.configForm = { ...defaultConfig(), ...(response.data || {}) }
        })
        .finally(() => { this.configLoading = false })
    },
    loadBenefits() {
      this.benefitLoading = true
      return listVipBenefits({ pageNum: 1, pageSize: 100 })
        .then(response => { this.benefits = response.rows || [] })
        .finally(() => { this.benefitLoading = false })
    },
    handleImageChange(file) {
      const raw = file && file.raw
      if (!raw) return
      const allowed = ['image/jpeg', 'image/png', 'image/webp']
      if (!allowed.includes(raw.type)) {
        this.$modal.msgError('客服微信图片仅支持JPEG、PNG或WebP格式')
        this.$refs.imageUpload.clearFiles()
        return
      }
      if (raw.size > 2 * 1024 * 1024) {
        this.$modal.msgError('客服微信图片不能超过2MB')
        this.$refs.imageUpload.clearFiles()
        return
      }
      this.releaseLocalImage()
      this.selectedImage = raw
      this.localImageUrl = URL.createObjectURL(raw)
      this.$refs.imageUpload.clearFiles()
    },
    saveConfig() {
      if (this.configSaving) return
      this.$refs.configForm.validate(valid => {
        if (!valid) return
        this.configSaving = true
        updateVipPageConfig({
          customerServiceTip: this.configForm.customerServiceTip
        }, this.selectedImage).then(() => {
          this.$modal.msgSuccess('客服配置保存成功')
          this.selectedImage = null
          this.releaseLocalImage()
          return this.loadConfig()
        }).finally(() => { this.configSaving = false })
      })
    },
    openAdd() {
      this.benefitForm = defaultBenefit()
      this.benefitVisible = true
      this.$nextTick(() => this.$refs.benefitForm && this.$refs.benefitForm.clearValidate())
    },
    openEdit(row) {
      this.benefitForm = { ...defaultBenefit(), ...row }
      this.benefitVisible = true
      this.$nextTick(() => this.$refs.benefitForm && this.$refs.benefitForm.clearValidate())
    },
    saveBenefit() {
      if (this.benefitSaving) return
      this.$refs.benefitForm.validate(valid => {
        if (!valid) return
        this.benefitSaving = true
        const request = this.benefitForm.id ? updateVipBenefit : addVipBenefit
        request(this.benefitForm).then(() => {
          this.$modal.msgSuccess(this.benefitForm.id ? '权益修改成功' : '权益新增成功')
          this.benefitVisible = false
          return this.loadBenefits()
        }).finally(() => { this.benefitSaving = false })
      })
    },
    removeBenefit(row) {
      this.$modal.confirm(`确认删除权益“${row.benefitText}”吗？`)
        .then(() => deleteVipBenefit(row.id))
        .then(() => {
          this.$modal.msgSuccess('权益删除成功')
          return this.loadBenefits()
        })
    },
    releaseLocalImage() {
      if (this.localImageUrl) URL.revokeObjectURL(this.localImageUrl)
      this.localImageUrl = ''
    }
  }
}
</script>

<style scoped>
.config-card { margin-bottom: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.image-editor { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.image-preview,
.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180px;
  min-height: 180px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
}
.image-preview img { display: block; max-width: 100%; max-height: 320px; }
.image-placeholder { color: #909399; text-align: center; }
.upload-tip { width: 100%; color: #909399; font-size: 12px; line-height: 20px; }
.danger { color: #f56c6c; }
</style>
