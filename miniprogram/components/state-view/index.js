Component({ properties: { loading: Boolean, empty: Boolean, error: String, emptyText: { type: String, value: '暂无内容' } }, methods: { retry() { this.triggerEvent('retry'); } } });
