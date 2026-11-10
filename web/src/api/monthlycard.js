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
