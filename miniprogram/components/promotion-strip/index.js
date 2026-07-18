Component({
  properties: { items: { type: Array, value: [] } },
  methods: {
    open(event) {
      const item = event.currentTarget.dataset.item;
      if (item && item.linkPath) wx.navigateTo({ url: item.linkPath });
    }
  }
});
