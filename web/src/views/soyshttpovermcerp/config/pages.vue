<template>
  <config-layout title="页面与资源" :help-map="helpMap" file="pages.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >

      <el-card shadow="never" class="mb">
        <div slot="header">Web 站点</div>
        <el-form label-width="200px" size="small">
          <el-form-item label="静态资源根目录">
            <el-input v-model="model.web.root" />
            <div class="hint">dist / 前端构建产物所在目录。</div>
          </el-form-item>
          <el-form-item label="首页路径">
            <el-input v-model="model.web.home" />
          </el-form-item>
          <el-form-item label="大文件阈值（字节）">
            <byte-converter v-model="model.web['large-file-threshold']" />
          </el-form-item>
          <el-form-item label="大文件上限（字节）">
            <byte-converter v-model="model.web['large-file-max-bytes']" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">缓存策略</div>
        <el-form label-width="200px" size="small">
          <el-form-item label="缓存总大小上限（字节）">
            <byte-converter v-model="model.web.cache['max-bytes']" />
          </el-form-item>
          <el-form-item label="缓存条目数上限">
            <el-input-number v-model="model.web.cache['max-entries']" :min="1" />
          </el-form-item>
          <el-form-item label="缓存 TTL（秒）">
            <el-input-number v-model="model.web.cache['ttl-seconds']" :min="0" />
          </el-form-item>
          <el-form-item label="固定缓存（pinned）">
            <div v-for="(p,i) in model.web.cache.pinned" :key="i" class="srow">
              <el-input :value="p" size="small" @change="v => $set(model.web.cache.pinned, i, v)" />
              <el-button type="text" size="mini" icon="el-icon-delete" @click="model.web.cache.pinned.splice(i,1)" />
            </div>
            <el-button size="mini" type="text" icon="el-icon-plus" @click="model.web.cache.pinned.push('')">添加 pinned 路径</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">已登记页面（pages.page）</div>
        <el-table :data="pageList" size="mini" border>
          <el-table-column label="路径" width="140">
            <template slot-scope="s"><el-input v-model="s.row._key" size="mini" /></template>
          </el-table-column>
          <el-table-column label="显示名 nicknames">
            <template slot-scope="s">
              <el-input :value="(s.row.nicknames||[]).join(', ')" size="mini"
                        @change="v => $set(s.row, 'nicknames', v.split(/[,，]/).map(x=>x.trim()).filter(Boolean))"
                        placeholder="逗号分隔" />
            </template>
          </el-table-column>
          <el-table-column label="描述" min-width="180">
            <template slot-scope="s"><el-input v-model="s.row.description" size="mini" /></template>
          </el-table-column>
          <el-table-column label="资源文件" width="180">
            <template slot-scope="s"><el-input v-model="s.row.resource" size="mini" /></template>
          </el-table-column>
          <el-table-column label="内联权限" min-width="160">
            <template slot-scope="s">
              <el-input :value="(s.row.permissions||[]).join(', ')" size="mini"
                        @change="v => $set(s.row, 'permissions', v.split(/[,，]/).map(x=>x.trim()).filter(Boolean))"
                        placeholder="权限节点，逗号分隔" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template slot-scope="s">
              <el-button type="text" size="mini" style="color:#f56c6c" @click="removePage(s.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="mini" type="text" icon="el-icon-plus" style="margin-top:8px" @click="addPage">添加页面</el-button>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">自动路由（pages.auto）</div>
        <div class="hint" style="margin-bottom:8px">键=URL 路径，值=来源；值可为文件 / 目录（递归注册其中所有 .html）/ 反引号包裹的网络跳转。</div>
        <div v-for="(v,k,i) in model.pages.auto" :key="i" class="kvrow">
          <el-input :value="k" size="small" @change="nv => renameKey(model.pages.auto, k, nv)" />
          <el-input :value="v" size="small" @change="nv => $set(model.pages.auto, k, nv)" placeholder="来源路径" />
          <el-button type="text" size="mini" icon="el-icon-delete" @click="deleteKey(model.pages.auto, k)" />
        </div>
        <el-button size="mini" type="text" icon="el-icon-plus" @click="addMapEntry(model.pages.auto)">添加路由</el-button>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">页面权限（pages.permissions）</div>
        <div class="hint" style="margin-bottom:8px">路径 → 权限节点数组（AND 语义，全部通过才放行）；未登录 302 登录页。</div>
        <div v-for="(v,k,i) in model.pages.permissions" :key="i" class="kvrow">
          <el-input :value="k" size="small" @change="nv => renameKey(model.pages.permissions, k, nv)" />
          <el-input :value="(v||[]).join(', ')" size="small"
                    @change="nv => $set(model.pages.permissions, k, nv.split(/[,，]/).map(x=>x.trim()).filter(Boolean))"
                    placeholder="权限节点，逗号分隔" />
          <el-button type="text" size="mini" icon="el-icon-delete" @click="deleteKey(model.pages.permissions, k)" />
        </div>
        <el-button size="mini" type="text" icon="el-icon-plus" @click="addPermEntry">添加权限映射</el-button>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">别名路由（pages.alias）</div>
        <div class="hint" style="margin-bottom:8px">别名路径 → 真实目标路径；给任意已存在链接（含第三方插件页面/API）新增内部映射入口。</div>
        <div v-for="(v,k,i) in model.pages.alias" :key="i" class="kvrow">
          <el-input :value="k" size="small" @change="nv => renameKey(model.pages.alias, k, nv)" placeholder="别名路径，如 /erp-admin" />
          <el-input :value="v" size="small" @change="nv => $set(model.pages.alias, k, nv)" placeholder="真实目标路径，如 /web/plugins/MCERP" />
          <el-button type="text" size="mini" icon="el-icon-delete" @click="deleteKey(model.pages.alias, k)" />
        </div>
        <el-button size="mini" type="text" icon="el-icon-plus" @click="addAliasEntry">添加别名</el-button>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../mixins/configPage'
