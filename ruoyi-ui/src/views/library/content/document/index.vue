<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="2"><el-button v-if="$auth.hasPermiAnd(['library:document:add', 'library:document:upload'])" type="primary" icon="el-icon-plus" @click="open()">新增</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="categoryId" label="分类编号" />
      <el-table-column prop="pointPrice" label="所需积分" />
      <el-table-column prop="accessType" label="访问方式" width="110">
        <template slot-scope="scope">{{ accessTypeText(scope.row.accessType) }}</template>
      </el-table-column>
      <el-table-column prop="conversionStatus" label="处理状态" width="110" align="center">
        <template slot-scope="scope">
          <el-tag :type="conversionStatusType(scope.row.conversionStatus)" size="mini">
            {{ conversionStatusText(scope.row.conversionStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishStatus" label="发布状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="publishStatusType(scope.row.publishStatus)" size="mini">
            {{ publishStatusText(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template slot-scope="scope">
          <el-button v-hasPermi="['library:document:edit']" type="text" @click="open(scope.row)">修改</el-button>
          <el-button v-if="scope.row.publishStatus !== 'PUBLISHED'" v-hasPermi="['library:document:publish']" type="text" @click="changePublish(scope.row, true)">发布</el-button>
          <el-button v-else v-hasPermi="['library:document:publish']" type="text" @click="changePublish(scope.row, false)">下架</el-button>
          <el-button v-hasPermi="['library:document:remove']" type="text" class="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />

    <el-dialog
      :title="form.id ? '修改文档' : '新增文档'"
      :visible.sync="visible"
      :before-close="beforeClose"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-if="!form.id" class="upload-section">
        <el-form label-width="110px">
          <el-form-item label="原文件" required>
            <el-upload
              ref="originalUpload"
              action="#"
              :auto-upload="false"
              :limit="1"
              :file-list="fileList"
              :on-change="onOriginalChange"
              :on-remove="onOriginalRemove"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt"
            >
              <el-button :disabled="processing || saving" type="primary">选择文件</el-button>
            </el-upload>
            <el-button
              class="mt12"
              type="success"
              :loading="processing"
              :disabled="!selectedFile || saving || !!preparedSession"
              @click="processFile"
            >处理文件</el-button>
            <div class="form-tip">文件处理完成后会自动生成固定页数的试看 PDF 和首页缩略图。</div>
          </el-form-item>
        </el-form>

        <el-card v-if="preparedSession" class="prepared-card" shadow="never">
          <div slot="header">文件处理结果</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="文件名">{{ preparedSession.originalFileName }}</el-descriptions-item>
            <el-descriptions-item label="格式">{{ preparedSession.fileFormat }}</el-descriptions-item>
            <el-descriptions-item label="总页数">{{ preparedSession.pageCount }}</el-descriptions-item>
            <el-descriptions-item label="试看页数">{{ preparedSession.previewPages }}</el-descriptions-item>
          </el-descriptions>
          <div class="thumbnail-row">
            <el-image
              v-if="thumbnailBlobUrl"
              class="thumbnail"
              :src="thumbnailBlobUrl"
              :preview-src-list="[thumbnailBlobUrl]"
              fit="contain"
            />
            <el-upload
              action="#"
              :show-file-list="false"
              :auto-upload="false"
              accept=".jpg,.jpeg,.png"
              :on-change="replaceThumbnail"
            >
              <el-button size="small" :loading="replacingThumbnail">替换缩略图</el-button>
            </el-upload>
          </div>
        </el-card>
      </div>

      <el-form :model="form" label-width="110px">
        <el-form-item v-if="form.id" label="文档缩略图">
          <div v-loading="thumbnailLoading" class="thumbnail-row saved-thumbnail-row">
            <el-image
              v-if="savedThumbnailUrl"
              class="thumbnail"
              :src="savedThumbnailUrl"
              :preview-src-list="[savedThumbnailUrl]"
              fit="contain"
            />
            <div v-else class="thumbnail-empty">{{ thumbnailLoading ? '缩略图加载中' : '暂无缩略图' }}</div>
            <el-upload
              v-if="$auth.hasPermiAnd(['library:document:edit', 'library:document:upload'])"
              action="#"
              :show-file-list="false"
              :auto-upload="false"
              accept=".jpg,.jpeg,.png"
              :on-change="replaceSavedThumbnailFile"
            >
              <el-button size="small" :loading="replacingThumbnail">替换缩略图</el-button>
            </el-upload>
          </div>
          <div class="form-tip">点击缩略图可放大查看；选择新图片后会立即替换已保存文档的缩略图。</div>
        </el-form-item>
        <el-form-item label="标题" required><el-input v-model="form.title" maxlength="200" /></el-form-item>
        <el-form-item label="分类编号" required><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" maxlength="1000" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="多个标签用逗号分隔" /></el-form-item>
        <el-form-item label="所需积分" required><el-input-number v-model="form.pointPrice" :min="0" /></el-form-item>
        <el-form-item label="访问方式" required>
          <el-radio-group v-model="form.accessType">
            <el-radio label="POINT">积分兑换</el-radio>
            <el-radio label="VIP_FREE">会员免费</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button :disabled="processing || saving" @click="requestClose">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="processing || replacingThumbnail" @click="submit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listDocuments,
  updateDocument,
  deleteDocument,
  publishDocument,
  unpublishDocument,
  prepareDocumentUpload,
  getDocumentUploadThumbnail,
  replaceDocumentUploadThumbnail,
  getSavedDocumentThumbnail,
  replaceSavedDocumentThumbnail,
  commitDocumentUpload,
  cancelDocumentUpload
} from '@/api/library/content'

export default {
  name: 'LibraryDocument',
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      query: { pageNum: 1, pageSize: 10 },
      visible: false,
      form: {},
      fileList: [],
      selectedFile: null,
      preparedSession: null,
      thumbnailBlobUrl: '',
      savedThumbnailUrl: '',
      thumbnailLoading: false,
      thumbnailRequestSequence: 0,
      processing: false,
      replacingThumbnail: false,
      saving: false
    }
  },
  created() { this.load() },
  beforeDestroy() {
    this.thumbnailRequestSequence += 1
    this.revokeThumbnailUrl()
  },
  methods: {
    conversionStatusText(status) {
      return {
        PENDING: '待处理',
        CONVERTING: '处理中',
        SUCCESS: '处理成功',
        FAILED: '处理失败'
      }[status] || '未知状态'
    },
    conversionStatusType(status) {
      return { PENDING: 'info', CONVERTING: 'warning', SUCCESS: 'success', FAILED: 'danger' }[status] || 'info'
    },
    publishStatusText(status) {
      return { DRAFT: '草稿', PUBLISHED: '已发布' }[status] || '未知状态'
    },
    publishStatusType(status) {
      return status === 'PUBLISHED' ? 'success' : 'info'
    },
    accessTypeText(accessType) {
      return accessType === 'VIP_FREE' ? '会员免费' : '积分兑换'
    },
    load() {
      this.loading = true
      listDocuments(this.query).then(res => {
        this.rows = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    open(row) {
      this.resetUploadState()
      this.form = row
        ? { ...row }
        : { title: '', categoryId: null, summary: '', tags: '', pointPrice: 0, accessType: 'POINT', sortOrder: 0 }
      if (!this.form.accessType) this.form.accessType = 'POINT'
      this.visible = true
      if (row && row.id) this.loadSavedThumbnail(row.id)
    },
    loadSavedThumbnail(documentId) {
      const sequence = ++this.thumbnailRequestSequence
      this.thumbnailLoading = true
      this.savedThumbnailUrl = ''
      getSavedDocumentThumbnail(documentId).then(res => {
        if (sequence !== this.thumbnailRequestSequence || !this.visible || this.form.id !== documentId) return
        this.savedThumbnailUrl = res.data ? res.data.thumbnailUrl : ''
      }).catch(() => {
        if (sequence === this.thumbnailRequestSequence) this.savedThumbnailUrl = ''
      }).finally(() => {
        if (sequence === this.thumbnailRequestSequence) this.thumbnailLoading = false
      })
    },
    onOriginalChange(file) {
      const assign = () => {
        this.selectedFile = file.raw
        this.fileList = [file]
        if (!this.form.title && file.name) this.form.title = file.name.replace(/\.[^.]+$/, '')
      }
      if (!this.preparedSession) return assign()
      this.cancelPreparedSession().finally(assign)
    },
    onOriginalRemove() {
      this.selectedFile = null
      this.fileList = []
      if (this.preparedSession) this.cancelPreparedSession()
    },
    processFile() {
      if (!this.selectedFile) return this.$modal.msgError('请选择文档原文件')
      this.processing = true
      const data = new FormData()
      data.append('file', this.selectedFile)
      prepareDocumentUpload(data).then(res => {
        this.preparedSession = res.data
        return this.refreshThumbnail()
      }).then(() => {
        this.$modal.msgSuccess('文件处理完成')
      }).catch(() => {
        this.preparedSession = null
        this.revokeThumbnailUrl()
      }).finally(() => { this.processing = false })
    },
    refreshThumbnail() {
      if (!this.preparedSession) return Promise.resolve()
      return getDocumentUploadThumbnail(this.preparedSession.sessionId).then(data => {
        this.revokeThumbnailUrl()
        const blob = data instanceof Blob ? data : new Blob([data], { type: 'image/jpeg' })
        this.thumbnailBlobUrl = URL.createObjectURL(blob)
      })
    },
    replaceThumbnail(file) {
      if (!this.preparedSession || !file.raw) return
      this.replacingThumbnail = true
      const data = new FormData()
      data.append('file', file.raw)
      replaceDocumentUploadThumbnail(this.preparedSession.sessionId, data)
        .then(() => this.refreshThumbnail())
        .then(() => this.$modal.msgSuccess('缩略图已替换'))
        .finally(() => { this.replacingThumbnail = false })
    },
    replaceSavedThumbnailFile(file) {
      if (!this.form.id || !file.raw) return
      this.replacingThumbnail = true
      const data = new FormData()
      data.append('file', file.raw)
      replaceSavedDocumentThumbnail(this.form.id, data).then(res => {
        this.savedThumbnailUrl = res.data.thumbnailUrl
        this.$modal.msgSuccess('缩略图已替换')
      }).finally(() => { this.replacingThumbnail = false })
    },
    submit() {
      if (!this.form.title || !this.form.categoryId) return this.$modal.msgError('请填写标题和分类编号')
      if (this.form.id) {
        this.saving = true
        return updateDocument(this.form).then(() => this.onSaved()).finally(() => { this.saving = false })
      }
      if (!this.preparedSession) return this.$modal.msgError('请先选择并处理文档原文件')
      this.saving = true
      commitDocumentUpload(this.preparedSession.sessionId, this.form).then(() => {
        this.preparedSession = null
        this.onSaved()
      }).finally(() => { this.saving = false })
    },
    onSaved() {
      this.$modal.msgSuccess('保存成功')
      this.visible = false
      this.resetUploadState()
      this.load()
    },
    requestClose() {
      this.beforeClose(() => { this.visible = false })
    },
    beforeClose(done) {
      if (this.processing || this.saving || this.replacingThumbnail) {
        this.$modal.msgWarning('文件正在处理中，请稍候')
        return
      }
      if (!this.preparedSession) {
        this.resetUploadState()
        return done()
      }
      this.cancelPreparedSession().finally(() => {
        this.resetUploadState()
        done()
      })
    },
    cancelPreparedSession() {
      const session = this.preparedSession
      this.preparedSession = null
      this.revokeThumbnailUrl()
      if (!session) return Promise.resolve()
      return cancelDocumentUpload(session.sessionId).catch(() => {})
    },
    resetUploadState() {
      this.thumbnailRequestSequence += 1
      this.selectedFile = null
      this.fileList = []
      this.preparedSession = null
      this.processing = false
      this.replacingThumbnail = false
      this.savedThumbnailUrl = ''
      this.thumbnailLoading = false
      this.saving = false
      this.revokeThumbnailUrl()
    },
    revokeThumbnailUrl() {
      if (this.thumbnailBlobUrl) URL.revokeObjectURL(this.thumbnailBlobUrl)
      this.thumbnailBlobUrl = ''
    },
    remove(row) {
      this.$modal.confirm(`确认删除文档“${row.title}”吗？`)
        .then(() => deleteDocument(row.id))
        .then(() => { this.$modal.msgSuccess('删除成功'); this.load() })
    },
    changePublish(row, publishing) {
      const action = publishing ? '发布' : '下架'
      this.$modal.confirm(`确认${action}文档“${row.title}”吗？`)
        .then(() => (publishing ? publishDocument : unpublishDocument)(row.id))
        .then(() => { this.$modal.msgSuccess(`${action}成功`); this.load() })
    }
  }
}
</script>

<style scoped>
.danger{color:#f56c6c}.mt12{margin-top:12px}.form-tip{margin-top:8px;color:#909399;font-size:12px}.prepared-card{margin-bottom:20px}.thumbnail-row{display:flex;align-items:flex-end;gap:20px;margin-top:18px}.saved-thumbnail-row{margin-top:0}.thumbnail{width:180px;height:240px;border:1px solid #ebeef5;background:#f5f7fa;cursor:zoom-in}.thumbnail-empty{display:flex;align-items:center;justify-content:center;width:180px;height:240px;border:1px solid #ebeef5;background:#f5f7fa;color:#909399}
</style>
