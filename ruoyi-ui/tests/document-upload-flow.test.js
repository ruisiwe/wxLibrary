const assert = require('assert')
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')

const root = path.resolve(__dirname, '..')
const viewPath = path.join(root, 'src/views/library/content/document/index.vue')
const apiPath = path.join(root, 'src/api/library/content.js')

function read(file) { return fs.readFileSync(file, 'utf8') }

const view = read(viewPath)
const api = read(apiPath)
const component = compiler.parseComponent(view)
const compiled = compiler.compile(component.template.content)
assert.deepStrictEqual(compiled.errors, [], `文档管理模板编译失败：${compiled.errors.join('；')}`)

assert(view.includes('处理文件'), '新增文档弹窗应先处理浏览器选择的文件')
assert(view.includes('preparedSession'), '处理完成后应保存临时会话信息')
assert(view.includes('thumbnailBlobUrl'), '受保护缩略图应通过鉴权请求转为 Blob 预览')
assert(view.includes(':before-close="beforeClose"'), '关闭新增弹窗应取消未提交会话')
assert(view.includes('replaceThumbnail'), '新增文档弹窗应允许替换缩略图')
assert(view.includes('replaceSavedThumbnailFile'), '修改文档弹窗应允许替换已保存的缩略图')
assert(view.includes("hasPermiAnd(['library:document:add', 'library:document:upload'])"), '新增入口必须同时校验新增和上传权限')
assert(!view.includes('label="预览页数"'), '管理端不能手工填写试看页数')
assert(!view.includes('label="封面地址"'), '管理端不能手工填写封面地址')
assert(!view.includes('上传原文件'), '列表不能保留旧的独立上传入口')
assert(!view.includes('执行转换'), '文档新增流程不能暴露手工转换步骤')

assert(api.includes("url: '/library/document-upload/prepare'"), '前端应调用文档预处理接口')
assert(api.includes('/library/document-upload/session/${sessionId}/thumbnail'), '前端应调用会话缩略图接口')
assert(api.includes('/library/document-upload/session/${sessionId}/commit'), '前端应调用最终确认接口')
assert(api.includes("responseType: 'blob'"), '缩略图读取应使用带鉴权的 Blob 请求')
assert(api.includes('/library/document-upload/document/${documentId}/thumbnail'), '前端应调用已保存文档缩略图替换接口')

console.log('后台文档两阶段上传契约测试通过')
