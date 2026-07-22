<template>
  <simple-list
    title="宣传图片"
    :loader="listBanners"
    :creator="addBanner"
    :updater="updateBanner"
    :remover="deleteBanner"
    :columns="columns"
    :form-fields="fields"
    :default-form="defaults"
    :permissions="permissions"
  />
</template>

<script>
import SimpleList from '@/views/library/common/SimpleList'
import {
  listBanners,
  addBanner,
  updateBanner,
  deleteBanner,
  listBannerDocumentOptions
} from '@/api/library/content'

export default {
  name: 'LibraryBanner',
  components: { SimpleList },
  data() {
    return {
      listBanners,
      addBanner,
      updateBanner,
      deleteBanner,
      permissions: { add: 'library:banner:add', edit: 'library:banner:edit', remove: 'library:banner:remove' },
      defaults: {
        title: '', imageUrl: '', documentId: null, documentSelectable: null,
        sortOrder: 0, status: '0', startTime: null, endTime: null
      },
      columns: [
        { prop: 'id', label: '编号' },
        { prop: 'title', label: '标题' },
        { prop:'documentTitle', label: '关联文档' },
        { prop: 'sortOrder', label: '排序' },
        { prop: 'status', label: '状态', options: [{ value: '0', label: '启用' }, { value: '1', label: '停用' }] }
      ],
      fields: [
        { prop: 'title', label: '标题', required: true },
        { prop: 'imageUrl', label: '图片地址', required: true },
        {
          prop: 'documentId', label: '关联文档', type:'remote-select', required: true,
          requiredMessage: '请选择关联文档',
          placeholder: '请输入文档标题搜索', optionValue: 'id', debounce: 300,
          remoteLoader: keyword => this.searchDocumentOptions(keyword),
          optionLabel: option => this.documentOptionLabel(option),
          optionDisabled: option => option.documentSelectable === false,
          initialOption: row => this.initialDocumentOption(row),
          selectableProp: 'documentSelectable',
          validate: form => !form.documentId || form.documentSelectable !== false,
          validationMessage: '原关联文档已下架，请重新选择',
          invalidMessage: '原关联文档已下架，请重新选择',
          emptyText: '未找到可关联的已发布文档',
          loadErrorText: '关联文档搜索失败，请稍后重试'
        },
        { prop: 'sortOrder', label: '排序', type: 'number', required: true },
        { prop: 'status', label: '状态', type: 'select', required: true, options: [{ value: '0', label: '启用' }, { value: '1', label: '停用' }] },
        { prop: 'startTime', label: '开始时间', type: 'datetime' },
        { prop: 'endTime', label: '结束时间', type: 'datetime' }
      ]
    }
  },
  methods: {
    searchDocumentOptions(keyword) {
      return listBannerDocumentOptions({ keyword, pageNum: 1, pageSize: 20 }).then(response => {
        const data = response.data || {}
        return (data.items || []).map(item => ({ ...item, documentSelectable: true }))
      })
    },
    documentOptionLabel(option) {
      return [option.title, option.categoryName, option.fileFormat].filter(Boolean).join(' / ')
    },
    initialDocumentOption(row) {
      if (!row || !row.documentId) return null
      return {
        id: row.documentId,
        title: row.documentTitle || '原关联文档',
        categoryName: row.documentCategoryName,
        fileFormat: row.documentFileFormat,
        documentSelectable: row.documentSelectable !== false
      }
    }
  }
}
</script>
