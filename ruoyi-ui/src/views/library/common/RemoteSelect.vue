<template>
  <div class="remote-select">
    <el-select
      :value="value"
      filterable
      remote
      reserve-keyword
      :placeholder="field.placeholder || '请输入关键词搜索'"
      :remote-method="queueSearch"
      :loading="loading"
      :no-data-text="error || field.emptyText || '暂无可选数据'"
      style="width:100%"
      @input="$emit('input', $event)"
      @change="onChange"
      @visible-change="onVisibleChange"
    >
      <el-option
        v-for="option in options"
        :key="optionValue(option)"
        :label="optionLabel(option)"
        :value="optionValue(option)"
        :disabled="optionDisabled(option)"
      />
    </el-select>
    <div v-if="selectionInvalid" class="remote-select__warning">
      {{ field.invalidMessage || '当前选项不可用，请重新选择' }}
    </div>
    <div v-if="error" class="remote-select__error">{{ error }}</div>
  </div>
</template>

<script>
export default {
  name: 'LibraryRemoteSelect',
  props: {
    value: { type: [String, Number], default: null },
    field: { type: Object, required: true },
    row: { type: Object, required: true }
  },
  data() {
    return { options: [], loading: false, error: '', timer: null, requestSequence: 0 }
  },
  computed: {
    selectionInvalid() {
      const selected = this.findOption(this.value, this.options)
      return Boolean(selected && this.optionDisabled(selected))
    }
  },
  watch: {
    row: {
      immediate: true,
      handler() { this.seedInitialOption() }
    }
  },
  beforeDestroy() {
    if (this.timer) clearTimeout(this.timer)
    this.requestSequence += 1
  },
  methods: {
    seedInitialOption() {
      if (this.timer) clearTimeout(this.timer)
      this.timer = null
      this.requestSequence += 1
      this.loading = false
      const initial = this.field.initialOption ? this.field.initialOption(this.row) : null
      this.options = initial ? [initial] : []
      this.error = ''
    },
    optionValue(option) {
      return option[this.field.optionValue || 'value']
    },
    optionLabel(option) {
      if (this.field.optionLabel) return this.field.optionLabel(option)
      return option.label
    },
    optionDisabled(option) {
      return this.field.optionDisabled ? this.field.optionDisabled(option) : false
    },
    findOption(value, options) {
      return (options || []).find(option => String(this.optionValue(option)) === String(value))
    },
    mergeSelected(items) {
      const next = Array.isArray(items) ? items.slice() : []
      const selected = this.findOption(this.value, this.options)
      if (selected && !this.findOption(this.value, next)) next.unshift(selected)
      return next
    },
    queueSearch(keyword) {
      if (this.timer) clearTimeout(this.timer)
      this.timer = setTimeout(() => this.loadOptions(keyword), this.field.debounce || 300)
    },
    loadOptions(keyword) {
      if (!this.field.remoteLoader) return
      const sequence = ++this.requestSequence
      this.loading = true
      this.error = ''
      Promise.resolve(this.field.remoteLoader(keyword || ''))
        .then(items => {
          if (sequence === this.requestSequence) this.options = this.mergeSelected(items)
        })
        .catch(() => {
          if (sequence === this.requestSequence) {
            this.error = this.field.loadErrorText || '搜索失败，请稍后重试'
          }
        })
        .finally(() => {
          if (sequence === this.requestSequence) this.loading = false
        })
    },
    onVisibleChange(visible) {
      if (visible) this.loadOptions('')
    },
    onChange(value) {
      this.$emit('selection-change', this.findOption(value, this.options) || null)
    }
  }
}
</script>

<style scoped>
.remote-select__warning{margin-top:6px;color:#e6a23c;font-size:12px;line-height:18px}
.remote-select__error{margin-top:6px;color:#f56c6c;font-size:12px;line-height:18px}
</style>
