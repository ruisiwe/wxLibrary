<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :model="query" :inline="true" size="small">
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="query.title"
          placeholder="请输入轮播图标题"
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="2">
        <el-button
          v-hasPermi="['library:banner:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="openAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="2">
        <el-button
          v-hasPermi="['library:banner:remove']"
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="selectedIds.length === 0"
          @click="removeSelected"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="load" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="rows"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column prop="id" label="编号" width="80" align="center" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="documentTitle" label="关联文档" min-width="220" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.documentTitle || `文档 #${scope.row.documentId}` }}</span>
          <el-tag v-if="scope.row.documentSelectable === false" type="warning" size="mini">已不可选</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="165">
        <template slot-scope="scope">
          {{ scope.row.startTime ? parseTime(scope.row.startTime, '{y}-{m}-{d}') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" width="165">
        <template slot-scope="scope">
          {{ scope.row.endTime ? parseTime(scope.row.endTime, '{y}-{m}-{d}') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button v-hasPermi="['library:banner:edit']" type="text" @click="openEdit(scope.row)">修改</el-button>
          <el-button
            v-hasPermi="['library:banner:remove']"
            type="text"
            class="danger"
            @click="removeRows([scope.row.id], scope.row.title)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="query.pageNum"
      :limit.sync="query.pageSize"
      @pagination="load"
    />

    <el-dialog
      :title="form.id ? '修改轮播图' : '新增轮播图'"
      :visible.sync="visible"
      width="780px"
      append-to-body
      :close-on-click-modal="false"
      :before-close="beforeDialogClose"
    >
      <el-form ref="bannerForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="关联文档" prop="documentId">
          <remote-select
            v-model="form.documentId"
            :field="documentField"
            :row="form"
            @selection-change="onDocumentSelection"
          />
        </el-form-item>
        <el-form-item label="轮播图片" required>
          <div v-loading="imageLoading">
            <banner-image-cropper
              ref="imageCropper"
              :value="bannerPreviewUrl || existingImageUrl"
              @change="onBannerCropped"
            />
          </div>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">启用</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="不设置则立即开始"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="不设置则长期展示"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button :disabled="saving" @click="requestClose">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import RemoteSelect from '@/views/library/common/RemoteSelect'
import BannerImageCropper from './BannerImageCropper'
import {
  listBanners,
  addBanner,
  updateBanner,
  deleteBanner,
  getBannerImage,
  listBannerDocumentOptions
} from '@/api/library/content'

const emptyForm = () => ({
  id: null,
  title: '',
  documentId: null,
  documentTitle: '',
  documentCategoryName: '',
  documentFileFormat: '',
  documentSelectable: null,
  sortOrder: 0,
  status: '0',
  startTime: null,
  endTime: null
})

export default {
  name: 'LibraryBanner',
  components: { RemoteSelect, BannerImageCropper },
  data() {
    return {
      loading: false,
      showSearch: true,
      rows: [],
      total: 0,
      query: { pageNum: 1, pageSize: 10, title: '', status: '' },
      selectedIds: [],
      visible: false,
      form: emptyForm(),
      bannerBlob: null,
      bannerPreviewUrl: '',
      existingImageUrl: '',
      imageLoading: false,
      previewRequestSequence: 0,
      saving: false,
      rules: {
        title: [{ required: true, message: '请输入轮播图标题', trigger: 'blur' }],
        documentId: [{ required: true, message: '请选择关联文档', trigger: 'change' }],
        sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    documentField() {
      return {
        placeholder: '请输入文档标题搜索',
        optionValue: 'id',
        debounce: 300,
        remoteLoader: keyword => this.searchDocumentOptions(keyword),
        optionLabel: option => this.documentOptionLabel(option),
        optionDisabled: option => option.documentSelectable === false,
        initialOption: row => this.initialDocumentOption(row),
        invalidMessage: '原关联文档已下架，请重新选择',
        emptyText: '未找到匹配的文档',
        loadErrorText: '关联文档搜索失败，请稍后重试'
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      listBanners(this.query).then(response => {
        this.rows = response.rows || []
        this.total = response.total || 0
      }).finally(() => { this.loading = false })
    },
    handleQuery() {
      this.query.pageNum = 1
      this.load()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.query.pageNum = 1
      this.load()
    },
    handleSelectionChange(selection) {
      this.selectedIds = (selection || []).map(item => item.id)
    },
    openAdd() {
      this.clearDialogState()
      this.form = emptyForm()
      this.visible = true
    },
    openEdit(row) {
      this.clearDialogState()
      this.form = { ...emptyForm(), ...row }
      this.visible = true
      this.imageLoading = true
      const sequence = ++this.previewRequestSequence
      getBannerImage(row.id).then(response => {
        if (sequence !== this.previewRequestSequence || !this.visible
          || this.form.id !== row.id) return
        this.existingImageUrl = response.data ? response.data.imageUrl : ''
      }).finally(() => {
        if (sequence === this.previewRequestSequence) this.imageLoading = false
      })
    },
    searchDocumentOptions(keyword) {
      return listBannerDocumentOptions({ keyword, pageNum: 1, pageSize: 20 }).then(response => {
        const data = response.data || {}
        return data.items || []
      })
    },
    documentOptionLabel(option) {
      return [
        option.title,
        option.categoryName,
        option.fileFormat,
        this.documentAvailabilityText(option)
      ].filter(Boolean).join(' / ')
    },
    documentAvailabilityText(option) {
      const labels = {
        AVAILABLE: '已发布',
        DRAFT: '草稿，暂不可关联',
        CATEGORY_DISABLED: '分类已停用',
        CATEGORY_UNAVAILABLE: '分类不可用',
        UNAVAILABLE: '不可关联'
      }
      if (option && labels[option.availabilityStatus]) return labels[option.availabilityStatus]
      return option && option.documentSelectable !== false ? '已发布' : '不可关联'
    },
    initialDocumentOption(row) {
      if (!row || !row.documentId) return null
      return {
        id: row.documentId,
        title: row.documentTitle || '原关联文档',
        categoryName: row.documentCategoryName,
        fileFormat: row.documentFileFormat,
        documentSelectable: row.documentSelectable !== false,
        availabilityStatus: row.documentSelectable === false ? 'UNAVAILABLE' : 'AVAILABLE'
      }
    },
    onDocumentSelection(option) {
      this.form.documentSelectable = option ? option.documentSelectable !== false : null
      this.form.documentTitle = option ? option.title : ''
      this.form.documentCategoryName = option ? option.categoryName : ''
      this.form.documentFileFormat = option ? option.fileFormat : ''
    },
    onBannerCropped(result) {
      this.bannerBlob = result.blob
      this.bannerPreviewUrl = result.previewUrl
    },
    submit() {
      if (this.saving) return
      this.$refs.bannerForm.validate(valid => {
        if (!valid) return
        if (this.form.documentSelectable === false) {
          this.$modal.msgError('原关联文档已下架，请重新选择')
          return
        }
        if (!this.form.id && !this.bannerBlob) {
          this.$modal.msgError('请选择本地图片并完成裁剪')
          return
        }
        if (this.form.startTime && this.form.endTime
          && new Date(this.form.startTime).getTime() >= new Date(this.form.endTime).getTime()) {
          this.$modal.msgError('展示结束时间必须晚于开始时间')
          return
        }
        const payload = { ...this.form }
        delete payload.imageUrl
        this.saving = true
        const request = this.form.id
          ? updateBanner(payload, this.bannerBlob)
          : addBanner(payload, this.bannerBlob)
        request.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.visible = false
          this.clearDialogState()
          this.load()
        }).finally(() => { this.saving = false })
      })
    },
    removeSelected() {
      this.removeRows(this.selectedIds)
    },
    removeRows(ids, title) {
      if (!ids || ids.length === 0) return
      const target = title ? `“${title}”` : `选中的${ids.length}条轮播图`
      this.$modal.confirm(`确认删除${target}吗？`)
        .then(() => deleteBanner(ids.join(',')))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.selectedIds = []
          this.load()
        })
    },
    requestClose() {
      this.beforeDialogClose(() => { this.visible = false })
    },
    beforeDialogClose(done) {
      if (this.saving) {
        this.$modal.msgWarning('轮播图正在保存，请稍候')
        return
      }
      this.clearDialogState()
      done()
    },
    clearDialogState() {
      this.previewRequestSequence += 1
      this.bannerBlob = null
      this.bannerPreviewUrl = ''
      this.existingImageUrl = ''
      this.imageLoading = false
      this.saving = false
      if (this.$refs.imageCropper) this.$refs.imageCropper.reset()
      if (this.$refs.bannerForm) this.$refs.bannerForm.clearValidate()
    }
  }
}
</script>

<style scoped>
.danger { color: #f56c6c; }
</style>
