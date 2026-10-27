/**
 * permission store：路由生成与过滤。
 * <p>wujie 模式下跳过 GenerateRoutes（主应用统一鉴权），
 * 仅做本地静态路由映射；隐藏 MCERP 自带的系统管理目录。
 */
import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    },
  },
  actions: {
    // 生成路由
    GenerateRoutes({ commit }) {
      return new Promise(resolve => {
        // 向后端请求路由数据
        getRouters().then(res => {
          const sdata = JSON.parse(JSON.stringify(res.data))
          const rdata = JSON.parse(JSON.stringify(res.data))
          const sidebarRoutes = filterAsyncRouter(sdata)
          const rewriteRoutes = filterAsyncRouter(rdata, false, true)
          const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
          rewriteRoutes.push({ path: '*', redirect: '/404', hidden: true })
          router.addRoutes(asyncRoutes)
          router.addRoutes(rewriteRoutes)
          commit('SET_ROUTES', rewriteRoutes)
          commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(sidebarRoutes))
          commit('SET_DEFAULT_ROUTES', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
          resolve(rewriteRoutes)
        })
      })
    }
  }
}

// 遍历后台传来的路由字符串，转换为组件对象
// isNested: 是否在 Layout 内部嵌套（顶层路由用 Layout，嵌套 dir 用 ParentView，避免两层 Layout 重复渲染侧边栏）
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false, isNested = false) {
  return asyncRouterMap.filter(route => {
    // 隐藏"系统管理"目录（MCERP 自带），不出现在子应用侧边栏
    if (route.name === 'System' || (route.path && route.path.startsWith('/system'))) {
      route.hidden = true
    }
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理：嵌套路由里的 Layout 降级为 ParentView
      if (route.component === 'Layout') {
        route.component = isNested ? ParentView : Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type, true)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view) => {
  // wujie: 前缀：后端 menus() 的 component 是完整 URL（/web/plugins/&lt;id&gt;/erp/xxx），
  // 给 MCERP 主应用 wujie 容器加载用；子应用自己访问时需把它映射到本地 views 组件
  if (typeof view === 'string' && view.startsWith('wujie:')) {
    const url = view.slice(6)
    // 提取 /erp/... 之后的子路径：/web/plugins/SOYSHTTPOverMC-ERP/erp/settings/config → erp/settings/config
    const m = url.match(/\/soyshttpovermcerp\/(.+)$/)
    if (m) {
      let sub = m[1]
      view = 'soyshttpovermcerp/' + sub
    }
  }
  if (process.env.NODE_ENV === 'development') {
    return (resolve) => require([`@/views/${view}`], resolve)
  } else {
    // 使用 import 实现生产环境的路由懒加载
    return () => import(`@/views/${view}`)
  }
}

export default permission
