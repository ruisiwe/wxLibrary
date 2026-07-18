const courses = require('../../services/course');
const vip = require('../../services/vip');
Page({
  data:{items:[],loading:true,error:''},onLoad(){this.load()},load(){this.setData({loading:true,error:''});Promise.all([courses.list(),courses.mine(),vip.profile()]).then(([all,grants,profile])=>this.setData({items:courses.mergeMyCourses(all,grants,profile.vipActive),loading:false})).catch(error=>this.setData({loading:false,error:error.message}))},open(event){wx.navigateTo({url:`/pages/course-detail/course-detail?id=${event.currentTarget.dataset.id}`})}
});
