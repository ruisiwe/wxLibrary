<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="2"><el-button v-hasPermi="['library:document:add']" type="primary" icon="el-icon-plus" @click="open()">新增</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="categoryId" label="分类编号" />
      <el-table-column prop="pointPrice" label="所需积分" />
      <el-table-column prop="conversionStatus" label="转换状态" />
      <el-table-column prop="publishStatus" label="发布状态" />
      <el-table-column label="操作" width="310" fixed="right">
        <template slot-scope="scope">
          <el-button v-hasPermi="['library:document:edit']" type="text" @click="open(scope.row)">修改</el-button>
          <el-button v-hasPermi="['library:document:upload']" type="text" @click="openUpload(scope.row)">上传原文件</el-button>
          <el-button v-if="scope.row.publishStatus !== 'PUBLISHED'" v-hasPermi="['library:document:publish']" type="text" @click="changePublish(scope.row, true)">发布</el-button>
          <el-button v-else v-hasPermi="['library:document:publish']" type="text" @click="changePublish(scope.row, false)">下架</el-button>
          <el-button v-hasPermi="['library:document:remove']" type="text" class="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />

    <el-dialog :title="form.id ? '修改文档' : '新增文档'" :visible.sync="visible" width="650px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="标题" required><el-input v-model="form.title" maxlength="200" /></el-form-item>
        <el-form-item label="分类编号" required><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" maxlength="1000" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="多个标签用逗号分隔" /></el-form-item>
        <el-form-item label="所需积分" required><el-input-number v-model="form.pointPrice" :min="0" /></el-form-item>
        <el-form-item label="预览页数" required><el-input-number v-model="form.previewPages" :min="0" /></el-form-item>
        <el-form-item label="封面地址"><el-input v-model="form.coverUrl" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></span>
    </el-dialog>

    <el-dialog title="上传文档原文件" :visible.sync="uploadVisible" width="520px">
      <el-alert title="上传将创建新的转换任务，请到“文档转换”页面执行。" type="warning" :closable="false" />
      <el-upload ref="upload" class="mt16" :auto-upload="false" :limit="1" :file-list="fileList" :on-change="onFileChange" :on-remove="onFileRemove">
        <el-button type="primary">选择文件</el-button>
      </el-upload>
      <span slot="footer"><el-button @click="uploadVisible=false">取消</el-button><el-button type="primary" @click="submitUpload">确认上传</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { listDocuments, addDocument, updateDocument, deleteDocument, publishDocument, unpublishDocument, uploadDocumentFile } from '@/api/library/content'

export default {
  name: 'LibraryDocument',
  data() {
    return { loading: false, rows: [], total: 0, query: { pageNum: 1, pageSize: 10 }, visible: false, uploadVisible: false, form: {}, uploadRow: null, fileList: [], selectedFile: null }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      listDocuments(this.query).then(res => { this.rows = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false })
    },
    open(row) {
      this.form = row ? { ...row } : { title: '', categoryId: null, summary: '', tags: '', pointPrice: 0, previewPages: 0, sortOrder: 0 }
      this.visible = true
    },
    submit() {
      if (!this.form.title || !this.form.categoryId) return this.$modal.msgError('请填写标题和分类编号')
      ;(this.form.id ? updateDocument : addDocument)(this.form).then(() => { this.$modal.msgSuccess('保存成功'); this.visible = false; this.load() })
    },
    remove(row) {
      this.$modal.confirm(`确认删除文档“${row.title}”吗？`).then(() => deleteDocument(row.id)).then(() => { this.$modal.msgSuccess('删除成功'); this.load() })
    },
    changePublish(row, publishing) {
      const action = publishing ? '发布' : '下架'
      this.$modal.confirm(`确认${action}文档“${row.title}”吗？`).then(() => (publishing ? publishDocument : unpublishDocument)(row.id)).then(() => { this.$modal.msgSuccess(`${action}成功`); this.load() })
    },
    openUpload(row) { this.uploadRow = row; this.fileList = []; this.selectedFile = null; this.uploadVisible = true },
    onFileChange(file) { this.selectedFile = file.raw },
    onFileRemove() { this.selectedFile = null },
    submitUpload() {
      if (!this.selectedFile) return this.$modal.msgError('请选择文档原文件')
      const data = new FormData()
      data.append('file', this.selectedFile)
      uploadDocumentFile(this.uploadRow.id, data).then(() => { this.$modal.msgSuccess('上传成功，已创建转换任务'); this.uploadVisible = false; this.load() })
    }
  }
}
</script>

<style scoped>.danger{color:#f56c6c}.mt16{margin-top:16px}</style>
