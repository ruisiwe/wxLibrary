<template><simple-list title="会员套餐" :loader="listVipPlans" :creator="addVipPlan" :updater="updateVipPlan" :remover="deleteVipPlan" :columns="columns" :form-fields="fields" :default-form="defaults" :permissions="permissions"/></template>
<script>
import SimpleList from '@/views/library/common/SimpleList'
import { listVipPlans,addVipPlan,updateVipPlan,deleteVipPlan } from '@/api/library/vip'
export default { name:'LibraryVipPlan',components:{SimpleList},data(){return{
  listVipPlans,addVipPlan,updateVipPlan,deleteVipPlan,permissions:{add:'library:vip:plan',edit:'library:vip:plan',remove:'library:vip:plan'},
  defaults:{planCode:'',planName:'',priceCent:0,validDays:30,giftPoints:0,sortOrder:0,status:'0'},
  columns:[{prop:'planCode',label:'套餐编码'},{prop:'planName',label:'套餐名称'},{prop:'priceCent',label:'价格（分）'},{prop:'validDays',label:'有效天数'},{prop:'giftPoints',label:'赠送积分'},{prop:'status',label:'状态',options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}],
  fields:[{prop:'planCode',label:'套餐编码',required:true,maxlength:32},{prop:'planName',label:'套餐名称',required:true},{prop:'priceCent',label:'价格（分）',type:'number',required:true},{prop:'validDays',label:'有效天数',type:'select',required:true,options:[{value:30,label:'月卡（30天）'},{value:365,label:'年卡（365天）'}]},{prop:'giftPoints',label:'赠送积分',type:'number',required:true},{prop:'sortOrder',label:'排序',type:'number',required:true},{prop:'status',label:'状态',type:'select',required:true,options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}]
}} }
</script>
