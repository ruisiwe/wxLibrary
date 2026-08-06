import request from '@/utils/request'

const pageConfigFormData = (config, image) => {
  const formData = new FormData()
  formData.append('config', new Blob([JSON.stringify(config)], { type: 'application/json' }))
  if (image) formData.append('image', image, image.name || 'customer-service-image')
  return formData
}

export const listVipBenefits = query => request({
  url: '/library/vip-benefit/list',
  method: 'get',
  params: query
})

export const getVipBenefit = id => request({
  url: `/library/vip-benefit/${id}`,
  method: 'get'
})

export const addVipBenefit = data => request({
  url: '/library/vip-benefit',
  method: 'post',
  data
})

export const updateVipBenefit = data => request({
  url: '/library/vip-benefit',
  method: 'put',
  data
})

export const deleteVipBenefit = id => request({
  url: `/library/vip-benefit/${id}`,
  method: 'delete'
})

export const getVipPageConfig = () => request({
  url: '/library/vip-page-config',
  method: 'get'
})

export const updateVipPageConfig = (config, image) => request({
  url: '/library/vip-page-config',
  method: 'put',
  data: pageConfigFormData(config, image),
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
})

export const clearVipPageConfigImage = () => request({
  url: '/library/vip-page-config/image',
  method: 'delete'
})
