<template>
  <simple-list
    plain
    title="会员套餐"
    :loader="listVipPlans"
    :creator="addVipPlan"
    :updater="updateVipPlan"
    :remover="deleteVipPlan"
    :columns="columns"
    :form-fields="fields"
    :default-form="defaults"
    :permissions="permissions"
  >
    <template slot="column-priceCent" slot-scope="{ value }">
      ¥{{ formatYuan(value) }}
    </template>
    <template slot="field-priceCent" slot-scope="{ form }">
      <el-input-number
        :value="centToYuan(form.priceCent)"
        :min="0"
        :precision="2"
        :step="0.01"
        controls-position="right"
        @input="setPriceYuan(form, $event)"
      />
    </template>
  </simple-list>
</template>
<script>
import SimpleList from '@/views/library/common/SimpleList'
import { listVipPlans,addVipPlan,updateVipPlan,deleteVipPlan } from '@/api/library/vip'
export default { name:'LibraryVipPlan',components:{SimpleList},data(){return{
  listVipPlans,addVipPlan,updateVipPlan,deleteVipPlan,permissions:{add:'library:vip:plan',edit:'library:vip:plan',remove:'library:vip:plan'},
  defaults:{planCode:'',planName:'',priceCent:0,validDays:30,giftPoints:0,sortOrder:0,status:'0'},
  columns:[{prop:'planCode',label:'套餐编码'},{prop:'planName',label:'套餐名称'},{prop:'priceCent',label:'价格（元）'},{prop:'validDays',label:'有效天数'},{prop:'giftPoints',label:'赠送积分'},{prop:'status',label:'状态',options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}],
  fields:[{prop:'planCode',label:'套餐编码',required:true,maxlength:32},{prop:'planName',label:'套餐名称',required:true},{prop:'priceCent',label:'价格（元）',type:'number',required:true},{prop:'validDays',label:'有效天数（天）',type:'number',required:true,min:1,max:3650,precision:0},{prop:'giftPoints',label:'赠送积分',type:'number',required:true},{prop:'sortOrder',label:'排序',type:'number',required:true},{prop:'status',label:'状态',type:'select',required:true,options:[{value:'0',label:'启用'},{value:'1',label:'停用'}]}]
}},methods:{
  centToYuan(value){return Number(value || 0) / 100},
  setPriceYuan(form,value){this.$set(form,'priceCent',Math.round(Number(value || 0) * 100))},
  formatYuan(value){return this.centToYuan(value).toFixed(2)}
} }
</script>
