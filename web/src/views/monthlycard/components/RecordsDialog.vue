<template>
  <el-dialog
    :title="'领取记录 · ' + tierName"
    :visible.sync="visible"
    width="720px"
    append-to-body
  >
    <div v-loading="loading">
      <div class="records-summary">
        共 <b>{{ total }}</b> 条记录（按月份排序）
      </div>
      <el-table :data="records" size="small" border stripe max-height="420">
        <el-table-column prop="name" label="玩家" width="160" />
        <el-table-column prop="uuid" label="UUID" min-width="240">
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.uuid }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="month" label="领取月份" width="140">
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.month }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !records.length" class="empty-state">
        该档位暂无领取记录
      </div>
    </div>
    <div slot="footer">
      <el-button @click="visible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getTierRecords } from '@/api/monthlycard'

export default {
  name: 'RecordsDialog',
  props: {
    value: { type: Boolean, default: false },
    tierKey: { type: String, default: '' },
    tierName: { type: String, default: '' }
  },
  data() {
    return {
      visible: false,
      loading: false,
      records: [],
      total: 0
    }
  },
  watch: {
    value(val) {
      this.visible = val
      if (val) this.loadRecords()
    },
    visible(val) {
      this.$emit('input', val)
    }
  },
  methods: {
    loadRecords() {
      this.loading = true
      this.records = []
      this.total = 0
      getTierRecords(this.tierKey).then(res => {
        this.loading = false
        if (res.code === 200 && res.data) {
          this.records = res.data.records || []
          this.total = res.data.total || this.records.length
        }
      }).catch(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.records-summary { font-size: 13px; color: #606266; margin-bottom: 12px; }
.mono-text { font-family: "Courier New", monospace; font-size: 12px; }
.empty-state { text-align: center; padding: 40px 0; color: #909399; }
</style>
