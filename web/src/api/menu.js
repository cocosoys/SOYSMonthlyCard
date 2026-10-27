import request from '@/utils/request'

/**
 * MCERP 宿主命名空间：getRouters 是 MCERP 主插件 ErpAuthController 端点
 * （菜单表 ⊕ 已登记 ERP 模块菜单合成）。附属插件自身命名空间下没有此端点。
 */
const MCERP_HOST_API = '/api/plugins/MCERP'

// 获取路由（走 MCERP 宿主命名空间，而非附属插件自身命名空间）
export const getRouters = () => {
  return request({
    url: '/auth/getRouters',
    method: 'get',
    baseURL: MCERP_HOST_API
  })
}