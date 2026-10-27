<template>
  <div class="app-container">
    <el-card shadow="never" class="logs-card" v-loading="loading">
      <div slot="header" class="card-header">
        <div class="header-left">
          <span>领取审计日志</span>
          <el-tag :type="logEnabled ? 'success' : 'info'" size="mini" style="margin-left:10px">
            {{ logEnabled ? '审计开启' : '审计关闭' }}
          </el-tag>
        </div>
        <el-button type="primary" icon="el-icon-refresh" size="small" @click="loadLogs" :loading="loading">刷新</el-button>
      </div>

      <div class="log-file-info">
        <i class="el-icon-document"></i>
        文件：{{ logFile || 'logs/claims.log' }}（插件数据目录下，显示末尾 500 行）
      </div>

      <div class="log-viewer" ref="logViewer">
        <div v-if="!loading && !lines.length" class="empty-state">
          暂无日志内容
        </div>
        <div v-for="(line, idx) in lines" :key="idx" class="log-line" :class="lineClass(line)">
          <span class="log-line-num">{{ idx + 1 }}</span>
          <span class="log-line-text">{{ line }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getLogs } from '@/api/monthlycard'

export default {
  name: 'MonthlyCardLogs',
  data() {
    return {
      loading: false,
      lines: [],
      logEnabled: false,
      logFile: ''
    }
  },
  created() {
    this.loadLogs()
  },
  methods: {
    loadLogs() {
      this.loading = true
      this.lines = []
      getLogs().then(res => {
        this.loading = false
        if (res.code === 200 && res.data) {
          this.logEnabled = res.data.enabled || false
          this.logFile = res.data.file || ''
          this.lines = res.data.lines || []
          this.$nextTick(() => {
            if (this.$refs.logViewer) {
              this.$refs.logViewer.scrollTop = this.$refs.logViewer.scrollHeight
            }
          })
        } else {
          this.$message.error(res.msg || '加载日志失败')
        }
      }).catch(() => {
        this.loading = false
      })
    },
    lineClass(line) {
      if (/失败|错误|异常|error|denied/i.test(line)) return 'log-err'
      if (/成功|领取|claim|补发/i.test(line)) return 'log-ok'
      return ''
    }
  }
}
</script>

<style scoped>
.logs-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.log-file-info {
  font-size: 13px;
  color: #606266;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 12px;
}
.log-viewer {
  background: #1e1e2e;
  border-radius: 8px;
  padding: 12px;
  height: 520px;
  overflow-y: auto;
  font-family: "Courier New", "Consolas", monospace;
  font-size: 12.5px;
  line-height: 1.7;
}
.log-line {
  display: flex;
  gap: 10px;
  padding: 1px 4px;
  border-radius: 3px;
}
.log-line:hover { background: rgba(255,255,255,0.05); }
.log-line-num {
  color: #4a4a6a;
  user-select: none;
  min-width: 36px;
  text-align: right;
  flex-shrink: 0;
}
.log-line-text { color: #c8c8d8; word-break: break-all; }
.log-err .log-line-text { color: #ff6b6b; }
.log-ok .log-line-text { color: #5ddb6b; }
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #6a6a8a;
  font-family: "PingFang SC", sans-serif;
}
</style>
