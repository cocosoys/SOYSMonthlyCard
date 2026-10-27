<template>
  <config-layout title="访问限制器" :help-map="helpMap" file="gateway/policies/access-limiter.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never">
        <div slot="header">固定窗口访问限制（{{ (model['path-patterns']||[]).length }} 条）</div>
        <el-form label-width="100px" size="small" style="margin-bottom:10px">
          <el-form-item label="启用">
            <el-switch v-model="model.enabled" />
            <div class="hint">超出窗口内 limit 次 → 429 + Retry-After；被动刷新（无后台定时任务）。</div>
          </el-form-item>
        </el-form>
        <el-table :data="model['path-patterns']" size="mini" border>
          <el-table-column label="名称" width="150">
            <template slot-scope="s"><el-input v-model="s.row.name" size="mini" /></template>
          </el-table-column>
          <el-table-column label="描述">
            <template slot-scope="s"><el-input v-model="s.row.description" size="mini" /></template>
          </el-table-column>
          <el-table-column label="维度" width="120">
            <template slot-scope="s">
              <el-select v-model="s.row.scope" size="mini">
                <el-option label="ip" value="ip" />
                <el-option label="key" value="key" />
                <el-option label="path" value="path" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="上限" width="90">
            <template slot-scope="s"><el-input-number v-model="s.row.limit" size="mini" :min="1" controls-position="right" style="width:90px" /></template>
          </el-table-column>
          <el-table-column label="窗口(秒)" width="110">
            <template slot-scope="s"><el-input-number v-model="s.row['window-seconds']" size="mini" :min="1" controls-position="right" style="width:100px" /></template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template slot-scope="s">
              <el-button type="text" size="mini" style="color:#f56c6c" @click="model['path-patterns'].splice(s.$index,1)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="mini" type="text" icon="el-icon-plus" style="margin-top:8px" @click="addRow">添加规则</el-button>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../../mixins/configPage'
import ConfigLayout from '../../components/ConfigLayout.vue'

export default {
  name: 'SettingsGwAccess',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'gw-access',
      helpMap: {
        '启用': '固定窗口访问限制的总开关。开启后，每个“维度”在“窗口(秒)”内超过“上限”次访问时，网关返回 429（Too Many Requests）并附带 Retry-After 响应头告知客户端等待时间。采用被动刷新机制：无需后台定时任务，计数随请求到达自然过期。',
        '名称': '规则的标识名称，仅用于在列表里区分不同规则。示例：登录接口限流、下载接口限流。',
        '描述': '该规则的用途说明。示例：限制 /api/login 每个 IP 每分钟最多 30 次。',
        '维度': '按什么维度统计访问次数：ip = 按客户端 IP（同一 IP 共享计数）；key = 按 API Key（同一密钥共享计数）；path = 按请求路径（同一路径共享计数）。示例：对登录接口选 ip，对下载接口选 key。',
        '上限': '每个窗口内该维度允许的最大访问次数，达到后立即触发 429。示例：50 表示窗口内最多 50 次。',
        '窗口(秒)': '固定窗口的时长（秒），窗口结束后计数自然清零。示例：3600 = 1 小时；60 = 1 分钟。'
      }
    }
  },
  methods: {
    addRow() {
      if (!Array.isArray(this.model['path-patterns'])) this.$set(this.model, 'path-patterns', [])
      this.model['path-patterns'].push({ name: '', description: '', scope: 'ip', limit: 50, 'window-seconds': 3600 })
    }
  }
}
</script>
