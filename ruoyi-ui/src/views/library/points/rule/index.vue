<template><simple-list title="积分规则" :loader="listPointRules" :updater="updatePointRule" :columns="columns" :form-fields="fields" :permissions="permissions"/></template>
<script>
import SimpleList from '@/views/library/common/SimpleList'
import { listPointRules,updatePointRule } from '@/api/library/points'
export default { name:'LibraryPointRule',components:{SimpleList},data(){return{
  listPointRules,updatePointRule,permissions:{edit:'library:points:rule'},
  columns:[{prop:'eventType',label:'事件类型'},{prop:'ruleName',label:'规则名称'},{prop:'pointValue',label:'奖励积分'},{prop:'dailyLimit',label:'每日上限'},{prop:'status',label:'状态',options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}],
  fields:[{prop:'pointValue',label:'奖励积分',type:'number',required:true},{prop:'dailyLimit',label:'每日上限',type:'number',required:true,min:1,max:5},{prop:'status',label:'状态',type:'select',required:true,options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]},{prop:'remark',label:'备注',type:'textarea',maxlength:500}]
}} }
</script>
