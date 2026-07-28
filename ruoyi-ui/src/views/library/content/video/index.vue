<template>
  <div class="app-container">
    <el-form :inline="true" size="small"><el-form-item label="课程"><el-select v-model="courseId" filterable placeholder="请选择课程" @change="loadVideos"><el-option v-for="course in courses" :key="course.id" :label="course.title" :value="course.id" /></el-select></el-form-item><el-form-item><el-button v-hasPermi="['library:course:edit']" type="primary" plain size="mini" :disabled="!courseId" @click="open()">新增视频</el-button></el-form-item></el-form>
    <el-row :gutter="10" class="mb8">
      <right-toolbar :search="false" @queryTable="loadVideos" />
    </el-row>
    <el-table v-loading="loading" :data="videos">
      <el-table-column prop="title" label="视频标题" />
      <el-table-column prop="videoObjectKey" label="私有对象键" min-width="220" show-overflow-tooltip />
      <el-table-column prop="durationSeconds" label="时长（秒）" />
      <el-table-column prop="sortOrder" label="排序" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作"><template slot-scope="scope"><el-button v-hasPermi="['library:course:edit']" type="text" @click="open(scope.row)">修改</el-button></template></el-table-column>
    </el-table>
    <el-dialog :title="form.id ? '修改课程视频' : '新增课程视频'" :visible.sync="visible">
      <el-form :model="form" label-width="120px">
        <el-form-item label="视频标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="私有对象键" required><el-input v-model="form.videoObjectKey" /></el-form-item>
        <el-form-item label="时长（秒）" required><el-input-number v-model="form.durationSeconds" :min="1" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import { listCourses, listVideos, saveVideo } from '@/api/library/course'
export default {
  name: 'LibraryVideo',
  data() { return { loading: false, courses: [], courseId: null, videos: [], visible: false, form: {} } },
  created() { listCourses({ pageNum: 1, pageSize: 100 }).then(res => { this.courses = res.rows || []; if (this.courses.length) { this.courseId = this.courses[0].id; this.loadVideos() } }) },
  methods: {
    loadVideos() { this.loading = true; listVideos(this.courseId).then(res => { this.videos = res.data || res || [] }).finally(() => { this.loading = false }) },
    open(row) { this.form = row ? { ...row } : { courseId: this.courseId, title: '', videoObjectKey: '', durationSeconds: 1, sortOrder: 0, status: '0' }; this.visible = true },
    submit() { if (!this.form.title || !this.form.videoObjectKey) return this.$modal.msgError('请填写视频标题和私有对象键'); saveVideo(this.form).then(() => { this.$modal.msgSuccess('保存成功'); this.visible = false; this.loadVideos() }) }
  }
}
</script>
