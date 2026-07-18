import request from '@/utils/request'
export const listPointRules = query => request({ url: '/library/point-rule/list', method: 'get', params: query })
export const updatePointRule = data => request({ url: '/library/point-rule', method: 'put', data })
export const listPointRecords = query => request({ url: '/library/point-record/list', method: 'get', params: query })
