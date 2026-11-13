<template>
  <config-layout ref="layout" title="核心配置" file="config.yml" :help-map="helpMap" :dirty="dirty"
                 :loading="loading" :saving="saving" @reload="reload" @save="save">

    <!-- ready 守卫：异步 load 完成且对象链补齐前不渲染深链表单，避免 TypeError 整页白屏。
         Bukkit saveDefaultConfig() 只在文件不存在时写入，旧版 live 配置可能缺
         settings.audit / storage.backends.mysql 等节，必须靠 ensureSkeleton 兜底。 -->
    <div v-if="ready">
      <!-- ============ 领取设置 (settings) ============ -->
      <el-card shadow="never" class="mb">
        <div slot="header">领取设置 (settings)</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="每月领取日">
            <el-input-number v-model="model.settings['claim-day']" :min="1" :max="28" />
            <div class="hint">每月几号（含）之后可领取月卡礼包，建议 1-28。</div>
          </el-form-item>
          <el-form-item label="上线自动领取">
            <el-switch v-model="model.settings['auto-claim-on-join']" />
            <div class="hint">满足条件时玩家上线自动发放；关闭则需手动 /monthlycard 领取。</div>
          </el-form-item>
          <el-form-item label="离线补发">
            <el-switch v-model="model.settings['offline-compensation']" />
            <div class="hint">可领期间离线，上线时自动补发并使用「离线补发」文案提示。</div>
          </el-form-item>
          <el-form-item label="月份格式">
            <el-input v-model="model.settings['month-format']" style="width:220px" />
            <div class="hint">Java DateTimeFormatter 模式，如 yyyy-MM；用于领取记录与展示。</div>
          </el-form-item>
          <el-form-item label="语言代码">
            <el-input v-model="model.settings.language" style="width:160px" />
            <div class="hint">对应 language/&lt;代码&gt;.yml（内置 zh_cn / en_us）。</div>
          </el-form-item>
          <el-form-item label="金币单位">
            <el-input v-model="model.settings.units.money" style="width:160px" />
          </el-form-item>
          <el-form-item label="点券单位">
            <el-input v-model="model.settings.units.points" style="width:160px" />
            <div class="hint">用于提示语与 PAPI 占位符 %money_unit% / %points_unit%。</div>
          </el-form-item>
          <el-form-item label="调试模式">
            <el-switch v-model="model.settings.debug" />
            <div class="hint">输出存储读写细节，生产环境建议关闭。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ============ 领取审计 (settings.audit) ============ -->
      <el-card shadow="never" class="mb">
        <div slot="header">领取审计 (settings.audit)</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用审计日志">
            <el-switch v-model="model.settings.audit.enabled" />
            <div class="hint">记录谁在何时领取 / 重置 / 代领哪一档位。</div>
          </el-form-item>
          <el-form-item label="日志文件路径">
            <el-input v-model="model.settings.audit.file" style="width:280px" />
            <div class="hint">相对插件数据目录，对应「统计与日志」页读取的文件。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ============ 存储后端 (storage.backends) ============ -->
      <el-card shadow="never" class="mb">
        <div slot="header">存储后端 (storage.backends)</div>
        <el-form label-width="180px" size="small">
          <el-divider content-position="left">YAML 后端（默认启用，零依赖）</el-divider>
          <el-form-item label="启用 YAML">
            <el-switch v-model="model.storage.backends.yaml.enabled" />
          </el-form-item>
          <el-form-item label="数据文件">
            <el-input v-model="model.storage.backends.yaml.file" style="width:280px" />
          </el-form-item>
          <el-form-item label="保存时备份">
            <el-switch v-model="model.storage.backends.yaml['backup-on-save']" />
            <div class="hint">每次保存前备份为 .bak。</div>
          </el-form-item>

          <el-divider content-position="left">SQLite 后端</el-divider>
          <el-form-item label="启用 SQLite">
            <el-switch v-model="model.storage.backends.sqlite.enabled" />
          </el-form-item>
          <el-form-item label="数据库文件">
            <el-input v-model="model.storage.backends.sqlite.file" style="width:280px" />
          </el-form-item>
          <el-form-item label="表前缀">
            <el-input v-model="model.storage.backends.sqlite['table-prefix']" style="width:200px" />
          </el-form-item>

          <el-divider content-position="left">MySQL 后端（优先级最高）</el-divider>
          <el-form-item label="启用 MySQL">
            <el-switch v-model="model.storage.backends.mysql.enabled" />
            <div class="hint">启用后自动成为主存储，可跨服共享领取数据。</div>
          </el-form-item>
          <el-form-item label="JDBC URL">
            <el-input v-model="model.storage.backends.mysql.url" />
            <div class="hint">完整连接串，需含编码与时区参数。</div>
          </el-form-item>
          <el-form-item label="用户名 / 密码">
            <el-input v-model="model.storage.backends.mysql.username" style="width:160px" />
            <el-input v-model="model.storage.backends.mysql.password" show-password style="width:160px" class="ml" />
          </el-form-item>
          <el-form-item label="表前缀">
            <el-input v-model="model.storage.backends.mysql['table-prefix']" style="width:200px" />
          </el-form-item>
          <el-form-item label="连接保活间隔（秒）">
            <el-input-number v-model="model.storage.backends.mysql['keepalive-interval']" :min="0" />
            <div class="hint">0 关闭。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ============ 镜像策略 (storage.mirror) ============ -->
      <el-card shadow="never" class="mb">
        <div slot="header">镜像策略 (storage.mirror)</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用写入镜像">
            <el-switch v-model="model.storage.mirror.enabled" />
            <div class="hint">主存储写成功后同步到其余启用的辅助存储（热备份 / 降级）。</div>
          </el-form-item>
          <el-form-item label="异步镜像">
            <el-switch v-model="model.storage.mirror.async" />
            <div class="hint">镜像写入异步执行，避免阻塞主线程（推荐开启）。</div>
          </el-form-item>
          <el-form-item label="启动时全量同步">
            <el-switch v-model="model.storage.mirror['sync-on-startup']" />
            <div class="hint">启动时把主存储全量同步到辅助存储。</div>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
    <el-empty v-else description="配置加载中…" />

  </config-layout>
