import request from '@/utils/request'

const bannerFormData = (banner, image) => {
  const formData = new FormData()
  formData.append('banner', new Blob([JSON.stringify(banner)], { type: 'application/json' }))
  if (image) formData.append('image', image, 'banner.jpg')
  return formData
}

export const listBanners = query => request({ url: '/library/banner/list', method: 'get', params: query })
export const addBanner = (banner, image) => request({
  url: '/library/banner', method: 'post', data: bannerFormData(banner, image),
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
})
export const updateBanner = (banner, image) => request({
  url: '/library/banner', method: 'put', data: bannerFormData(banner, image),
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
})
export const deleteBanner = id => request({ url: `/library/banner/${id}`, method: 'delete' })
export const getBannerImage = id => request({ url: `/library/banner/${id}/image`, method: 'get' })
export const listBannerDocumentOptions = query => request({
  url: '/library/banner/document-options', method: 'get', params: query
})
export const listCategories = query => request({ url: '/library/category/list', method: 'get', params: query })
export const addCategory = data => request({ url: '/library/category', method: 'post', data })
export const updateCategory = data => request({ url: '/library/category', method: 'put', data })
export const deleteCategory = id => request({ url: `/library/category/${id}`, method: 'delete' })
export const listDocuments = query => request({ url: '/library/document/list', method: 'get', params: query })
export const addDocument = data => request({ url: '/library/document', method: 'post', data })
export const updateDocument = data => request({ url: '/library/document', method: 'put', data })
export const deleteDocument = id => request({ url: `/library/document/${id}`, method: 'delete' })
export const publishDocument = id => request({ url: `/library/document/${id}/publish`, method: 'put' })
export const unpublishDocument = id => request({ url: `/library/document/${id}/unpublish`, method: 'put' })
export const prepareDocumentUpload = data => request({
  url: '/library/document-upload/prepare', method: 'post', data,
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }, timeout: 180000
})
export const getDocumentUploadThumbnail = sessionId => request({
  url: `/library/document-upload/session/${sessionId}/thumbnail`, method: 'get', responseType: 'blob'
})
export const replaceDocumentUploadThumbnail = (sessionId, data) => request({
  url: `/library/document-upload/session/${sessionId}/thumbnail`, method: 'put', data,
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }, timeout: 30000
})
export const replaceSavedDocumentThumbnail = (documentId, data) => request({
  url: `/library/document-upload/document/${documentId}/thumbnail`, method: 'put', data,
  headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }, timeout: 30000
})
export const commitDocumentUpload = (sessionId, data) => request({
  url: `/library/document-upload/session/${sessionId}/commit`, method: 'post', data, timeout: 180000
})
export const cancelDocumentUpload = sessionId => request({
  url: `/library/document-upload/session/${sessionId}`, method: 'delete'
})
