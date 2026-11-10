import request from '@/utils/request'

/**
 * 月卡 ERP 侧接口（MCERP 模块控制器，路由 /api/plugins/SOYSMonthlyCard/soysmonthlycard/*）。
 *
 * <p>与 {@link ./monthlycard} 的 {@code /admin/*} 用户中心接口是两套：这里是 ERP 模块专用，
 * 带 {@code mcerp:monthlycard:*} 权限校验，页面在 MCERP 控制台内嵌（wujie）时使用。</p>
 */

// ========== 领取记录 ==========
export function getClaimList(params) {
  return request({
    url: '/soysmonthlycard/claim/list',
    method: 'get',
    params: params
  })
}

export function getClaimTop(limit) {
  return request({
    url: '/soysmonthlycard/claim/top',
    method: 'get',
    params: { limit: limit || 10 }
  })
}

export function claimFor(tierKey, player) {
  return request({
    url: '/soysmonthlycard/claim/' + tierKey + '/claim',
    method: 'post',
    data: { player: player }
  })
}

export function resetFor(tierKey, player) {
  return request({
    url: '/soysmonthlycard/claim/' + tierKey + '/reset',
    method: 'post',
    data: { player: player }
  })
}

// ========== 档位 ==========
export function getErpTiers(pageSize) {
  return request({
    url: '/soysmonthlycard/tier/list',
    method: 'get',
    params: { pageNum: 1, pageSize: pageSize || 100 }
  })
}

export function getErpSummary() {
  return request({
    url: '/soysmonthlycard/tier/summary',
    method: 'get'
  })
}

export function getErpTier(key) {
  return request({
    url: '/soysmonthlycard/tier/' + key,
    method: 'get'
  })
}

export function saveErpTier(key, data) {
  return request({
    url: '/soysmonthlycard/tier/' + key,
    method: 'put',
    data: data
  })
}

export function toggleErpTier(key) {
  return request({
    url: '/soysmonthlycard/tier/' + key + '/toggle',
    method: 'post'
  })
}

export function getErpInventory() {
  return request({
    url: '/soysmonthlycard/tier/inventory',
    method: 'get'
  })
}

// ========== 玩家 ==========
export function getErpOnlinePlayers() {
  return request({
    url: '/soysmonthlycard/claim/players',
    method: 'get'
  })
}

// ========== 日志 ==========
export function getErpLogs(maxLines) {
  return request({
    url: '/soysmonthlycard/stat/logs',
    method: 'get',
    params: maxLines ? { maxLines: maxLines } : {}
  })
}
