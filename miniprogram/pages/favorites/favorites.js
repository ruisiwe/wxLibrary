const documents = require('../../services/document');
Page({ data:{items:[],loading:true,error:''},onLoad(){this.load()},load(){this.setData({loading:true,error:''});documents.favorites().then(items=>this.setData({items:items||[],loading:false})).catch(error=>this.setData({loading:false,error:error.message}))} });
