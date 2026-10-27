import request from '@/utils/request'

/**
 * SOYSHTTPOverMC-ERP 扩展 API
 * baseURL 由 SOYS 契约注入（生产 = /api/plugins/SOYSHTTPOverMC-ERP，开发 = /dev-api 代理）
 * 路径与后端 Controller 类级前缀 /erp/* 一一对应
 */

/* ================= 用户 ================= */
export function listUser(params) {
  return request({ url: '/erp/user/list', method: 'get', params })
}
export function listUserGroups(uuid) {
  return request({ url: '/erp/user/groups', method: 'get', params: { uuid } })
}
export function setUserGroups(data) {
  return request({ url: '/erp/user/groups', method: 'post', data })
}
export function listUserPerms(uuid) {
  return request({ url: '/erp/user/perms', method: 'get', params: { uuid } })
}
export function addUserPerm(data) {
  return request({ url: '/erp/user/perm/add', method: 'post', data })
}
export function removeUserPerm(data) {
  return request({ url: '/erp/user/perm/remove', method: 'post', data })
}
export function setUserExpiry(data) {
  return request({ url: '/erp/user/expiry', method: 'post', data })
}
// 生成并绑定 X-API-KEY（返回明文，仅此一次）
export function assignApiKey(uuid) {
  return request({ url: '/erp/user/assign-key', method: 'post', data: { uuid } })
}

/* ================= 权限组 ================= */
export function listGroup(params) {
  return request({ url: '/erp/group/list', method: 'get', params })
}
export function createGroup(data) {
  return request({ url: '/erp/group/create', method: 'post', data })
}
export function updateGroup(data) {
  return request({ url: '/erp/group/update', method: 'post', data })
}
export function removeGroup(id) {
  return request({ url: '/erp/group/remove', method: 'post', data: { id } })
}
export function listGroupPerms(id) {
  return request({ url: '/erp/group/perms', method: 'get', params: { id } })
}
export function addGroupPerm(data) {
  return request({ url: '/erp/group/perm/add', method: 'post', data })
}
export function removeGroupPerm(data) {
  return request({ url: '/erp/group/perm/remove', method: 'post', data })
}
// 组间批量复制：把 sourceGroups 的全部权限节点加入 targetGroup（跳过已存在）
export function copyGroupPerms(data) {
  return request({ url: '/erp/group/perm/copy', method: 'post', data })
}
// 组间批量移除：删除 targetGroup 中与 sourceGroups 相同的权限节点
export function removeByGroupPerms(data) {
  return request({ url: '/erp/group/perm/remove-by', method: 'post', data })
}
export function listGroupMembers(id) {
  return request({ url: '/erp/group/members', method: 'get', params: { id } })
}
export function addGroupMember(data) {
  return request({ url: '/erp/group/member/add', method: 'post', data })
}
export function removeGroupMember(data) {
  return request({ url: '/erp/group/member/remove', method: 'post', data })
}

/* ================= X-API-KEY ================= */
export function listApiKey(params) {
  return request({ url: '/erp/apikey/list', method: 'get', params })
}
export function generateApiKey(data) {
  return request({ url: '/erp/apikey/generate', method: 'post', data })
}
export function toggleApiKey(data) {
  return request({ url: '/erp/apikey/toggle', method: 'post', data })
}
export function setApiKeyExpiry(data) {
  return request({ url: '/erp/apikey/expiry', method: 'post', data })
}
export function bindApiKey(data) {
  return request({ url: '/erp/apikey/bind', method: 'post', data })
}
export function unbindApiKey(data) {
  return request({ url: '/erp/apikey/unbind', method: 'post', data })
}
export function removeApiKey(keyId) {
  return request({ url: '/erp/apikey/remove', method: 'post', data: { keyId } })
}
export function listApiKeyPerms(keyId) {
  return request({ url: '/erp/apikey/perms', method: 'get', params: { keyId } })
}
export function addApiKeyPerm(data) {
  return request({ url: '/erp/apikey/perm/add', method: 'post', data })
}
export function removeApiKeyPerm(data) {
  return request({ url: '/erp/apikey/perm/remove', method: 'post', data })
}

/* ================= 配置文件（插件配置 / 网关配置） ================= */
export function listConfigFiles() {
  return request({ url: '/erp/config/files', method: 'get' })
}
export function loadConfigFile(file) {
  return request({ url: '/erp/config/load', method: 'get', params: { file } })
}
export function saveConfigFile(file, data) {
  return request({ url: '/erp/config/save', method: 'post', params: { file }, data })
}

/* ================= 语言包管理 ================= */
export function listLangFiles() {
  return request({ url: '/erp/lang/list', method: 'get' })
}
export function loadLangEntries(file) {
  return request({ url: '/erp/lang/entries', method: 'get', params: { file } })
}
export function saveLangEntries(file, entries) {
  return request({ url: '/erp/lang/save', method: 'post', params: { file }, data: entries })
}

