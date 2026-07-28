<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['library:course:add']"
          type="primary"
          plain
          size="mini"
          icon="el-icon-plus"
          @click="openDialog()"
        >新增课程</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="load" />
    </el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="title" label="课程名称"/>
      <el-table-column prop="accessType" label="访问方式">
        <template slot-scope="s">{{ s.row.accessType === 'VIP' ? 'VIP 可看' : '课程码兑换' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态"/>
      <el-table-column label="操作">
        <template slot-scope="s">
          <el-button v-hasPermi="['library:course:edit']" type="text" @click="openDialog(s.row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="form.id ? '修改课程' : '新增课程'" :visible.sync="visible">
      <el-form :model="form" label-width="100px">
        <el-form-item label="课程名称" required><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="访问方式" required>
          <el-radio-group v-model="form.accessType">
            <el-radio label="VIP">VIP 可看</el-radio>
            <el-radio label="CODE">课程码兑换</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="课程摘要"><el-input v-model="form.summary" type="textarea"/></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></span>
    </el-dialog>
  </div>
</template>
<script>import { listCourses, addCourse, updateCourse } from '@/api/library/course';export default{name:'LibraryCourse',data(){return{loading:false,rows:[],visible:false,form:{}}},created(){this.load()},methods:{load(){this.loading=true;listCourses({pageNum:1,pageSize:100}).then(r=>{this.rows=r.rows||[]}).finally(()=>{this.loading=false})},openDialog(row){this.form=row?{...row}:{title:'',summary:'',accessType:'VIP',sortOrder:0,status:'0'};this.visible=true},submit(){if(!this.form.title||!this.form.accessType)return this.$modal.msgError('请填写课程名称并选择访问方式');(this.form.id?updateCourse:addCourse)(this.form).then(()=>{this.$modal.msgSuccess('保存成功');this.visible=false;this.load()})}}}</script><style scoped>.mt16{margin-top:16px}</style>
