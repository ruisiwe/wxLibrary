import request from '@/utils/request'
export const listCourses = query => request({ url: '/library/course/list', method: 'get', params: query })
export const addCourse = data => request({ url: '/library/course', method: 'post', data })
export const updateCourse = data => request({ url: '/library/course', method: 'put', data })
export const listVideos = id => request({ url: `/library/course/${id}/videos`, method: 'get' })
export const saveVideo = data => request({ url: '/library/course/video', method: 'put', data })
export const listCourseCodes = query => request({ url: '/library/course-code/list', method: 'get', params: query })
export const generateCourseCodes = data => request({ url: '/library/course-code/generate', method: 'post', data })
