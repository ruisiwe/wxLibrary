<template>
  <div class="category-icon-picker">
    <el-button class="category-icon-picker__trigger" @click="open">
      <icon :name="displayName" size="24px" />
      <span>{{ selectedOption ? selectedOption.label : '请选择图标' }}</span>
      <small>{{ value || '' }}</small>
      <i class="el-icon-arrow-down" />
    </el-button>

    <el-dialog
      title="选择分类图标"
      :visible.sync="visible"
      width="620px"
      append-to-body
    >
      <el-input
        v-model.trim="keyword"
        clearable
        prefix-icon="el-icon-search"
        placeholder="搜索中文名称、图标名称或关键词"
      />
      <div v-loading="loading" class="category-icon-picker__body">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          :closable="false"
          show-icon
        >
          <el-button type="text" @click="loadOptions">重新加载</el-button>
        </el-alert>
        <div v-else class="category-icon-picker__grid">
          <button
            v-for="item in filteredOptions"
            :key="item.name"
            type="button"
            class="category-icon-picker__item"
            :class="{ 'is-selected': item.name === value }"
            @click="select(item)"
          >
            <icon :name="item.name" size="30px" />
            <span>{{ item.label }}</span>
            <small>{{ item.name }}</small>
          </button>
        </div>
        <div v-if="!loading && !error && !filteredOptions.length" class="category-icon-picker__empty">
          没有匹配的图标
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Icon } from 'tdesign-icons-vue'
import { listCategoryIconOptions } from '@/api/library/content'

export default {
  name: 'CategoryIconPicker',
  components: { Icon },
  props: {
    value: { type: String, default: '' }
  },
  data() {
    return {
      visible: false,
      loading: false,
      loaded: false,
      error: '',
      keyword: '',
      options: []
    }
  },
  computed: {
    selectedOption() {
      return this.options.find(item => item.name === this.value) || null
    },
    displayName() {
      return this.selectedOption ? this.selectedOption.name : 'file'
    },
    filteredOptions() {
      const keyword = this.keyword.toLowerCase()
      if (!keyword) return this.options
      return this.options.filter(item => [item.name, item.label, item.keywords]
        .filter(Boolean)
        .some(value => value.toLowerCase().includes(keyword)))
    }
  },
  created() {
    this.loadOptions()
  },
  methods: {
    open() {
      this.visible = true
      if (!this.loaded) this.loadOptions()
    },
    loadOptions() {
      this.loading = true
      this.error = ''
      listCategoryIconOptions().then(response => {
        this.options = response.data || []
        this.loaded = true
      }).catch(() => {
        this.error = '分类图标加载失败，请重试'
      }).finally(() => {
        this.loading = false
      })
    },
    select(item) {
      this.$emit('input', item.name)
      this.visible = false
    }
  }
}
</script>

<style scoped>
.category-icon-picker__trigger{width:100%;display:flex;align-items:center;gap:10px}
.category-icon-picker__trigger span{flex:1;text-align:left}
.category-icon-picker__trigger small{color:#909399}
.category-icon-picker__body{min-height:260px;margin-top:16px}
.category-icon-picker__grid{display:grid;grid-template-columns:repeat(6,1fr);gap:12px;max-height:360px;overflow:auto}
.category-icon-picker__item{display:flex;flex-direction:column;align-items:center;gap:6px;padding:12px 6px;border:1px solid #dcdfe6;border-radius:6px;background:#fff;cursor:pointer}
.category-icon-picker__item:hover,.category-icon-picker__item.is-selected{color:#409eff;border-color:#409eff;background:#ecf5ff}
.category-icon-picker__item small{color:#909399;max-width:82px;overflow:hidden;text-overflow:ellipsis}
.category-icon-picker__empty{text-align:center;color:#909399;padding:80px 0}
</style>
