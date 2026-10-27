import request from '@/utils/request'

/**
 * SOYS 主插件自带 auth 基址：契约 apiPrefix（/api）+ /auth。
 * 登录/登出/会话完全交给主插件，不再走 MCERP 自建 auth（captchaImage/login/logout 已废弃）。
 */
function soysApi() {
  const ctx = (typeof window !== 'undefined' && window.SOYS_CONTEXT) || null
  return (ctx && ctx.apiPrefix) || '/api'
}

/**
 * MCERP 宿主命名空间：getInfo/getRouters 是 MCERP 主插件 ErpAuthController 端点
 * （/api/plugins/MCERP/auth/getInfo、/auth/getRouters）。
 * 附属插件自身命名空间（/api/plugins/<附属>）下没有这些端点，走默认 baseURL 会 401 → 误判未登录 → 清登录态。
 */
const MCERP_HOST_API = '/api/plugins/MCERP'

// 登录方法：直接调用主插件自带弹窗登录 POST /api/auth/login
// 成功返回 { token, player }（SOYS 契约：data.token / data.player）
export function login(username, password, code, uuid) {
  return window.SoysAuth.request(soysApi() + '/auth/login', {
    method: 'POST',
    json: true,
    body: JSON.stringify({ username, password, remember: true })
  }).then(r => {
    if (r.code === 200 && r.data && r.data.token) {
      return { token: r.data.token, player: r.data.player }
    }
    throw new Error(r.msg || '登录失败')
  })
}

// 注册方法（SOYS 无自建注册端点；保留签名避免 import 断裂）
export function register(data) {
  return Promise.reject(new Error('注册功能由主插件/游戏内处理'))
}

// 获取用户详细信息（若依契约，MCERP 主插件端点 /auth/getInfo → /api/plugins/MCERP/auth/getInfo）
export function getInfo() {
  return request({
    url: '/auth/getInfo',
    method: 'get',
    baseURL: MCERP_HOST_API
  })
}

// 解锁屏幕
export function unlockScreen(password) {
  return request({
    url: '/unlockscreen',
    method: 'post',
    data: { password }
  })
}

// 退出方法：直接调用主插件自带 POST /api/auth/logout
export function logout() {
  return window.SoysAuth.request(soysApi() + '/auth/logout', { method: 'POST' })
    .then(r => {
      if (r.code !== 200) throw new Error(r.msg || '退出失败')
    })
}

// 获取验证码（MCERP 已废弃自建验证码；保留导出避免 register.vue import 断裂）
export function getCodeImg() {
  return Promise.resolve({ captchaEnabled: false, img: '', uuid: '' })
}
