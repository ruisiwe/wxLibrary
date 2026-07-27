const assert = require('assert')
const fs = require('fs')
const path = require('path')

const menu = fs.readFileSync(
  path.resolve(__dirname, '../../sql/wechat_library_menu.sql'),
  'utf8'
)

assert(menu.includes("'VIP 权益介绍'"), '会员管理下应新增“VIP 权益介绍”菜单')
assert(menu.includes("'library/vip/benefit/index'"), '菜单应指向VIP权益介绍维护页')
assert(menu.includes("'library:vip:benefit:list'"), '菜单应包含权益查询权限')
assert(menu.includes("'library:vip:benefit:add'"), '菜单应包含权益新增权限')
assert(menu.includes("'library:vip:benefit:edit'"), '菜单应包含权益修改权限')
assert(menu.includes("'library:vip:benefit:remove'"), '菜单应包含权益删除权限')
assert(menu.includes("'library:vip:page-config:query'"), '菜单应包含客服配置查询权限')
assert(menu.includes("'library:vip:page-config:edit'"), '菜单应包含客服配置修改权限')

console.log('VIP权益介绍菜单契约测试通过')
