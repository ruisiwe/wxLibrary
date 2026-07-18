import request from '@/utils/request'
export const listAgreements = query => request({ url: '/library/agreement/list', method: 'get', params: query })
export const getAgreement = id => request({ url: `/library/agreement/${id}`, method: 'get' })
export const addAgreement = data => request({ url: '/library/agreement', method: 'post', data })
export const updateAgreement = data => request({ url: '/library/agreement', method: 'put', data })
export const publishAgreement = id => request({ url: `/library/agreement/${id}/publish`, method: 'put' })
