/**
 * SOYS 平台契约消费封装（@soys 契约 v5 · 严格消费）。
 *
 * 契约文件 public/__SOYS_CONTEXT__.js 由托管层（SOYS / MCERP ContractInjector）按 owner 插件注入后下发，
 * 子应用只是接收方：这里【只读取】window.SOYS_CONTEXT 的既有字段，
 * 不新增任何全局命名、不改写契约文件、不提供任何兜底值。
 * 契约缺失或字段残缺一律视为部署错误并直接抛错，避免静默落到错误的 API / 页面命名空间。
 *
 * 契约原语（SoysContextContractVO）：
 *   scheme / host / port / apiPrefix / pluginsPrefix / apiFullPrefix / pageFullPrefix
 *   / webResourcePrefix / pageBase / spaFallback / fpEnabled / fpStrict
 *
 * v5：
 *   - apiBaseURL 直接取 apiFullPrefix（移除 development 三元拆分 '/api' 兜底）；
 *   - pageBase 取契约 pageBase，history / hash 由契约 spaFallback 决定，不再写死 'history'；
 *   - 由模块标识派生 moduleId / moduleKey / moduleRoot，供路由根与页面内跳转使用，
 *     前端不再散落插件名字符串。
 *
 * 【本插件与模板的差异（必读）】
 * 模板按 pluginsPrefix 末段（owner 插件名）派生模块路由根。本插件不能这么做：
 *   owner 插件名 SOYSMonthlyCard → NameSafe 后 soysmonthlycard → /soysmonthlycard
 *   ERP 模块 identifier          → soysmonthlycard-erp → MCERP 实际下发 /soysmonthlycarderp
 * MCERP MenuRouteServiceImpl#moduleRoute 取的是 module.getId()（即扩张的 identifier），
 * 所以模块路由根是 /soysmonthlycarderp。两者不一致，必须以 MODULE_ID 显式覆盖，
 * 且该常量必须与 MonthlyCardErpExpansion#identifier() 保持一致（改一处必须同步另一处）。
 */
const ctx = typeof window !== 'undefined' ? window.SOYS_CONTEXT : null

if (!ctx) {
  throw new Error('[SOYS] 契约未注入：必须由托管层注入 __SOYS_CONTEXT__.js 后再加载本应用')
}

// 本应用运行所需的契约字段，缺一则视为契约版本不匹配 / 注入异常（不做静默兜底）
const REQUIRED_FIELDS = [
  'scheme', 'host', 'port', 'apiPrefix', 'pluginsPrefix', 'apiFullPrefix',
  'pageFullPrefix', 'pageBase', 'webResourcePrefix', 'spaFallback'
]
const missing = REQUIRED_FIELDS.filter(k => ctx[k] === undefined || ctx[k] === null || ctx[k] === '')
if (missing.length) {
  throw new Error('[SOYS] 契约字段缺失：' + missing.join(', '))
}

// owner 插件标识 = pluginsPrefix 的末段（/plugins/SOYSMonthlyCard → SOYSMonthlyCard）
const segments = ctx.pluginsPrefix.split('/').filter(Boolean)
const moduleId = segments[segments.length - 1]

// ERP 模块标识：与后端 MonthlyCardErpExpansion#identifier() 一一对应（见文件头说明）
const MODULE_ID = 'soysmonthlycard-erp'
// NameSafe.path：仅保留字母 / 数字 / 下划线并转小写
// （对齐 MCERP NameSafe.path() 与 MenuRouteServiceImpl.safeName() 的模块路由 path 规则）
const moduleKey = MODULE_ID.replace(/[^A-Za-z0-9_]/g, '').toLowerCase()
if (!moduleKey) {
  throw new Error('[SOYS] 模块标识无法派生模块路由名：' + MODULE_ID)
}
const moduleRoot = '/' + moduleKey

const apiBaseURL = ctx.apiFullPrefix
const pagePrefix = ctx.pageFullPrefix

const soysContext = {
  version: 5,
  server: ctx,                    // 服务器环境原语（只读）
  injected: true,
  apiPrefix: ctx.apiPrefix,
  pluginsPrefix: ctx.pluginsPrefix,
  pagePrefix,                     // 契约 pageFullPrefix
  pageBase: ctx.pageBase,         // vue-router base（附属插件 = pageFullPrefix + "/"）
  apiBaseURL,                     // axios baseURL / 上传 action / 头像回显统一使用
  moduleId,                       // owner 插件名（SOYSMonthlyCard）
  moduleKey,                      // ERP 模块 identifier 的 NameSafe + 小写（soysmonthlycarderp）
  moduleRoot,                     // 模块路由根（= MCERP MenuRouteServiceImpl 的 /<safeName>）
  loginUrl: '/login.html',
  capabilities: {
    spaFallback: ctx.spaFallback  // true → history；false → hash
  },

  /** 识别服务器提供的完整后端地址（基于注入原语运行时组装） */
  resolveServer() {
    const origin = ctx.scheme + '://' + ctx.host + (ctx.port && ctx.port !== 80 ? ':' + ctx.port : '')
    return {
      origin,
      scheme: ctx.scheme,
      host: ctx.host,
      port: ctx.port,
      backendBase: origin + apiBaseURL,
      loginUrl: origin + soysContext.loginUrl
    }
  }
}

// 暴露到全局便于浏览器控制台核对注入结果：window.SOYS_CONTEXT_API（既有调试通道，不新增命名）
if (typeof window !== 'undefined') {
  window.SOYS_CONTEXT_API = soysContext
}

export default soysContext