import ConfigLayout from '../components/ConfigLayout.vue'
import ByteConverter from '../components/ByteConverter.vue'

export default {
  name: 'SettingsPages',
  mixins: [configPage],
  components: { ConfigLayout, ByteConverter },
  data() {
    return {
      fileId: 'pages',
      helpMap: {
        '静态资源根目录': '前端资源目录。留空（默认）= 插件首次运行会把 jar 内置的 /dist/* 解压到插件数据目录的 web/ 文件夹，并直接以该磁盘目录为前端根目录；此后可直接编辑磁盘文件（index.html 等），刷新浏览器即生效，无需重构建；已存在文件不会被覆盖（保留你的修改），仅补回缺失的内置默认资源，磁盘缺失的文件回退 jar 内置。指定路径 = 优先从该目录读取静态文件（相对数据目录或绝对路径）。',
        '首页路径': '访问根路径 / 时伺服的内容；留空=默认 web.root 下的 index.html → jar 内置 /dist/index.html 兜底。支持三种来源：①相对/逻辑路径（如 dist/index.html、status/index.html，按 web.root→jar /dist/ 顺序解析）；②绝对路径（如 C:/sites/home.html，直接伺服本地磁盘文件）；③网络 URL（如 https://example.com/home.html，按需拉取并缓存约 5 分钟，拉取失败自动回退默认首页）。修改后需重启生效。',
        '大文件阈值（字节）': '大文件判定阈值：超过该大小的磁盘文件视为大文件——不写入缓存，交由「大文件加载抽象」处理（默认内置流式分块加载器；默认阈值 = cache.max-bytes，即 16MB）。',
        '大文件上限（字节）': '大文件安全上限：超过直接拒绝加载（防单文件打爆内存，默认 128MB = 134217728 字节）。',
        '缓存总大小上限（字节）': 'LRU 缓存字节上限（默认 16MB = 16777216）。仅约束「非常驻」文件；常驻（pinned）不计入此配额。仅对「惰性来源」生效（磁盘文件 / jar 资源，请求时读取）；registerPage 直接给定的字节不受控。',
        '缓存条目数上限': 'LRU 缓存条目数上限（默认 1024）。超过后按 LRU 淘汰最久未访问的条目。',
        '缓存 TTL（秒）': '条目存活时间（秒）：超过后若无人再次访问即失效释放，下次访问重新加载。',
        '固定缓存（pinned）': '常驻内存清单：路径精确匹配或目录前缀（以 / 结尾）匹配。命中项直接进常驻内存，不参与淘汰、不占用 max-bytes（用于必须高频/快速响应的资源，如门户首页）。例：["/", "/assets/"]（"/" 精确匹配门户首页；"/assets/" 前缀匹配整个目录）。',
        '已登记页面（pages.page）': '显式页面（resource + 可选 nicknames / description / permissions）。resource 为单个文件/资源，【不支持目录】，按扩展名推断 Content-Type（.html 为 text/html）。来源：相对插件数据目录（web/notice.html）、绝对磁盘路径（D:/sites/my-html/notice.html）、jar 内置资源（/dist/xxx.html）。',
        '路径': '页面的 URL 路径，如 /user、/group、/apikey。访问昵称（nicknames 列）同样命中本页面。',
        '显示名 nicknames': '昵称路由（别名 URL 路径）：访问昵称同样命中本页面。可填多个，用逗号分隔（支持中英文逗号）。示例：主页, 玩家列表。',
        '描述': '界面说明：/soyshttp pages 展示时自动拼接“ —— ”+description。用于识别页面用途。',
        '资源文件': '该页面的 resource 来源：相对插件数据目录（如 web/notice.html）；绝对磁盘路径（如 D:/sites/my-html/notice.html）；jar 内置资源（如 /dist/xxx.html）。不支持目录。',
        '内联权限': '单页内联网页访问权限（字符串数组，AND 语义，最高优先）：数组内全部通过才放行，任一缺失 → 302；与 pages.permissions 同路径时完全替换、不合并。仅对 HTML 页/跳转生效（css/js/图片等资源不拦）。示例：soyshttp.page.admin, soyshttp.page.admin.manage。',
        '自动路由（pages.auto）': '平价注册：键 = URL 路径，值 = 来源。值可为：①单文件/资源（按扩展名推断类型）；②目录（递归注册该文件夹下【所有 .html】文件，按相对路径挂载；配合键 "/" 即“写 / 注册文件夹下所有 html”）；③值以反引号包裹 => 登记 302 网络跳转（如 "/path": "`https://外部目标/`"，也可跳转站内路径）。修改后请执行 /soyshttp reload 或重启生效。',
        '页面权限（pages.permissions）': '全局路径规则（精确 /admin、目录通配 /console/*、全量 *）。权限节点为字符串数组，判定为 AND 语义（数组内全部通过才放行，任一缺失 → 302）。路径匹配复用 API 网关同款语义；权限节点复用组合权限服务（含本地权限表 local）。未登录 → 302 登录页 login.html；已登录缺权限 → 302 权限不足提示页 /perm-denied.html。仅对 HTML 页/跳转生效。',
        '别名路由（pages.alias）': '运维别名路由（可选）：给任意已存在链接（含第三方插件页面/API）新增“内部映射”别名。键=别名路径、值=真实目标路径；支持多别名指向同一真实链接。语义：①别名命中后浏览器地址栏不变，按真实路径走完整解析（精确/.html 智能/参数化/昵称/index 兜底均生效）；②精确单路径、不做前缀映射（子资源仍走原路径，别名只做入口）；③继承请求的 HTTP method（配 /api/foo 后 GET/POST 都映射到同 method 的真实路径）；④仅跳转一层（真实路径不再查别名，防成环）。典型用途：缩短冗长路径 / 中文路径别名 / 第三方插件页面起别名。示例："/erp-admin": "/web/plugins/MCERP"。'
      }
    }
  },
  computed: {
    // pages.page 是 {路径: {nicknames,description,resource}} 对象，转成行数组便于表格编辑，保存时回写
    pageList() {
      const obj = (this.model.pages && this.model.pages.page) || {}
      return Object.keys(obj).map(k => ({ _key: k, ...obj[k] }))
    }
  },
  methods: {
    addPage() {
      if (!this.model.pages) this.$set(this.model, 'pages', {})
      if (!this.model.pages.page) this.$set(this.model.pages, 'page', {})
      this.$set(this.model.pages.page, '/new-' + Date.now(), { nicknames: [], description: '', resource: '' })
    },
    removePage(i) {
      const row = this.pageList[i]
      this.$delete(this.model.pages.page, row._key)
    },
    renameKey(obj, oldK, newK) {
      if (oldK === newK || !obj[oldK] && obj[newK]) return
      const v = obj[oldK]
      this.$delete(obj, oldK)
      this.$set(obj, newK, v)
    },
    deleteKey(obj, k) { this.$delete(obj, k) },
    addMapEntry(obj) { this.$set(obj, '/new-' + Date.now(), '') },
    addPermEntry() {
      if (!this.model.pages.permissions) this.$set(this.model.pages, 'permissions', {})
      this.$set(this.model.pages.permissions, '/new-' + Date.now(), [])
    },
    addAliasEntry() {
      if (!this.model.pages.alias) this.$set(this.model.pages, 'alias', {})
      this.$set(this.model.pages.alias, '/new-' + Date.now(), '')
    }
  }
}
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }
.srow { display: flex; align-items: center; margin-bottom: 6px; }
.srow .el-input { flex: 1; }
.kvrow { display: flex; gap: 8px; margin-bottom: 6px; }
.kvrow .el-input { flex: 1; }
</style>
