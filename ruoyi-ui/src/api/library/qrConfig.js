import request from '@/utils/request'

export const listQrConfigs = query => request({
  url: '/library/qr-config/list',
  method: 'get',
  params: query
})

export const getQrConfig = id => request({
  url: `/library/qr-config/${id}`,
  method: 'get'
})

export const addQrConfig = data => request({
  url: '/library/qr-config',
  method: 'post',
  data
})

export const updateQrConfig = data => request({
  url: '/library/qr-config',
  method: 'put',
  data
})

export const deleteQrConfig = id => request({
  url: `/library/qr-config/${id}`,
  method: 'delete'
})

export const uploadQrConfigImage = (id, image) => {
  const data = new FormData()
  data.append('image', image, image.name || 'qr-image')
  return request({
    url: `/library/qr-config/${id}/image`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  })
}

export const clearQrConfigImage = id => request({
  url: `/library/qr-config/${id}/image`,
  method: 'delete'
})

export const getQrConfigImage = id => request({
  url: `/library/qr-config/${id}/image`,
  method: 'get',
  responseType: 'blob'
})
