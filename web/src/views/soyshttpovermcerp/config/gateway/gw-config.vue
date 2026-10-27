<template>
  <config-layout title="网关总开关" :help-map="helpMap" file="gateway/config.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never">
        <el-form label-width="180px" size="small">
          <el-form-item label="启用安全网关">
            <el-switch v-model="model.enabled" />
            <div class="hint">总开关；关闭后所有策略（鉴权/限流/白名单/TLS）均不生效。</div>
          </el-form-item>
          <el-form-item label="API 全局前缀">
            <el-input v-model="model['api-prefix']" />
            <div class="hint">@GetMapping("/ping") 一律映射为 /api/ping；修改需重启服务器。</div>
          </el-form-item>
          <el-form-item label="事件调试">
            <el-switch v-model="model['debug-events']" />
            <div class="hint">在控制台打印网关事件（请求进入/拒绝/完成/凭证下发/API 注册）。</div>
          </el-form-item>
        </el-form>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../../mixins/configPage'
import ConfigLayout from '../../components/ConfigLayout.vue'

export default {
  name: 'SettingsGwConfig',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'gw-config',
      helpMap: {
        '启用安全网关': '网关总开关。关闭后所有策略（鉴权/限流/白名单/TLS）均不生效。',
        'API 全局前缀': '注解式 API 全局前缀，始终生效。@GetMapping("/ping") 一律映射为 /api/ping。修改需重启服务器。',
        '事件调试': 'true 时在控制台打印网关事件（请求进入/拒绝/完成/凭证下发/API注册），便于排查。'
      }
    }
  }
}
</script>

<style scoped>.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }</style>
