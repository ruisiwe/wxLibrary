<template><simple-list title="文档分类" :loader="listCategories" :creator="addCategory" :updater="updateCategory" :remover="deleteCategory" :columns="columns" :form-fields="fields" :default-form="defaults" :permissions="permissions"/></template>
<script>
import SimpleList from '@/views/library/common/SimpleList'
import { listCategories, addCategory, updateCategory, deleteCategory } from '@/api/library/content'
export default { name:'LibraryCategory', components:{SimpleList}, data(){return{
  listCategories,addCategory,updateCategory,deleteCategory,
  permissions:{add:'library:category:add',edit:'library:category:edit',remove:'library:category:remove'},
  defaults:{name:'',icon:'',sortOrder:0,status:'0'},
  columns:[{prop:'id',label:'编号'},{prop:'name',label:'分类名称'},{prop:'icon',label:'图标'},{prop:'sortOrder',label:'排序'},{prop:'status',label:'状态',options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}],
  fields:[{prop:'name',label:'分类名称',required:true,maxlength:64},{prop:'icon',label:'图标地址'},{prop:'sortOrder',label:'排序',type:'number',required:true},{prop:'status',label:'状态',type:'select',required:true,options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}]
}} }
</script>
