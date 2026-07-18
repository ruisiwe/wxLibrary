import request from '@/utils/request'
export const listUsers = query => request({ url: '/library/wx-user/list', method: 'get', params: query })
export const getUser = id => request({ url: `/library/wx-user/${id}`, method: 'get' })
export const changeUserStatus = (id, status) => request({ url: `/library/wx-user/${id}/status`, method: 'put', params: { status } })
export const adjustUserPoints = (id, data) => request({ url: `/library/wx-user/${id}/points`, method: 'post', data })
