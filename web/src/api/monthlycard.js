import request from '@/utils/request'

/**
 * 用户中心 / 管理台接口（路由 /api/plugins/SOYSMonthlyCard/admin/*）。
 *
 * <p>注意：axios baseURL 已经是契约 {@code apiFullPrefix}（{@code /api/plugins/SOYSMonthlyCard}），
 * 且后端 {@code MonthlyCardController} 没有类级 {@code @RequestMapping}，方法直接挂 {@code /admin/*}。
 * 历史版本这里多写了一层 {@code /monthlycard}，实际请求会变成
 * {@code /api/plugins/SOYSMonthlyCard/admin/*} → 404，已按启动日志登记的
 * 真实路由（{@code 注册 API: GET /api/plugins/SOYSMonthlyCard/admin/tiers}）修正。</p>
 */

// ========== 玩家侧（凭证玩家，展示自身可领礼包） ==========
// 对应后端 @ApiPublic GET /view/tiers、/player/state（凭证由请求上下文解析）。
// 注：public/player/claim.html 的桥接脚本用同源 fetch 直连这两个端点，
// 此处封装供 SPA 内其它视图复用。
export function getPlayerTiers() {
  return request({
    url: '/view/tiers',
    method: 'get'
  })
}

export function getPlayerState() {
  return request({
    url: '/player/state',
    method: 'get'
  })
}

// ========== 概览 ==========
export function getOverview() {
  return request({
    url: '/admin/overview',
    method: 'get'
  })
}

// ========== 档位管理 ==========
export function getTiers() {
  return request({
    url: '/admin/tiers',
    method: 'get'
  })
}

export function getTier(key) {
  return request({
    url: '/admin/tiers/' + key,
    method: 'get'
  })
}

export function updateTier(key, data) {
  return request({
    url: '/admin/tiers/' + key,
    method: 'put',
    data: data
  })
}

export function toggleTier(key) {
  return request({
    url: '/admin/tiers/' + key + '/toggle',
    method: 'post'
  })
}

// ========== 领取/重置 ==========
export function claimTier(key, player) {
  return request({
    url: '/admin/tiers/' + key + '/claim',
    method: 'post',
    data: { player: player }
  })
}

export function resetTier(key, player) {
  return request({
    url: '/admin/tiers/' + key + '/reset',
    method: 'post',
    data: { player: player }
  })
}

// ========== 领取记录 ==========
export function getTierRecords(key) {
  return request({
    url: '/admin/tiers/' + key + '/records',
    method: 'get'
  })
}

// ========== 管理员背包 ==========
export function getAdminInventory() {
  return request({
    url: '/admin/inventory',
    method: 'get'
  })
}

// ========== 在线玩家 ==========
export function getOnlinePlayers() {
  return request({
    url: '/admin/players',
    method: 'get'
  })
}

// ========== 设置 ==========
export function getSettings() {
  return request({
    url: '/admin/settings',
    method: 'get'
  })
}

// @deprecated 后端 PUT /admin/settings 已下线（双写冲突）。
// 配置写入请使用 @/api/erp 的 saveConfigFile('config', data)（ERP「插件配置」页）。
// 仅历史 views/monthlycard/settings 页（已被路由重定向、不可达）仍引用本函数。
export function saveSettings(data) {
  return request({
    url: '/admin/settings',
    method: 'put',
    data: data
  })
}

// ========== 日志 ==========
export function getLogs() {
  return request({
    url: '/admin/logs',
    method: 'get'
  })
}
