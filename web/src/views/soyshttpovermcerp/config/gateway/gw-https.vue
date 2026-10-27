<template>
  <config-layout title="HTTPS 设置" :help-map="helpMap" file="gateway/https.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never">
        <div slot="header">基础</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用 HTTPS">
            <el-switch v-model="model.enabled" />
            <div class="hint">嗅探到 TLS 首包后动态挂 SslHandler，MC / 明文 HTTP / HTTPS 三协议共存于同端口。</div>
          </el-form-item>
          <el-form-item label="主机名">
            <el-input v-model="model.host" placeholder="自签 CN/SAN 与 426 Location 使用，留空=127.0.0.1" />
          </el-form-item>
        </el-form>
      </el-card>
      <el-card shadow="never" style="margin-top:14px">
        <div slot="header">证书（优先级：keystore &gt; cert+key &gt; 自动自签）</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="PEM 证书路径">
            <el-input v-model="model.cert" placeholder="可选" />
          </el-form-item>
          <el-form-item label="PEM 私钥路径">
            <el-input v-model="model.key" placeholder="可选，须 PKCS8" />
          </el-form-item>
          <el-form-item label="PKCS12 keystore">
            <el-input v-model="model.keystore" placeholder="可选，优先于 cert/key" />
          </el-form-item>
          <el-form-item label="keystore 密码">
            <el-input v-model="model['keystore-pass']" show-password />
          </el-form-item>
          <el-form-item label="私钥密码">
            <el-input v-model="model['key-pass']" show-password />
          </el-form-item>
        </el-form>
      </el-card>
      <el-card shadow="never" style="margin-top:14px">
        <div slot="header">TLS 协议</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用协议版本">
            <el-checkbox-group v-model="model['enabled-protocols']">
              <el-checkbox label="TLSv1.3" />
              <el-checkbox label="TLSv1.2" />
              <el-checkbox label="TLSv1.1" />
              <el-checkbox label="TLSv1" />
            </el-checkbox-group>
            <div class="hint">留空=全开（JVM 不支持的自动跳过）。</div>
          </el-form-item>
          <el-form-item label="最低 TLS">
            <el-select v-model="model['min-tls']" style="width:200px">
              <el-option v-for="l in ['TLSv1','TLSv1.1','TLSv1.2','TLSv1.3']" :key="l" :label="l" :value="l" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../../mixins/configPage'
import ConfigLayout from '../../components/ConfigLayout.vue'
import { saveConfigFile } from '@/api/erp'

const ALL = ['TLSv1.3', 'TLSv1.2', 'TLSv1.1', 'TLSv1']
export default {
  name: 'SettingsGwHttps',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'gw-https',
      helpMap: {
        '启用 HTTPS': '在 MC 端口就地 TLS。嗅探到 TLS 首包后动态挂 SslHandler，MC/明文 HTTP/HTTPS 三协议共存。',
        '主机名': '自签 CN/SAN 与 426 Location 使用的主机名。留空=127.0.0.1。',
        'PEM 证书路径': 'PEM 格式证书文件路径（可选）。',
        'PEM 私钥路径': 'PEM 格式私钥路径（可选，须 PKCS8 编码）。',
        'PKCS12 keystore': 'PKCS12 keystore 路径（可选，优先级高于 cert/key）。',
        'keystore 密码': 'PKCS12 keystore 密码。',
        '私钥密码': 'PEM 私钥密码（如加密）。',
        '启用协议版本': '启用的 TLS 协议版本。留空=全开 TLSv1.0~1.3（JVM 不支持的自动跳过）。',
        '最低 TLS': '最低允许的 TLS 协议版本。'
      }
    }
  },
  watch: {
    model: {
      handler(m) {
        if (m && (!m['enabled-protocols'] || !m['enabled-protocols'].length)) {
          this.$set(m, 'enabled-protocols', [...ALL])
        }
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
    // 保存前：全选时清空写回，保持 yml "空=全开" 语义
    save() {
      const p = this.model['enabled-protocols']
      if (p && ALL.every(x => p.includes(x))) this.$set(this.model, 'enabled-protocols', [])
      this.saving = true
      saveConfigFile(this.fileId, this.model).then(res => {
        this.saving = false
        this.original = JSON.stringify(this.model)
        this.$modal.msgSuccess('保存成功')
        const tip = res.data && res.data.tip
        if (tip) this.$alert(tip, '提示', { confirmButtonText: '知道了' })
      }).catch(() => { this.saving = false })
    }
  }
}
</script>

<style scoped>.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }</style>
