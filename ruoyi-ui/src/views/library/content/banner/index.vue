<template><simple-list title="宣传图片" :loader="listBanners" :creator="addBanner" :updater="updateBanner" :remover="deleteBanner" :columns="columns" :form-fields="fields" :default-form="defaults" :permissions="permissions"/></template>
<script>
import SimpleList from '@/views/library/common/SimpleList'
import { listBanners, addBanner, updateBanner, deleteBanner } from '@/api/library/content'
export default { name:'LibraryBanner', components:{SimpleList}, data(){return{
  listBanners,addBanner,updateBanner,deleteBanner,
  permissions:{add:'library:banner:add',edit:'library:banner:edit',remove:'library:banner:remove'},
  defaults:{title:'',imageUrl:'',documentId:null,sortOrder:0,status:'0',startTime:null,endTime:null},
  columns:[{prop:'id',label:'编号'},{prop:'title',label:'标题'},{prop:'documentId',label:'关联文档'},{prop:'sortOrder',label:'排序'},{prop:'status',label:'状态',options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}],
  fields:[{prop:'title',label:'标题',required:true},{prop:'imageUrl',label:'图片地址',required:true},{prop:'documentId',label:'关联文档编号',type:'number',required:true,min:1},{prop:'sortOrder',label:'排序',type:'number',required:true},{prop:'status',label:'状态',type:'select',required:true,options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]},{prop:'startTime',label:'开始时间',type:'datetime'},{prop:'endTime',label:'结束时间',type:'datetime'}]
}} }
</script>
