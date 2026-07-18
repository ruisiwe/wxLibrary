Component({
  properties: { items: { type: Array, value: [] } },
  methods: {
    open(event) {
      const id = event.currentTarget.dataset.id;
      wx.navigateTo({ url: `/pages/category/category?id=${id}` });
    }
  }
});
