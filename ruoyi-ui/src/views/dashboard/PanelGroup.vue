<template>
  <el-row :gutter="24" class="panel-group">
    <el-col v-for="item in panels" :key="item.key" :xs="12" :sm="12" :lg="6" class="card-panel-col">
      <div class="card-panel">
        <div :class="['card-panel-icon-wrapper', item.iconClass]">
          <svg-icon :icon-class="item.icon" class-name="card-panel-icon" />
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">{{ item.label }}</div>
          <count-to :start-val="0" :end-val="numberValue(summary[item.key])" :duration="1200" class="card-panel-num" />
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script>
import CountTo from 'vue-count-to'

export default {
  name: 'PanelGroup',
  components: { CountTo },
  props: {
    summary: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      panels: [
        { key: 'userCount', label: '用户数', icon: 'peoples', iconClass: 'icon-people' },
        { key: 'memberCount', label: '会员数', icon: 'user', iconClass: 'icon-member' },
        { key: 'documentCount', label: '文档数', icon: 'documentation', iconClass: 'icon-document' },
        { key: 'paidDocumentCount', label: '付费文档数', icon: 'money', iconClass: 'icon-paid' }
      ]
    }
  },
  methods: {
    numberValue(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number : 0
    }
  }
}
</script>

<style lang="scss" scoped>
.panel-group {
  .card-panel-col {
    margin-bottom: 24px;
  }

  .card-panel {
    display: flex;
    align-items: center;
    height: 108px;
    overflow: hidden;
    color: #666;
    background: #fff;
    border-radius: 6px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  }

  .card-panel-icon-wrapper {
    margin-left: 16px;
    padding: 16px;
    border-radius: 6px;
  }

  .card-panel-icon {
    font-size: 44px;
  }

  .icon-people { color: #40c9c6; background: rgba(64, 201, 198, 0.1); }
  .icon-member { color: #36a3f7; background: rgba(54, 163, 247, 0.1); }
  .icon-document { color: #34bfa3; background: rgba(52, 191, 163, 0.1); }
  .icon-paid { color: #f4516c; background: rgba(244, 81, 108, 0.1); }

  .card-panel-description {
    flex: 1;
    margin-left: 18px;
  }

  .card-panel-text {
    margin-bottom: 10px;
    color: rgba(0, 0, 0, 0.45);
    font-size: 15px;
    font-weight: 600;
  }

  .card-panel-num {
    color: #303133;
    font-size: 24px;
    font-weight: 700;
  }
}

@media (max-width: 550px) {
  .panel-group .card-panel-icon-wrapper {
    display: none;
  }

  .panel-group .card-panel-description {
    margin-left: 16px;
  }
}
</style>
