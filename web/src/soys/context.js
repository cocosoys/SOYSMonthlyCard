/**
 * SOYS 平台契约消费封装（@soys 契约 v4）
 *
 * 契约由 SOYS ContractInjector 自动注入：public/__SOYS_CONTEXT__.js 声明占位符
 * __SOYS_CONTEXT__，伺服 dist 时被替换为服务器环境原语 JSON，前端无需关心服务器地址——
 * 换服务器、改 api-prefix（config.yml）均自动适配，杜绝"写死 host:port 换服务器失效"。
 *
 * v4：apiBaseURL 改用契约 apiFullPrefix（/api/plugins/MCERP）——MCERP 后端已从
 * registerProxyController 改为 registerController 正常登记，控制器类级前缀（/auth、/system/*）
 * 直接挂在 apiFullPrefix 下；auth 相关请求路径（login/getInfo/logout/captchaImage）同步加 /auth 前缀。
 *
 * 服务器环境原语（注入后可用；SOYS v1.4.1+ 契约字段）：
 *   scheme / host / port / apiPrefix / pluginsPrefix / apiFullPrefix / pageFullPrefix / webResourcePrefix
 *   （旧字段 fullPrefix / pagePrefix 已由 apiFullPrefix / pageFullPrefix 取代）
 */
const ctx = (typeof window !== 'undefined' && window.SOYS_CONTEXT) || null

// 应用层 API 前缀（历史契约段；MCERP 控制器已改为纯业务前缀 /auth、/system/* 等，不再拼接此段）
const appApi = '/prod-api'

// 页面命名空间前缀：优先读契约 pageFullPrefix（非主插件 = /web/plugins/MCERP；主插件 = ""）；
// 未注入（开发模式）回退空串——页面资源一律相对路径，此值仅用于显式拼页面前缀的场景
const pagePrefix = (ctx && ctx.pageFullPrefix) || ''

// 完整 API 基址：生产 = 契约 apiFullPrefix（/api/plugins/MCERP，registerController 正常登记，
// 控制器类级前缀 /auth、/system/* 等直接挂在下方，与后端路由一一对应）；
// 开发 = vue-cli 代理前缀（.env.development VUE_APP_BASE_API=/dev-api），不依赖契约
const apiBaseURL = process.env.NODE_ENV === 'development'
  ? process.env.VUE_APP_BASE_API
  : (ctx && ctx.apiFullPrefix) || '/api'

const soysContext = {
  version: 4,
  server: ctx,               // 服务器环境原语（null = 未注入/开发模式）
  injected: !!ctx,
  appApi,
  pagePrefix,                // 页面命名空间（契约 pageFullPrefix；空 = 主插件/未注入）
  apiBaseURL,                // 完整 API 基址（axios baseURL / upload action / 头像回显统一使用）
  loginUrl: '/login.html',
  capabilities: {
    spaFallback: false       // SOYS 未实现 history 回退 → 前端必须使用 hash 模式
  },

  /** 识别服务器提供的完整后端地址（基于注入原语运行时组装） */
  resolveServer() {
    if (!ctx) return null
    const origin = ctx.scheme + '://' + ctx.host + (ctx.port && ctx.port !== 80 ? ':' + ctx.port : '')
    return {
      origin,
      scheme: ctx.scheme,
      host: ctx.host,
      port: ctx.port,
      backendBase: origin + apiBaseURL,   // http://host:port/api/prod-api
      loginUrl: origin + soysContext.loginUrl
    }
  }
}

// 暴露到全局便于浏览器控制台核对注入结果：window.SOYS_CONTEXT_API
if (typeof window !== 'undefined') {
  window.SOYS_CONTEXT_API = soysContext
}

export default soysContext
