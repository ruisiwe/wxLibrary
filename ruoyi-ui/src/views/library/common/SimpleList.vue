<template>
  <div :class="{ 'app-container': !embedded }">
    <component :is="plain ? 'div' : 'el-card'" shadow="never">
      <div v-if="!plain" slot="header" class="toolbar">
        <span>{{ title }}</span>
        <div>
          <el-button v-if="creator" v-hasPermi="[permissions.add]" type="primary" size="mini" icon="el-icon-plus" @click="openDialog()">新增</el-button>
          <el-button size="mini" icon="el-icon-refresh" @click="loadData">刷新</el-button>
        </div>
      </div>
      <el-row v-else-if="!embedded" :gutter="10" class="mb8">
        <el-col v-if="creator" :span="1.5">
          <el-button
            v-hasPermi="[permissions.add]"
            type="primary"
            plain
            size="mini"
            icon="el-icon-plus"
            @click="openDialog()"
          >新增</el-button>
        </el-col>
        <right-toolbar :search="false" @queryTable="loadData" />
      </el-row>
      <el-table
        v-loading="loading"
        :data="rows"
        :border="!plain"
        :stripe="!plain"
      >
        <el-table-column v-for="column in columns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.width || 120" show-overflow-tooltip>
          <template slot-scope="scope">
            <slot
              :name="'column-' + column.prop"
              :row="scope.row"
              :value="scope.row[column.prop]"
              :column="column"
            >
              {{ displayValue(scope.row[column.prop], column) }}
            </slot>
          </template>
        </el-table-column>
        <el-table-column v-if="updater || remover" label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="updater" v-hasPermi="[permissions.edit]" type="text" @click="openDialog(scope.row)">修改</el-button>
            <el-button v-if="remover" v-hasPermi="[permissions.remove]" type="text" class="danger" @click="removeRow(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="loadData" />
    </component>

    <el-dialog :title="form.id ? `修改${title}` : `新增${title}`" :visible.sync="visible" width="620px" append-to-body>
      <el-form ref="form" :model="form" label-width="120px">
        <el-form-item v-for="field in formFields" :key="field.prop" :label="field.label" :required="field.required">
          <slot :name="'field-' + field.prop" :form="form" :field="field">
            <el-input-number
              v-if="field.type === 'number'"
              v-model="form[field.prop]"
              :min="field.min === undefined ? 0 : field.min"
              :max="field.max"
              :precision="field.precision"
              controls-position="right"
            />
            <remote-select v-else-if="field.type === 'remote-select'" v-model="form[field.prop]" :field="field" :row="form" @selection-change="onRemoteSelectionChange(field, $event)" />
            <el-select v-else-if="field.type === 'select'" v-model="form[field.prop]" style="width:100%">
              <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-date-picker v-else-if="field.type === 'datetime'" v-model="form[field.prop]" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
            <el-input v-else v-model="form[field.prop]" :type="field.type === 'textarea' ? 'textarea' : 'text'" :rows="field.rows || 4" :maxlength="field.maxlength" show-word-limit />
          </slot>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import RemoteSelect from '@/views/library/common/RemoteSelect'

export default {
  name: 'LibrarySimpleList',
  components: { RemoteSelect },
  props: {
    title: { type: String, required: true },
    plain: { type: Boolean, default: false },
    embedded: { type: Boolean, default: false },
    loader: { type: Function, required: true },
    columns: { type: Array, required: true },
    creator: Function,
    updater: Function,
    remover: Function,
    formFields: { type: Array, default: () => [] },
    defaultForm: { type: Object, default: () => ({}) },
    permissions: { type: Object, default: () => ({ add: '', edit: '', remove: '' }) }
  },
  data() {
    return { loading: false, rows: [], total: 0, query: { pageNum: 1, pageSize: 10 }, visible: false, form: {} }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      this.loader(this.query).then(res => {
        this.rows = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    displayValue(value, column) {
      const option = column.options && column.options.find(item => item.value === value)
      return option ? option.label : value
    },
    openDialog(row) {
      this.form = row ? { ...row } : { ...this.defaultForm }
      this.visible = true
    },
    submit() {
      const missing = this.formFields.find(field => field.required && (this.form[field.prop] === undefined || this.form[field.prop] === null || String(this.form[field.prop]).trim() === ''))
      if (missing) return this.$modal.msgError(missing.requiredMessage || `请填写${missing.label}`)
      const invalid = this.formFields.find(field => field.validate && !field.validate(this.form))
      if (invalid) return this.$modal.msgError(invalid.validationMessage || `${invalid.label}不正确`)
      const operation = this.form.id ? this.updater : this.creator
      operation(this.form).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.visible = false
        this.loadData()
      })
    },
    removeRow(row) {
      this.$modal.confirm(`确认删除${this.title}“${row.title || row.name || row.planName || row.id}”吗？`)
        .then(() => this.remover(row.id))
        .then(() => { this.$modal.msgSuccess('删除成功'); this.loadData() })
    },
    onRemoteSelectionChange(field, option) {
      if (!field.selectableProp) return
      const selectable = option ? !(field.optionDisabled && field.optionDisabled(option)) : null
      this.$set(this.form, field.selectableProp, selectable)
    }
  }
}
</script>

<style scoped>
.toolbar{display:flex;align-items:center;justify-content:space-between}.danger{color:#f56c6c}
</style>
