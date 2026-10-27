<template>
  <config-layout title="认证鉴权" :help-map="helpMap" file="gateway/policies/auth.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never" class="mb">
        <div slot="header">基础</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用鉴权">
            <el-switch v-model="model.enabled" />
          </el-form-item>
          <el-form-item label="API Key 请求头">
            <el-input v-model="model.header" />
          </el-form-item>
          <el-form-item label="登录提供者">
            <el-input v-model="model['login-provider']" placeholder="留空=自动选第一个可用登录插件；无登录插件=免密模式" />
          </el-form-item>
          <el-form-item label="保护路径">
            <el-input :value="(model.paths||[]).join(', ')" size="small"
                      @change="v => $set(model, 'paths', v.split(/[,，]/).map(x=>x.trim()).filter(Boolean))"
                      placeholder="逗号分隔；空=全部；/api/* 前缀" />
          </el-form-item>
          <el-form-item label="豁免路径">
            <el-input :value="(model.exempt||[]).join(', ')" size="small"
                      @change="v => $set(model, 'exempt', v.split(/[,，]/).map(x=>x.trim()).filter(Boolean))" />
            <div class="hint">公开端点，命中跳过鉴权。如 /ping、/auth/login、/homepage/*。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">接受的凭证来源</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="X-API-Key 头"><el-switch v-model="model.accept.header" /></el-form-item>
          <el-form-item label="Authorization: Bearer"><el-switch v-model="model.accept.bearer" /></el-form-item>
          <el-form-item label="Authorization: Basic"><el-switch v-model="model.accept.basic" /></el-form-item>
          <el-form-item label="Cookie（颁发器校验）"><el-switch v-model="model.accept.cookie" /></el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">自动登录</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="记住我总开关">
            <el-switch v-model="model.auto.login.ttl.enable" />
            <div class="hint">登录时勾选"记住我"则签发长期设备 Cookie，自动登录。</div>
          </el-form-item>
          <el-form-item label="记住我有效期（天）">
            <el-input-number v-model="model.auto.login.ttl.activetime" :min="1" />
          </el-form-item>
          <el-form-item label="IP 匹配自动登录">
            <el-switch v-model="model.auto.login.ip.enabled" />
            <div class="hint">默认关闭：不再按“游戏在线 + 网页 IP==游戏 IP”自动登录（IP 无法精准到个人设备，同 NAT 下会误伤他人）；true=保留旧行为，仅局域网/单机建议开。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">X-API-Key 降级</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="local 不可用时放行全部">
            <el-switch v-model="model['api-key']['local-fallback-all']" />
            <div class="hint">false=安全优先（403）；true=fail-open 退化为全权限，仅信任内网使用。</div>
          </el-form-item>
        </el-form>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../../mixins/configPage'
import ConfigLayout from '../../components/ConfigLayout.vue'

export default {
  name: 'SettingsGwAuth',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'gw-auth',
      helpMap: {
        '启用鉴权': '统一凭证鉴权总开关。关闭后所有 API 端点不校验凭证。',
        'API Key 请求头': 'X-API-Key 头名称，默认 X-API-Key。',
        '登录提供者': '网页登录使用的登录插件提供者名称（如 authme）。留空=自动选第一个可用登录插件；无登录插件=免密模式。',
        '保护路径': '需要鉴权保护的路径列表。空=保护所有路径；* 全匹配；/api/* 前缀匹配。',
        '豁免路径': '豁免鉴权的公开端点列表。命中则跳过鉴权直接放行。如 /ping、/auth/login、/homepage/*。',
        'X-API-Key 头': '是否接受 X-API-Key 请求头作为凭证。',
        'Authorization: Bearer': '是否接受 Authorization: Bearer <key> 作为凭证。',
        'Authorization: Basic': '是否接受 Authorization: Basic（用户名=key）作为凭证。',
        'Cookie（颁发器校验）': '是否接受 Cookie（由启用的颁发器如 session-token 校验）。',
        '记住我总开关': '记住我（设备免登录）总开关。登录时勾选则签发长期设备 Cookie（soys_remember），后续自动登录。',
        '记住我有效期（天）': '记住我凭证有效期（天），默认 7。',
        'IP 匹配自动登录': '旧“IP 匹配免登录”开关（gateway/policies/auth.yml → auto.login.ip.enabled，默认 false）：false=关闭（默认）——不再按“游戏在线 + 网页 IP==游戏 IP”自动登录，因为 IP 无法精准到个人设备，同 NAT 下会误伤他人；true=保留旧行为，仅建议局域网/单机环境开启。主插件自动登录已升级为多维体系：记住我（auto.login.ttl，登录勾选“记住我”后签发长期设备凭证 Cookie soys_remember，后续访问 /api/auth/status 自动登录）；设备指纹双因子（auto.login.fp，自动登录时校验请求携带的 X-Device-Fingerprint 与设备绑定表 soys_device_binding 一致，防“记住我 Cookie 被搬到其他设备”；strict=true 时指纹不一致直接拒绝并引导“重新登录 / 游戏内票据绑定”）；游戏端票据绑定（auto.login.ticket，玩家游戏内登录后聊天栏出现一次性绑定链接，浏览器打开提交“票据+设备指纹”完成设备绑定并签发记住我凭证）。相比 IP 匹配，ttl + fp + ticket 组合更精准、不误伤同 NAT 用户，推荐优先使用。',
        'local 不可用时放行全部': 'false=安全优先（local 不可用时 403）；true=fail-open 退化为全权限，仅信任内网使用。'
      }
    }
  }
}
</script>

<style scoped>.mb{margin-bottom:14px}.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }</style>
