<template>
  <config-layout title="TLS 强制" :help-map="helpMap" file="gateway/policies/tls.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never">
        <div slot="header">强制 HTTPS</div>
        <el-form label-width="160px" size="small">
          <el-form-item label="启用">
            <el-switch v-model="model.enabled" />
            <div class="hint">明文 HTTP → 426 Upgrade Required，Location 指向同端口 https。</div>
          </el-form-item>
          <el-form-item label="跳转主机名">
            <el-input v-model="model.host" placeholder="留空=用 gateway/https.yml 的 host" />
          </el-form-item>
        </el-form>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../../mixins/configPage'
import ConfigLayout from '../../components/ConfigLayout.vue'

export default {
  name: 'SettingsGwTls',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'gw-tls',
      helpMap: {
        '启用': 'TLS 强制策略。明文 HTTP → 426 Upgrade Required，Location 指向同端口 https。',
        '跳转主机名': '426 响应 Location 使用的主机名。留空=用 gateway/https.yml 的 host。'
      }
    }
  }
}
</script>

<style scoped>.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }</style>
