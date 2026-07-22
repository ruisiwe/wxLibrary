<template>
  <div class="app-container">
    <el-button v-hasPermi="['library:agreement:add']" type="primary" icon="el-icon-plus" @click="open()">新增协议版本</el-button>
    <el-table v-loading="loading" :data="rows" border class="mt16">
      <el-table-column prop="agreementType" label="协议类型"><template slot-scope="s">{{agreementTypeLabel(s.row.agreementType)}}</template></el-table-column>
      <el-table-column prop="version" label="版本"/><el-table-column prop="title" label="标题" min-width="180"/><el-table-column prop="effectiveTime" label="生效时间" min-width="170"/><el-table-column prop="status" label="状态"><template slot-scope="s">{{s.row.status==='0'?'草稿':s.row.status==='1'?'已发布':'已替代'}}</template></el-table-column>
      <el-table-column label="操作" width="160"><template slot-scope="s"><el-button v-if="s.row.status==='0'" v-hasPermi="['library:agreement:edit']" type="text" @click="open(s.row)">修改</el-button><el-button v-if="s.row.status==='0'" v-hasPermi="['library:agreement:publish']" type="text" @click="publish(s.row)">发布</el-button></template></el-table-column>
    </el-table>
    <el-dialog :title="form.id?'修改协议版本':'新增协议版本'" :visible.sync="visible" width="760px">
      <el-form :model="form" label-width="100px"><el-form-item label="协议类型" required><el-radio-group v-model="form.agreementType"><el-radio label="PRIVACY">用户隐私协议</el-radio><el-radio label="STATEMENT">网站声明</el-radio><el-radio label="FILE_DISCLAIMER">文件发送免责声明</el-radio></el-radio-group></el-form-item><el-form-item label="版本" required><el-input v-model="form.version"/></el-form-item><el-form-item label="标题" required><el-input v-model="form.title"/></el-form-item><el-form-item label="协议内容" required><el-input v-model="form.content" type="textarea" :rows="12"/></el-form-item><el-form-item label="生效时间" required><el-date-picker v-model="form.effectiveTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss"/></el-form-item></el-form>
      <span slot="footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submit">保存草稿</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import { listAgreements,addAgreement,updateAgreement,publishAgreement } from '@/api/library/agreement'
export default {name:'LibraryAgreement',data(){return{loading:false,rows:[],visible:false,form:{}}},created(){this.load()},methods:{
  agreementTypeLabel(type){return type==='PRIVACY'?'用户隐私协议':type==='STATEMENT'?'网站声明':type==='FILE_DISCLAIMER'?'文件发送免责声明':type},
  load(){this.loading=true;listAgreements({pageNum:1,pageSize:100}).then(r=>{this.rows=r.rows||[]}).finally(()=>{this.loading=false})},
  open(row){this.form=row?{...row}:{agreementType:'PRIVACY',version:'',title:'',content:'',effectiveTime:'',status:'DRAFT'};this.visible=true},
  submit(){if(!this.form.version||!this.form.title||!this.form.content||!this.form.effectiveTime)return this.$modal.msgError('请完整填写协议版本、标题、内容和生效时间');(this.form.id?updateAgreement:addAgreement)(this.form).then(()=>{this.$modal.msgSuccess('协议草稿已保存');this.visible=false;this.load()})},
  publish(row){this.$modal.confirm(`确认发布协议“${row.title}”吗？未来生效版本会在生效时间到达后启用。`).then(()=>publishAgreement(row.id)).then(()=>{this.$modal.msgSuccess('发布成功');this.load()})}
}}
</script><style scoped>.mt16{margin-top:16px}</style>
