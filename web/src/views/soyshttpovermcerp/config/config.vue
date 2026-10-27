<template>
  <config-layout title="核心配置" file="config.yml" :help-map="helpMap" :dirty="dirty" :loading="loading" :saving="saving"
    @reload="reload" @save="save">

      <el-card shadow="never" class="mb" id="sec-upload">
        <div slot="header">数据贡献 (upload)</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用数据贡献">
            <el-switch v-model="model.upload.enabled" />
            <div class="hint">将服务器公网地址（IP:端口）匿名贡献给数据服务器，仅用于用量/地域统计。有顾虑请关闭。</div>
          </el-form-item>
          <el-form-item label="统计上报服务器">
            <el-input v-model="model.upload.server" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-channel">
        <div slot="header">插件消息通道</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="channel">
            <el-input v-model="model.channel" />
            <div class="hint">Bukkit 插件消息通道名，默认 httpproxy:main。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-mc">
        <div slot="header">MC 服务器地址与端口</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="host">
            <el-input v-model="model.mc.host" placeholder="留空=自动取 server.properties server-ip" />
          </el-form-item>
          <el-form-item label="port">
            <el-input-number v-model="model.mc.port" :min="0" />
            <div class="hint">0=自动取 server.properties server-port；必须等于游戏对外端口。</div>
          </el-form-item>
          <el-form-item label="public-host">
            <el-input v-model="model.mc['public-host']" placeholder="群组服对外公布的公网地址，留空=沿用 host" />
          </el-form-item>
          <el-form-item label="public-port">
            <el-input-number v-model="model.mc['public-port']" :min="0" />
          </el-form-item>
          <el-form-item label="信任前置代理">
            <el-switch v-model="model.mc['trust-proxy']" />
            <div class="hint">读取 X-Forwarded-For 恢复真实访客 IP（用于限流/白名单）。后端可被直连时建议关。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-proxy">
        <div slot="header">群组服（BungeeCord / Velocity）</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="server-name">
            <el-input v-model="model.proxy['server-name']" placeholder="本服在代理中的唯一名称，独立服留空" />
          </el-form-item>
          <el-form-item label="proxy-address">
            <el-input v-model="model.proxy['proxy-address']" placeholder="127.0.0.1:代理端口；留空=直连后端，跨服功能失效" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-sniffer">
        <div slot="header">同端口 HTTP 嗅探器</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用嗅探">
            <el-switch v-model="model.sniffer.enabled" />
            <div class="hint">访问端口=MC 端口，在 Spigot 监听上嗅探 HTTP，MC/HTTP/HTTPS 三协议共用端口。</div>
          </el-form-item>
          <el-form-item label="请求体上限（字节）">
            <byte-converter v-model="model.sniffer['max-body-bytes']" />
            <div class="hint">超过返回 413，默认 8MB。</div>
          </el-form-item>
          <el-form-item label="HTTP 并发上限">
            <el-input-number v-model="model.sniffer['http-concurrency']" :min="1" />
            <div class="hint">同时等待 HTTP 后端的请求数，满了新请求直接 503。日常 4~8。</div>
          </el-form-item>
          <el-form-item label="等待队列容量">
            <el-input-number v-model="model.sniffer['http-queue-size']" :min="0" />
          </el-form-item>
          <el-form-item label="keep-alive 空闲秒">
            <el-input-number v-model="model.sniffer['keep-alive-idle-seconds']" :min="0" />
            <div class="hint">长连接空闲 N 秒后关闭；调大可减少连接重建与证书重校验。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-backend">
        <div slot="header">HTTP 后端传输模式</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="传输模式">
            <el-select v-model="model['http-backend'].mode">
              <el-option label="direct 直接调用（延迟最低）" value="direct" />
              <el-option label="netty-eventloop（推荐，默认）" value="netty-eventloop" />
              <el-option label="memory-queue 内存队列（背压）" value="memory-queue" />
              <el-option label="standalone-server 独立端口服务器" value="standalone-server" />
            </el-select>
          </el-form-item>
          <el-form-item label="netty-eventloop 线程数">
            <el-input-number v-model="model['http-backend']['netty-eventloop'].threads" :min="1"
              :disabled="model['http-backend'].mode !== 'netty-eventloop'" />
          </el-form-item>
          <el-form-item label="memory-queue 队列容量">
            <el-input-number v-model="model['http-backend']['memory-queue'].capacity" :min="1"
              :disabled="model['http-backend'].mode !== 'memory-queue'" />
          </el-form-item>
          <el-form-item label="memory-queue worker 数">
            <el-input-number v-model="model['http-backend']['memory-queue'].workers" :min="1"
              :disabled="model['http-backend'].mode !== 'memory-queue'" />
          </el-form-item>
          <el-form-item label="standalone 监听地址">
            <el-input v-model="model['http-backend']['standalone-server'].host"
              :disabled="model['http-backend'].mode !== 'standalone-server'" />
          </el-form-item>
          <el-form-item label="standalone 监听端口">
            <el-input-number v-model="model['http-backend']['standalone-server'].port" :min="1"
              :disabled="model['http-backend'].mode !== 'standalone-server'" />
            <div class="hint">需与 MC 端口不同。仅 standalone-server 模式生效。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-log">
        <div slot="header">日志管控</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="日志级别">
            <el-select v-model="model.log.level" style="width:200px">
              <el-option v-for="l in ['OFF','ERROR','WARN','INFO','DEBUG','TRACE']" :key="l" :label="l" :value="l" />
            </el-select>
            <div class="hint">热重载生效，无需重启。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-perm">
        <div slot="header">权限判断组合</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="权限插件 providers">
            <el-tag v-for="(p,i) in model.permission.providers" :key="i" closable size="mini" style="margin-right:6px"
                     @close="model.permission.providers.splice(i,1)">{{ p }}</el-tag>
            <el-button size="mini" type="text" icon="el-icon-plus" @click="addProvider">添加 provider</el-button>
            <div class="hint">留空=自动加入所有已安装插件；可选 luckperms / permsex / essentials / essentialx / local。</div>
          </el-form-item>
          <el-form-item label="离线玩家降级">
            <el-select v-model="model.permission['offline-fallback']" style="width:200px">
              <el-option label="op-only 仅 OP（默认）" value="op-only" />
              <el-option label="local 查本地权限表" value="local" />
              <el-option label="false 全部拒绝（最严格）" value="false" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-storage">
        <div slot="header">数据存储</div>
        <el-form label-width="180px" size="small">
          <el-divider content-position="left">YAML 后端（零依赖）</el-divider>
          <el-form-item label="启用 YAML">
            <el-switch v-model="model.storage.backends.yaml.enabled" />
          </el-form-item>
          <el-form-item label="YAML 文件目录">
            <el-input v-model="model.storage.backends.yaml.file" />
          </el-form-item>
          <el-form-item label="保存时备份">
            <el-switch v-model="model.storage.backends.yaml['backup-on-save']" />
          </el-form-item>

          <el-divider content-position="left">SQLite 后端</el-divider>
          <el-form-item label="启用 SQLite">
            <el-switch v-model="model.storage.backends.sqlite.enabled" />
          </el-form-item>
          <el-form-item label="数据库文件">
            <el-input v-model="model.storage.backends.sqlite.file" />
          </el-form-item>

          <el-divider content-position="left">MySQL 后端</el-divider>
          <el-form-item label="启用 MySQL">
            <el-switch v-model="model.storage.backends.mysql.enabled" />
          </el-form-item>
          <el-form-item label="JDBC URL">
            <el-input v-model="model.storage.backends.mysql.url" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="model.storage.backends.mysql.username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="model.storage.backends.mysql.password" show-password />
          </el-form-item>

          <el-divider content-position="left">跨服同步</el-divider>
          <el-form-item label="开启跨服同步">
            <el-switch v-model="model.storage['cross-server']" />
            <div class="hint">需所有实例 MySQL 指向同一库；YAML 后端无法跨实例共享。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb" id="sec-auto">
        <div slot="header">自动运维</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="总开关">
            <el-switch v-model="model['auto'].ops.enabled" />
          </el-form-item>
          <el-form-item label="自动初始化">
            <el-switch v-model="model['auto'].ops.init" />
            <div class="hint">默认文件复制 + init.sql + 种子数据（表空才插入）。</div>
          </el-form-item>
          <el-form-item label="自动更新">
            <el-switch v-model="model['auto'].ops.update" />
          </el-form-item>
          <el-form-item label="失败策略">
            <el-select v-model="model['auto'].ops.fail" style="width:200px">
              <el-option label="disable 仅禁用失败插件（默认）" value="disable" />
              <el-option label="warn 跳过并告警" value="warn" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

  </config-layout>
