import request from '@/utils/request'

export const listBanners = query => request({ url: '/library/banner/list', method: 'get', params: query })
export const addBanner = data => request({ url: '/library/banner', method: 'post', data })
export const updateBanner = data => request({ url: '/library/banner', method: 'put', data })
export const deleteBanner = id => request({ url: `/library/banner/${id}`, method: 'delete' })
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
export const listConversions = query => request({ url: '/library/document-file/list', method: 'get', params: query })
export const uploadDocumentFile = (documentId, data) => request({
  url: `/library/document-file/document/${documentId}/upload`,
  method: 'post',
  data,
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const executeConversion = id => request({ url: `/library/document-file/${id}/execute`, method: 'post' })
export const retryConversion = id => request({ url: `/library/document-file/${id}/retry`, method: 'post' })
