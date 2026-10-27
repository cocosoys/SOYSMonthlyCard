<template>
  <config-layout title="IP 白/黑名单" :help-map="helpMap" file="gateway/policies/ip-allowlist.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never">
        <div slot="header">IP 访问控制</div>
        <el-form label-width="180px" size="small">
          <el-form-item label="启用">
            <el-switch v-model="model.enabled" />
          </el-form-item>
          <el-form-item label="默认策略">
            <el-radio-group v-model="model.default">
              <el-radio label="allow">allow（列表=黑名单，其余放行）</el-radio>
              <el-radio label="deny">deny（列表=白名单，仅命中放行）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="信任前置代理">
            <el-switch v-model="model['trust-proxy']" />
            <div class="hint">优先取 X-Forwarded-For 首个 IP。</div>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card shadow="never" style="margin-top:14px">
        <div slot="header">IP / CIDR 列表</div>
        <div v-for="(ip,i) in (model.list||[])" :key="i" class="srow">
          <el-input :value="ip" size="small" @change="v => $set(model.list, i, v)" placeholder="如 127.0.0.1 或 192.168.1.0/24" />
          <el-button type="text" size="mini" icon="el-icon-delete" @click="model.list.splice(i,1)" />
        </div>
        <el-button size="mini" type="text" icon="el-icon-plus" @click="model.list.push('')">添加一行</el-button>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../../mixins/configPage'
import ConfigLayout from '../../components/ConfigLayout.vue'

export default {
  name: 'SettingsGwAllow',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'gw-allow',
      helpMap: {
        '启用': 'IP 白/黑名单策略总开关。关闭时不做任何 IP 过滤，所有请求按正常流程处理。',
        '默认策略': '列表中 IP 的默认处置方式：allow = 列表即黑名单，命中列表的 IP 拒绝访问，其余全部放行；deny = 列表即白名单，仅命中列表的 IP 放行，其余全部拒绝。',
        '信任前置代理': 'true 时优先取 X-Forwarded-For 请求头的首个 IP 作为客户端地址（适用于前置了 Nginx/CDN 等可信代理的场景）；false 时直接使用 TCP 连接的对端 IP。开启后请确保请求只来自可信代理，否则攻击者可伪造该请求头绕过限制。',
        'IP / CIDR 列表': 'IP 或 CIDR 段列表，每行一个。支持 IPv4 单 IP（如 127.0.0.1）与 CIDR 网段（如 192.168.1.0/24 表示 192.168.1.0 ~ 192.168.1.255）。配合“默认策略”生效：default=allow 时此处为黑名单，default=deny 时此处为白名单。示例：127.0.0.1、10.0.0.0/8、114.114.114.114。',
        'IP 访问控制': '按 IP/CIDR 段进行访问控制。default=allow 时列表即黑名单；default=deny 时列表即白名单。'
      }
    }
  }
}
</script>

<style scoped>
.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }
.srow { display: flex; align-items: center; margin-bottom: 6px; }
.srow .el-input { flex: 1; }
</style>