</template>

<script>
import configPage from '@/views/soyshttpovermcerp/mixins/configPage'
import ConfigLayout from '@/views/soyshttpovermcerp/components/ConfigLayout.vue'

/**
 * 「插件配置」菜单 —— 数据驱动的 config.yml 可视化编辑器。
 *
 * 复用工程既有设施：configPage mixin（load/save/dirty/reload，端点 /erp/config/*）、
 * ConfigLayout（左电梯导航 + 中表单 + 右帮助面板）。本页只负责
 * ① 声明 fileId='config'（对应后端 MonthlyCardConfigFile.CONFIG）
 * ② 按本插件 config.yml 的真实节组织卡片与 helpMap
 * ③ ready 守卫 + ensureSkeleton 防缺节白屏。
 */
export default {
  name: 'MonthlyCardConfigEdit',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      ready: false,
      fileId: 'config',
      helpMap: {
        '每月领取日': 'settings.claim-day：每月几号（含）之后可领取月卡礼包，建议 1-28，避免 29/30/31 在短月份不触发。',
        '上线自动领取': 'settings.auto-claim-on-join：玩家上线时若满足「有权限 + 本月未领 + 已过领取日」则自动发放。',
        '离线补发': 'settings.offline-compensation：可领期间离线的玩家，上线时自动补发。',
        '月份格式': 'settings.month-format：Java DateTimeFormatter 模式，决定领取记录按月去重的键，修改会导致历史月份口径变化。',
        '语言代码': 'settings.language：对应 language/<代码>.yml 语言包，内置 zh_cn / en_us；修改后需 /monthlycard reload。',
        '金币单位': 'settings.units.money：提示语与 %%money_unit%% 占位符的显示文案。',
        '点券单位': 'settings.units.points：提示语与 %%points_unit%% 占位符的显示文案。',
        '调试模式': 'settings.debug：输出存储读写细节到控制台，生产环境建议 false。',
        '启用审计日志': 'settings.audit.enabled：记录领取 / 重置 / 代领操作，供「统计与日志」页查看。',
        '日志文件路径': 'settings.audit.file：相对插件数据目录的日志文件路径，默认 logs/claims.log。',
        '启用 YAML': 'storage.backends.yaml.enabled：YAML 文件存储（优先级 10），零依赖，默认启用。',
        '启用 SQLite': 'storage.backends.sqlite.enabled：SQLite 单文件存储（优先级 20），需服务端提供 org.sqlite.JDBC 驱动。',
        '启用 MySQL': 'storage.backends.mysql.enabled：MySQL 存储（优先级 30），启用后自动成为主存储，可跨服共享数据。',
        '数据库文件': 'storage.backends.sqlite.file 或 yaml.file：相对插件数据目录的文件路径。',
        '数据文件': 'storage.backends.yaml.file：领取记录 YAML 文件，默认 data/claims.yml。',
        'JDBC URL': 'storage.backends.mysql.url：完整 JDBC 连接串，含编码与时区参数。',
        '用户名 / 密码': 'storage.backends.mysql.username / password：数据库凭据，以明文写回 config.yml，请限制本菜单权限。',
        '表前缀': '同一数据库多插件共用时区分表名。',
        '保存时备份': 'storage.backends.yaml.backup-on-save：每次保存前生成 .bak 备份。',
        '连接保活间隔（秒）': 'storage.backends.mysql.keepalive-interval：连接保活探测间隔，0 关闭。',
        '启用写入镜像': 'storage.mirror.enabled：主存储写成功后同步到辅助存储，作为热备份 / 降级方案。',
        '异步镜像': 'storage.mirror.async：镜像写入异步执行，避免阻塞主线程。',
        '启动时全量同步': 'storage.mirror.sync-on-startup：启动时把主存储全量同步到辅助存储。'
      }
    }
  },
  watch: {
    // mixin 异步 load 完成后整体替换 model；补齐缺失的嵌套对象后再放行渲染
    model(d) {
      if (d && typeof d === 'object') {
        this.ensureSkeleton(d)
        // 结构兜底本身会改动 model，需同步 original，否则一进页面就误报「未保存」
        this.original = JSON.stringify(this.model)
        this.ready = true
        // ConfigLayout 只在 mounted + 500ms 扫描 .el-card；卡片在 v-if="ready" 内，
        // 异步慢于 500ms 时电梯导航会空白，故渲染后主动重建一次锚点
        this.$nextTick(() => {
          const layout = this.$refs.layout
          if (layout && typeof layout.buildAnchors === 'function') {
            layout.buildAnchors()
          }
        })
      }
    }
  },
  methods: {
    // 只保证模板深链访问到的「对象链」存在，不臆造标量值：
    // 缺节也能正常显示，用户交互后才落值，避免擅自写入。
    ensureSkeleton(d) {
      const ensure = (parent, key) => {
        if (!parent[key] || typeof parent[key] !== 'object') parent[key] = {}
        return parent[key]
      }
      const settings = ensure(d, 'settings')
      ensure(settings, 'units')
      ensure(settings, 'audit')
      const storage = ensure(d, 'storage')
      const backends = ensure(storage, 'backends')
      ensure(backends, 'yaml')
      ensure(backends, 'sqlite')
      ensure(backends, 'mysql')
      ensure(storage, 'mirror')
    }
  }
}
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }
.ml { margin-left: 10px; }
</style>
