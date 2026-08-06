<template>
  <div class="app-container qr-config-page">
    <el-form
      ref="queryForm"
      :model="query"
      :inline="true"
      size="small"
      class="query-form"
      @submit.native.prevent
    >
      <el-form-item label="菜单名称" prop="menuName">
        <el-input
          v-model="query.menuName"
          placeholder="请输入菜单名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="query.status" placeholder="全部状态" clearable>
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-toolbar">
      <el-button
        v-hasPermi="['library:qr:add']"
        type="primary"
        plain
        size="mini"
        icon="el-icon-plus"
        @click="openAdd"
      >新增</el-button>
      <right-toolbar :show-search.sync="showSearch" @queryTable="loadList" />
    </div>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="菜单名称" prop="menuName" min-width="150" />
      <el-table-column label="引导文字" prop="guideText" min-width="220" show-overflow-tooltip />
      <el-table-column label="二维码图片" width="130" align="center">
        <template slot-scope="scope">
          <el-image
            v-if="previewUrls[scope.row.id]"
            class="qr-preview"
            :src="previewUrls[scope.row.id]"
            :preview-src-list="[previewUrls[scope.row.id]]"
            fit="contain"
          />
          <span v-else class="empty-image">
            {{ scope.row.imageConfigured ? '加载中' : '暂未配置' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="90" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            :disabled="!canEdit"
            @change="changeStatus(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="310" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['library:qr:edit']"
            type="text"
            icon="el-icon-edit"
            @click="openEdit(scope.row)"
          >修改</el-button>
          <label v-hasPermi="['library:qr:edit']" class="upload-action">
            <i class="el-icon-upload2" />
            {{ scope.row.imageConfigured ? '替换图片' : '上传图片' }}
            <input
              type="file"
              accept=".jpg,.jpeg,.png,.webp"
              @change="uploadImage(scope.row, $event)"
            >
          </label>
          <el-button
            v-if="scope.row.imageConfigured"
            v-hasPermi="['library:qr:edit']"
            type="text"
            class="danger"
            @click="clearImage(scope.row)"
          >清空图片</el-button>
          <el-button
            v-hasPermi="['library:qr:remove']"
            type="text"
            class="danger"
            @click="removeRow(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="query.pageNum"
      :limit.sync="query.pageSize"
      @pagination="loadList"
    />

    <el-dialog
      :title="form.id ? '修改二维码配置' : '新增二维码配置'"
      :visible.sync="dialogVisible"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="菜单名称" prop="menuName">
          <el-input
            v-model="form.menuName"
            maxlength="50"
            show-word-limit
            placeholder="例如：加入社群"
          />
        </el-form-item>
        <el-form-item label="引导文字" prop="guideText">
          <el-input
            v-model="form.guideText"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：长按识别二维码加入社群"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" :precision="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="保存文字配置后，可在列表中上传二维码图片；支持 JPEG、PNG 或 WebP，不能超过 2MB。"
        />
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listQrConfigs,
  getQrConfig,
  addQrConfig,
  updateQrConfig,
  deleteQrConfig,
  uploadQrConfigImage,
  clearQrConfigImage,
  getQrConfigImage
} from '@/api/library/qrConfig'

const defaultForm = () => ({
  id: null,
  menuName: '',
  guideText: '',
  sortOrder: 0,
  status: '0'
})

export default {
  name: 'LibraryQrConfig',
  data() {
    return {
      loading: false,
      saving: false,
      showSearch: true,
      rows: [],
      total: 0,
      previewUrls: {},
      dialogVisible: false,
      query: {
        pageNum: 1,
        pageSize: 10,
        menuName: '',
        status: ''
      },
      form: defaultForm(),
      rules: {
        menuName: [
          { required: true, message: '请输入菜单名称', trigger: 'blur' },
          { max: 50, message: '菜单名称不能超过50个字符', trigger: 'blur' }
        ],
        guideText: [
          { max: 200, message: '引导文字不能超过200个字符', trigger: 'blur' }
        ],
        sortOrder: [{ required: true, message: '请输入排序', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    canEdit() {
      const permissions = (this.$store.getters && this.$store.getters.permissions) || []
      return permissions.includes('*:*:*') || permissions.includes('library:qr:edit')
    }
  },
  created() {
    this.loadList()
  },
  beforeDestroy() {
    this.releasePreviews()
  },
  methods: {
    loadList() {
      this.loading = true
      return listQrConfigs(this.query)
        .then(response => {
          this.rows = response.rows || []
          this.total = response.total || 0
          this.loadPreviews()
        })
        .finally(() => { this.loading = false })
    },
    loadPreviews() {
      this.releasePreviews()
      this.rows.filter(row => row.imageConfigured).forEach(row => {
        getQrConfigImage(row.id).then(blob => {
          this.$set(this.previewUrls, row.id, URL.createObjectURL(blob))
        }).catch(() => {})
      })
    },
    releasePreviews() {
      Object.keys(this.previewUrls).forEach(id => URL.revokeObjectURL(this.previewUrls[id]))
      this.previewUrls = {}
    },
    handleQuery() {
      this.query.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.handleQuery()
    },
    openAdd() {
      this.form = defaultForm()
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    openEdit(row) {
      getQrConfig(row.id).then(response => {
        this.form = { ...defaultForm(), ...(response.data || row) }
        this.dialogVisible = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      })
    },
    save() {
      if (this.saving) return
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const request = this.form.id ? updateQrConfig : addQrConfig
        request(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.dialogVisible = false
          return this.loadList()
        }).finally(() => { this.saving = false })
      })
    },
    changeStatus(row) {
      updateQrConfig(row).then(() => {
        this.$modal.msgSuccess(row.status === '0' ? '已启用' : '已停用')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    uploadImage(row, event) {
      const input = event.target
      const file = input.files && input.files[0]
      input.value = ''
      if (!file) return
      const allowed = ['image/jpeg', 'image/png', 'image/webp']
      if (!allowed.includes(file.type)) {
        this.$modal.msgError('二维码图片仅支持 JPEG、PNG 或 WebP 格式')
        return
      }
      if (file.size > 2 * 1024 * 1024) {
        this.$modal.msgError('二维码图片不能超过 2MB')
        return
      }
      uploadQrConfigImage(row.id, file).then(() => {
        this.$modal.msgSuccess(row.imageConfigured ? '图片替换成功' : '图片上传成功')
        return this.loadList()
      })
    },
    clearImage(row) {
      this.$modal.confirm(`确认清空“${row.menuName}”的二维码图片吗？`)
        .then(() => clearQrConfigImage(row.id))
        .then(() => {
          this.$modal.msgSuccess('图片已清空')
          return this.loadList()
        })
    },
    removeRow(row) {
      this.$modal.confirm(`确认删除二维码配置“${row.menuName}”吗？`)
        .then(() => deleteQrConfig(row.id))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          return this.loadList()
        })
    }
  }
}
</script>

<style scoped>
.query-form { margin-bottom: 2px; }
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.qr-preview { width: 64px; height: 64px; }
.empty-image { color: #909399; font-size: 12px; }
.upload-action {
  display: inline-block;
  margin-left: 10px;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
}
.upload-action input { display: none; }
.danger { color: #f56c6c; }
</style>
