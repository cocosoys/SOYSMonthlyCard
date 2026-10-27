<template>
  <div class="app-container">
    <el-card shadow="never" class="settings-card" v-loading="loading">
      <div slot="header" class="card-header">
        <span>月卡插件设置</span>
        <div>
          <el-button icon="el-icon-refresh" size="small" @click="loadSettings" style="margin-right:8px">重新加载</el-button>
          <el-button type="primary" icon="el-icon-check" size="small" :loading="saving" @click="handleSave">保存设置</el-button>
        </div>
      </div>

      <el-form label-width="160px" size="small">
        <!-- 基础设置 -->
        <el-divider content-position="left">基础设置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="每月领取日">
              <el-input-number v-model="settings['claim-day']" :min="1" :max="28" controls-position="right" />
              <span class="form-tip">（1-28）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="月份格式">
              <el-input v-model="settings['month-format']" placeholder="Java 日期模式" />
              <span class="form-tip">如 yyyy-MM</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="金币单位文案">
              <el-input v-model="settings['units.money']" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点券单位文案">
              <el-input v-model="settings['units.points']" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="自动领取">
          <el-switch v-model="settings['auto-claim-on-join']" active-text="玩家上线时自动尝试领取" />
        </el-form-item>
        <el-form-item label="离线补发">
          <el-switch v-model="settings['offline-compensation']" active-text="可领期间离线，上线自动补发" />
        </el-form-item>
        <el-form-item label="调试模式">
          <el-switch v-model="settings['debug']" active-text="输出存储读写细节" />
        </el-form-item>

        <!-- 审计 -->
        <el-divider content-position="left">领取审计</el-divider>
        <el-form-item label="启用审计日志">
          <el-switch v-model="settings['audit.enabled']" active-text="记录所有领取/重置/代领操作" />
        </el-form-item>
        <el-form-item label="日志文件路径">
          <el-input v-model="settings['audit.file']" disabled />
          <span class="form-tip">插件数据目录下，暂不支持网页修改</span>
        </el-form-item>

        <!-- 存储后端 -->
        <el-divider content-position="left">存储后端（主辅模型）</el-divider>
        <el-form-item label="YAML 文件">
          <el-switch v-model="backends.yaml.enabled" active-text="启用 YAML 存储" />
        </el-form-item>
        <el-form-item label="SQLite 数据库">
          <el-switch v-model="backends.sqlite.enabled" active-text="启用 SQLite 存储" />
        </el-form-item>
        <el-form-item label="MySQL 数据库">
          <el-switch v-model="backends.mysql.enabled" active-text="启用 MySQL 存储" />
        </el-form-item>
        <el-alert
          title="优先级 MySQL > SQLite > YAML；启用的最高优先级后端为主存储。后端启用变更需重启服务器生效，连接串/账号请在 config.yml 中配置（网页不处理密码）。"
          type="warning"
          :closable="false"
          show-icon
        />

        <!-- 镜像策略 -->
        <el-divider content-position="left">镜像策略（辅助存储热备份）</el-divider>
        <el-form-item label="写入时镜像">
          <el-switch v-model="mirror.enabled" active-text="写入时镜像到辅助存储" />
        </el-form-item>
        <el-form-item label="异步镜像">
          <el-switch v-model="mirror.async" active-text="镜像写入异步执行（推荐）" />
        </el-form-item>
        <el-form-item label="启动同步">
          <el-switch v-model="mirror['sync-on-startup']" active-text="启动时主存储全量同步到辅助存储" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getSettings, saveSettings } from '@/api/monthlycard'

export default {
  name: 'MonthlyCardSettings',
  data() {
    return {
      loading: false,
      saving: false,
      settings: {
        'claim-day': 1,
        'month-format': 'yyyy-MM',
        'units.money': '金币',
        'units.points': '点券',
        'auto-claim-on-join': false,
        'offline-compensation': false,
        'debug': false,
        'audit.enabled': true,
        'audit.file': 'logs/claims.log'
      },
      backends: {
        yaml: { enabled: true },
        sqlite: { enabled: false },
        mysql: { enabled: false }
      },
      mirror: {
        enabled: false,
        async: true,
        'sync-on-startup': false
      }
    }
  },
  created() {
    this.loadSettings()
  },
  methods: {
    loadSettings() {
      this.loading = true
      getSettings().then(res => {
        this.loading = false
        if (res.code === 200 && res.data) {
          const s = res.data.settings || {}
          const be = res.data.backends || {}
          const mi = res.data.mirror || {}
          this.settings = { ...this.settings, ...s }
          if (s.units) {
            this.settings['units.money'] = s.units.money || '金币'
            this.settings['units.points'] = s.units.points || '点券'
          }
          this.backends = {
            yaml: { enabled: (be.yaml && be.yaml.enabled) || false },
            sqlite: { enabled: (be.sqlite && be.sqlite.enabled) || false },
            mysql: { enabled: (be.mysql && be.mysql.enabled) || false }
          }
          this.mirror = {
            enabled: mi.enabled || false,
            async: mi.async !== false,
            'sync-on-startup': mi['sync-on-startup'] || false
          }
        }
      }).catch(() => {
        this.loading = false
      })
    },
    handleSave() {
      this.saving = true
      const payload = {
        settings: {
          'claim-day': this.settings['claim-day'],
          'month-format': this.settings['month-format'],
          'auto-claim-on-join': this.settings['auto-claim-on-join'],
          'offline-compensation': this.settings['offline-compensation'],
          'debug': this.settings['debug'],
          'audit.enabled': this.settings['audit.enabled'],
          units: {
            money: this.settings['units.money'],
            points: this.settings['units.points']
          }
        },
        backends: {
          yaml: { enabled: this.backends.yaml.enabled },
          sqlite: { enabled: this.backends.sqlite.enabled },
          mysql: { enabled: this.backends.mysql.enabled }
        },
        mirror: {
          enabled: this.mirror.enabled,
          async: this.mirror.async,
          'sync-on-startup': this.mirror['sync-on-startup']
        }
      }
      saveSettings(payload).then(res => {
        this.saving = false
        if (res.code === 200) {
          this.$message.success(res.msg || '设置已保存')
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      }).catch(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.settings-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-tip { font-size: 12px; color: #909399; margin-left: 8px; }
</style>
