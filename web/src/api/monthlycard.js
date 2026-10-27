import request from '@/utils/request'

// ========== 概览 ==========
export function getOverview() {
  return request({
    url: '/monthlycard/admin/overview',
    method: 'get'
  })
}

// ========== 档位管理 ==========
export function getTiers() {
  return request({
    url: '/monthlycard/admin/tiers',
    method: 'get'
  })
}

export function getTier(key) {
  return request({
    url: '/monthlycard/admin/tiers/' + key,
    method: 'get'
  })
}

export function updateTier(key, data) {
  return request({
    url: '/monthlycard/admin/tiers/' + key,
    method: 'put',
    data: data
  })
}

export function toggleTier(key) {
  return request({
    url: '/monthlycard/admin/tiers/' + key + '/toggle',
    method: 'post'
  })
}

// ========== 领取/重置 ==========
export function claimTier(key, player) {
  return request({
    url: '/monthlycard/admin/tiers/' + key + '/claim',
    method: 'post',
    data: { player: player }
  })
}

export function resetTier(key, player) {
  return request({
    url: '/monthlycard/admin/tiers/' + key + '/reset',
    method: 'post',
    data: { player: player }
  })
}

// ========== 领取记录 ==========
export function getTierRecords(key) {
  return request({
    url: '/monthlycard/admin/tiers/' + key + '/records',
    method: 'get'
  })
}

// ========== 管理员背包 ==========
export function getAdminInventory() {
  return request({
    url: '/monthlycard/admin/inventory',
    method: 'get'
  })
}

// ========== 在线玩家 ==========
export function getOnlinePlayers() {
  return request({
    url: '/monthlycard/admin/players',
    method: 'get'
  })
}

// ========== 设置 ==========
export function getSettings() {
  return request({
    url: '/monthlycard/admin/settings',
    method: 'get'
  })
}

export function saveSettings(data) {
  return request({
    url: '/monthlycard/admin/settings',
    method: 'put',
    data: data
  })
}

// ========== 日志 ==========
export function getLogs() {
  return request({
    url: '/monthlycard/admin/logs',
    method: 'get'
  })
}
