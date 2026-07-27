<template>
  <simple-list
    title="文档分类"
    :loader="listCategories"
    :creator="addCategory"
    :updater="updateCategory"
    :remover="deleteCategory"
    :columns="columns"
    :form-fields="fields"
    :default-form="defaults"
    :permissions="permissions"
  >
    <template slot="column-icon" slot-scope="{ row }">
      <div class="category-icon-cell">
        <icon :name="isValidIcon(row.icon) ? row.icon : 'file'" size="22px" />
        <span>{{ row.icon || 'file' }}</span>
        <el-tag v-if="iconOptionsLoaded && !isValidIcon(row.icon)" size="mini" type="danger">
          图标配置已失效
        </el-tag>
      </div>
    </template>
    <template slot="field-icon" slot-scope="{ form }">
      <category-icon-picker
        :value="form.icon"
        @input="$set(form, 'icon', $event)"
      />
    </template>
  </simple-list>
</template>

<script>
import { Icon } from 'tdesign-icons-vue'
import SimpleList from '@/views/library/common/SimpleList'
import CategoryIconPicker from './CategoryIconPicker'
import {
  listCategories,
  listCategoryIconOptions,
  addCategory,
  updateCategory,
  deleteCategory
} from '@/api/library/content'

export default {
  name: 'LibraryCategory',
  components: { Icon, SimpleList, CategoryIconPicker },
  data() {
    return {
      listCategories,
      addCategory,
      updateCategory,
      deleteCategory,
      iconOptionsLoaded: false,
      validIconNames: [],
      permissions: {
        add: 'library:category:add',
        edit: 'library:category:edit',
        remove: 'library:category:remove'
      },
      defaults: { name: '', icon: '', sortOrder: 0, status: '0' },
      columns: [
        { prop: 'id', label: '编号' },
        { prop: 'name', label: '分类名称' },
        { prop: 'icon', label: '图标', width: 220 },
        { prop: 'sortOrder', label: '排序' },
        {
          prop: 'status',
          label: '状态',
          options: [
            { value: '0', label: '启用' },
            { value: '1', label: '停用' }
          ]
        }
      ],
      fields: [
        { prop: 'name', label: '分类名称', required: true, maxlength: 64 },
        {
          prop: 'icon',
          label: '分类图标',
          required: true,
          requiredMessage: '请选择分类图标'
        },
        { prop: 'sortOrder', label: '排序', type: 'number', required: true },
        {
          prop: 'status',
          label: '状态',
          type: 'select',
          required: true,
          options: [
            { value: '0', label: '启用' },
            { value: '1', label: '停用' }
          ]
        }
      ]
    }
  },
  created() {
    listCategoryIconOptions().then(response => {
      this.validIconNames = (response.data || []).map(item => item.name)
      this.iconOptionsLoaded = true
    }).catch(() => {
      this.iconOptionsLoaded = false
    })
  },
  methods: {
    isValidIcon(name) {
      return Boolean(name) && this.validIconNames.includes(name)
    }
  }
}
</script>

<style scoped>
.category-icon-cell{display:flex;align-items:center;gap:8px}
</style>
