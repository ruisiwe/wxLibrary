import request from '@/utils/request'

// 查询后台首页全部文库统计数据
export function getDashboardStatistics() {
  return request({
    url: '/library/dashboard',
    method: 'get'
  })
}
