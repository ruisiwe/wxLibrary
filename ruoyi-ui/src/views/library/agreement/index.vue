<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['library:agreement:add']"
          type="primary"
          plain
          size="mini"
          icon="el-icon-plus"
          @click="open()"
        >新增协议版本</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="load" />
    </el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="agreementType" label="协议类型"><template slot-scope="s">{{agreementTypeLabel(s.row.agreementType)}}</template></el-table-column>
      <el-table-column prop="version" label="版本"/>
      <el-table-column prop="title" label="标题" min-width="180"/>
      <el-table-column label="生效时间" min-width="170">
        <template slot-scope="s">
          {{ s.row.effectiveTime ? parseTime(s.row.effectiveTime, '{y}-{m}-{d}') : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态"><template slot-scope="s">{{s.row.status==='0'?'草稿':s.row.status==='1'?'已发布':'已替代'}}</template></el-table-column>
      <el-table-column label="操作" width="200">
        <template slot-scope="s">
          <el-button type="text" @click="view(s.row)">查看</el-button>
          <el-button
            v-if="s.row.status === '0'"
            v-hasPermi="['library:agreement:edit']"
            type="text"
            @click="open(s.row)"
          >修改</el-button>
          <el-button
            v-if="s.row.status === '0'"
            v-hasPermi="['library:agreement:publish']"
            type="text"
            @click="publish(s.row)"
          >发布</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog
      :title="readOnly ? '查看协议版本' : form.id ? '修改协议版本' : '新增协议版本'"
      :visible.sync="visible"
      width="760px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="协议类型" required>
          <el-radio-group v-model="form.agreementType" :disabled="readOnly">
            <el-radio label="PRIVACY">用户隐私协议</el-radio>
            <el-radio label="STATEMENT">网站声明</el-radio>
            <el-radio label="FILE_DISCLAIMER">文件发送免责声明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="版本" required>
          <el-input v-model="form.version" :disabled="readOnly"/>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" :disabled="readOnly"/>
        </el-form-item>
        <el-form-item label="协议内容" required>
          <el-input v-model="form.content" type="textarea" :rows="12" :disabled="readOnly"/>
        </el-form-item>
        <el-form-item label="生效时间" required>
          <el-date-picker
            v-model="form.effectiveTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            :disabled="readOnly"
          />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button v-if="readOnly" @click="visible = false">关闭</el-button>
        <template v-else>
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="submit">保存草稿</el-button>
        </template>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import { listAgreements,addAgreement,updateAgreement,publishAgreement } from '@/api/library/agreement'
export default {name:'LibraryAgreement',data(){return{loading:false,rows:[],visible:false,readOnly: false,form:{}}},created(){this.load()},methods:{
  agreementTypeLabel(type){return type==='PRIVACY'?'用户隐私协议':type==='STATEMENT'?'网站声明':type==='FILE_DISCLAIMER'?'文件发送免责声明':type},
  load(){this.loading=true;listAgreements({pageNum:1,pageSize:100}).then(r=>{this.rows=r.rows||[]}).finally(()=>{this.loading=false})},
  view(row){this.readOnly = true;this.form={...row};this.visible=true},
  open(row){this.readOnly = false;this.form=row?{...row}:{agreementType:'PRIVACY',version:'',title:'',content:'',effectiveTime:'',status:'0'};this.visible=true},
  submit(){if(!this.form.version||!this.form.title||!this.form.content||!this.form.effectiveTime)return this.$modal.msgError('请完整填写协议版本、标题、内容和生效时间');(this.form.id?updateAgreement:addAgreement)(this.form).then(()=>{this.$modal.msgSuccess('协议草稿已保存');this.visible=false;this.load()})},
  publish(row){this.$modal.confirm(`确认发布协议“${row.title}”吗？未来生效版本会在生效时间到达后启用。`).then(()=>publishAgreement(row.id)).then(()=>{this.$modal.msgSuccess('发布成功');this.load()})}
}}
</script><style scoped>.mt16{margin-top:16px}</style>