</template>

<script>
import configPage from '../mixins/configPage'
import ConfigLayout from '../components/ConfigLayout.vue'
import ByteConverter from '../components/ByteConverter.vue'

export default {
  name: 'SettingsMain',
  mixins: [configPage],
  components: { ConfigLayout, ByteConverter },
  data() {
    return {
      fileId: 'config',
      helpMap: {
        '启用数据贡献': '是否同意将当前服务器的公网地址（IP:端口，如 127.0.0.1:25564）匿名贡献给 cocosoys 的数据服务器，仅用于让 cocosoys 进行数据统计（如插件用量、地域分布）。本质为向 upload.server 发送一个 POST 请求，请求体只携带 IP 与端口。我们承诺该地址仅用于统计，不会暴露详细数据给任何第三方或个人。若你存在顾虑请禁用它。',
        '统计上报服务器': '数据上报的 POST 请求地址，默认 https://api.cocosoys.com/report。仅当启用数据贡献为 true 时生效。',
        'channel': 'Bukkit 插件消息通道名，默认 httpproxy:main。用于插件间跨服通信（BungeeCord channel）。一般无需修改，除非与其他插件通道名冲突。',
        'host': 'MC 服务器监听地址。留空（默认）则自动取 server.properties 的 server-ip（为空再回退 127.0.0.1）。已显式填写的值优先。',
        'port': 'MC 服务器端口。必须等于 Spigot server.properties 的 server-port（即"服务器端口"）。留 0（默认）则自动取 server.properties 的 server-port（再回退运行期端口）。无需另配端口、无需挪窝。',
        'public-host': '群组服（BungeeCord/Waterfall/Velocity）下对外公布的「公网地址」覆盖。留空则沿用上面的 host。当后端端口需经代理对外、或后端绑定内网而客户端应连代理公网地址时，在此填客户端实际可达的 host。仅影响对外连接信息（/send 链接、状态页显示的地址），不改变 HTTP 后端连接本服的地址。',
        'public-port': '群组服下对外公布的公网端口覆盖。0=沿用上面的 port。同 public-host，仅影响对外显示。',
        '信任前置代理': '是否信任前置代理（SOYSHTTPOverMC 的 BungeeCord 代理模块）注入的 X-Forwarded-For，以恢复真实访客 IP（用于限流/白名单/审计）。仅当后端确在可信代理之后时保持 true。若后端可被客户端直连（未经代理），建议设 false 防伪造。',
        'server-name': '该子服在代理中的唯一名称（对应 BungeeCord config.yml servers.<name>）。仅在群组服下填写；独立服留空。用于：①跨服请求路由（/server/<server-name>/...）；②携带服务器标签经 discovery 广播。注意：群组服下每台子服的 server-name 必须唯一且与该服在代理里注册的名字一致。',
        'proxy-address': '经代理连接的地址（host:port，即 BungeeCord/Velocity 监听端口）。群组服下必须填写：只有"经代理连接"的身份，其 BungeeCord 频道 Forward 才会被代理跨服中继；留空则直连后端，Forward 被静默丢弃 → 跨服请求/发现全部失效。通常填 127.0.0.1:<代理端口>。',
        '启用嗅探': '核心功能：在 Spigot 自身监听端口上嗅探 HTTP 流量并就地转换。MC 流量原样放行，玩家与 curl 共用同一个端口。true=访问端口==服务器端口。1.12.2 用 Netty pipeline 注入（标准 netty，全功能）。',
        '请求体上限（字节）': 'HTTP 请求体大小上限（字节），超过返回 413。默认 8388608（8MB）。上传大文件时需调大。',
        'HTTP 并发上限': '同时阻塞在 HTTP 后端等待（future.get）的请求数上限（默认 4）。每个请求在经 HTTP 后端处理期间占用一个 worker；达到该值且队列满后，新请求直接 503 快速失败。日常负载可适当调大（如 4~8），需权衡单连接吞吐。',
        '等待队列容量': '等待中的请求队列容量（默认 8），满后新请求直接 503。',
        'keep-alive 空闲秒': 'HTTP keep-alive 空闲超时（秒，默认 30）：响应写完且未收到下一个请求时，空闲 N 秒后关闭连接，避免长连接泄漏。调大可减少连接断开重建频率：①减少 Chrome 对自签证书场景下新建连接的重校验；②提升长轮询/SSE 等长连接稳定性。代价是空闲连接占用更多 socket 资源。推荐 60~120。',
        '传输模式': '控制 HTTP 请求从网关层到业务处理层的传输方式。四种模式：direct=在 Netty IO 线程直接调用（延迟最低 <1ms，但高并发会阻塞 IO 线程）；netty-eventloop=提交到独立 Netty EventLoop（延迟 1~3ms，不阻塞网关 IO，推荐默认）；memory-queue=有界 ArrayBlockingQueue + worker 线程（支持背压，队列满返回 503，延迟 2~5ms）；standalone-server=独立端口启动 Netty HTTP 服务器（不占用 MC 端口，延迟最低 <1ms）。',
        'netty-eventloop 线程数': 'netty-eventloop 模式下处理请求的 EventLoop 线程数，默认 2。仅 mode=netty-eventloop 时可编辑。',
        'memory-queue 队列容量': 'memory-queue 模式下任务队列容量（默认 1024），满则返回 503。仅 mode=memory-queue 时可编辑。',
        'memory-queue worker 数': 'memory-queue 模式下 worker 线程数（默认 4）。仅 mode=memory-queue 时可编辑。',
        'standalone 监听地址': 'standalone-server 模式监听地址。0.0.0.0=所有网卡，127.0.0.1=仅本地。仅 mode=standalone-server 时可编辑。',
        'standalone 监听端口': 'standalone-server 模式监听端口（默认 25565），需与 MC 端口不同。仅 mode=standalone-server 时可编辑。',
        '日志级别': '统一日志门面 LogKit 的打印级别（全插件日志经此过滤）。OFF=关闭所有输出；ERROR=仅严重错误；WARN=错误+警告；INFO=以上+常规运行信息（默认）；DEBUG=以上+调试明细（策略明细、请求跟踪等）；TRACE=以上+最细粒度追踪。修改后 /soyshttp reload 热重载，无需重启服务器。',
        '权限插件 providers': '指定哪些权限插件加入判断组合（"或"逻辑：任一返回 true 则权限认证通过）。留空（默认 []）=所有已安装且支持的权限插件自动加入。指定列表=只使用列表中的插件。可选：luckperms（推荐，支持离线查询）、permsex（老牌，支持离线）、essentials/essentialx（仅在线）、local（插件内置本地权限表，在线/离线均可查）。',
        '离线玩家降级': '离线玩家权限判断降级策略（当没有权限插件支持离线查询时使用）。op-only（默认）=仅 OP 玩家返回 true（从 ops.json 读取）；local=查插件内置本地权限表（data/soys_perm_*.yml 或 SQL 表）；false=所有离线玩家返回 false（最严格）。',
        '启用 YAML': 'YAML 后端（默认启用，零依赖）。实体数据经 ORM 写入 data/ 目录下各 .yml 文件。适合小数据量、单机场景。',
        'YAML 文件目录': '存放各类 yml 表的文件夹（不是单个文件）。ORM 各实体写入 <file>/<表名>.yml。默认 data/。',
        '保存时备份': '保存时是否自动备份旧文件。',
        '启用 SQLite': 'SQLite 后端（默认禁用）。1.12.2 服务端自带驱动直接可用。适合中等数据量。',
        '数据库文件': 'SQLite 数据库文件路径，默认 data/records.db。',
        '启用 MySQL': 'MySQL 后端（默认禁用；与 SQLite 可同时启用）。priority 最高者为主存储（MYSQL 30 > SQLITE 20 > YAML 10）。适合大数据量、跨服共享场景。',
        'JDBC URL': 'MySQL 连接 URL，含数据库名、编码、时区等参数。示例：jdbc:mysql://localhost:3306/minecraft?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai',
        '用户名': 'MySQL 数据库用户名。',
        '密码': 'MySQL 数据库密码。',
        '开启跨服同步': '开启后多个服务端实例共享同一份跨服数据（令牌黑名单/审计/心跳/全局密钥）。前提：所有实例的 MySQL 后端必须启用且指向同一数据库。YAML 后端为单机文件，无法跨实例共享。跨服开启但 SQL 未启用时启动会告警。',
        '总开关': '自动运维总开关（默认 true）。关闭后所有自动数据操作（默认文件复制、init.sql 执行、种子数据）均跳过，仅可经显式命令触发。',
        '自动初始化': '自动初始化（默认 true）：默认数据文件复制（已存在不覆盖）+ init.sql（幂等约定，仅 MySQL 方言）执行 + 种子数据（表空才插入）。',
        '自动更新': '自动更新（默认 true）。',
        '失败策略': '初始化/更新失败策略：disable（默认）=仅禁用失败的对应插件（主插件自身失败→禁用 SOYS；附属插件数据失败→仅禁用该附属，主插件及其它插件继续运行，不会连坐）；warn=跳过并告警（继续运行，相关功能可能缺失）。'
      }
    }
  },
  methods: {
    addProvider() {
      this.$prompt('provider 名', '添加权限插件', { inputValue: 'luckperms' }).then(({ value }) => {
        this.model.permission.providers.push(value)
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }
</style>
