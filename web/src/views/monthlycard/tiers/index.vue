<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="档位" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="档位名 / 权限节点"
          clearable
          size="small"
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="全部" clearable size="small" style="width: 120px" @change="handleQuery">
          <el-option label="启用中" :value="true" />
          <el-option label="已禁用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="loadData"></right-toolbar>
    </el-row>

    <!-- 档位表格 -->
    <el-table v-loading="loading" :data="pageRows" border stripe size="small">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column prop="key" label="档位" width="130">
        <template slot-scope="scope">
          <div class="tier-key-cell">
            <span :class="'tier-badge tier-' + scope.row.key">{{ tierCN(scope.row.key) }}</span>
            <span class="tier-key-en">{{ scope.row.key }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="permission" label="权限节点" min-width="180">
        <template slot-scope="scope">
          <span v-if="scope.row.permission" class="mono-text">{{ scope.row.permission }}</span>
          <span v-else class="muted-text">无（所有人）</span>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="mini">
            {{ scope.row.enabled ? '启用中' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="money" label="金币" width="80" align="right">
        <template slot-scope="scope">
          <span v-if="scope.row.money > 0">{{ scope.row.money }}</span>
          <span v-else class="muted-text">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="点券" width="80" align="right">
        <template slot-scope="scope">
          <span v-if="scope.row.points > 0">{{ scope.row.points }}</span>
          <span v-else class="muted-text">—</span>
        </template>
      </el-table-column>
      <el-table-column label="本月领取" width="130" align="center">
        <template slot-scope="scope">
          <div><b>{{ scope.row.claimedThisMonth || 0 }}</b> <span class="muted-text">人领取</span></div>
          <div class="muted-text" style="font-size:11px;margin-top:2px;">
            物品 {{ scope.row.itemsCount || 0 }} · 指令 {{ scope.row.commandsCount || 0 }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" align="center" fixed="right">
        <template slot-scope="scope">
          <div class="row-actions">
            <el-button type="success" size="mini" icon="el-icon-present" @click="openClaim(scope.row)">领取</el-button>
            <el-button type="danger" size="mini" icon="el-icon-refresh-left" @click="openReset(scope.row)">重置领取</el-button>
            <el-button type="warning" size="mini" icon="el-icon-box" @click="openGiftConfig(scope.row)">礼品配置</el-button>
            <el-button size="mini" icon="el-icon-setting" @click="openTierConfig(scope.row)">配置</el-button>
            <el-button size="mini" icon="el-icon-document" @click="openRecords(scope.row)">领取记录</el-button>
            <el-button
              :type="scope.row.enabled ? 'info' : 'success'"
              size="mini"
              :icon="scope.row.enabled ? 'el-icon-close' : 'el-icon-check'"
              @click="handleToggle(scope.row)"
            >{{ scope.row.enabled ? '禁用' : '启用' }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="applyFilter"
    />

    <div v-if="!loading && !pageRows.length" class="empty-state">
      暂无礼包档位，请检查 rewards.yml
    </div>

    <!-- 弹窗 -->
    <PlayerActionDialog v-model="claimVisible" :tier-key="currentTierKey" :tier-name="currentTierName" mode="claim" @success="loadData" />
    <PlayerActionDialog v-model="resetVisible" :tier-key="currentTierKey" :tier-name="currentTierName" mode="reset" @success="loadData" />
    <GiftConfigDialog v-model="giftVisible" :tier-key="currentTierKey" :tier-name="currentTierName" @saved="loadData" />
    <TierConfigDialog v-model="configVisible" :tier-key="currentTierKey" :tier-name="currentTierName" @saved="loadData" />
    <RecordsDialog v-model="recordsVisible" :tier-key="currentTierKey" :tier-name="currentTierName" />
  </div>
</template>

<script>
import { getErpTiers, toggleErpTier } from '@/api/monthlycard-erp'
import PlayerActionDialog from '../components/PlayerActionDialog.vue'
import GiftConfigDialog from '../components/GiftConfigDialog.vue'
import TierConfigDialog from '../components/TierConfigDialog.vue'
import RecordsDialog from '../components/RecordsDialog.vue'

export default {
  name: 'MonthlyCardTiers',
  components: { PlayerActionDialog, GiftConfigDialog, TierConfigDialog, RecordsDialog },
  data() {
    return {
      loading: false,
      showSearch: true,
      tiers: [],
      pageRows: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: '',
        enabled: ''
      },
      currentTierKey: '',
      currentTierName: '',
      claimVisible: false,
      resetVisible: false,
      giftVisible: false,
      configVisible: false,
      recordsVisible: false
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loading = true
      getErpTiers().then(res => {
        this.loading = false
        if (res.code === 200) {
          this.tiers = res.rows || []
        } else {
          this.$message.error(res.msg || '加载档位列表失败')
          this.tiers = []
        }
        this.applyFilter()
      }).catch(() => {
        this.loading = false
        this.tiers = []
        this.applyFilter()
      })
    },
    /** 客户端筛选 + 分页 */
    applyFilter() {
      const kw = (this.queryParams.keyword || '').trim().toLowerCase()
      const status = this.queryParams.enabled
      let list = this.tiers
      if (kw) {
        list = list.filter(t =>
          (t.key && t.key.toLowerCase().indexOf(kw) > -1) ||
          (t.permission && t.permission.toLowerCase().indexOf(kw) > -1)
        )
      }
      if (status !== '' && status !== null && status !== undefined) {
        list = list.filter(t => !!t.enabled === status)
      }
      this.total = list.length
      const start = (this.queryParams.pageNum - 1) * this.queryParams.pageSize
      this.pageRows = list.slice(start, start + this.queryParams.pageSize)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.applyFilter()
    },
    resetQuery() {
      this.queryParams.keyword = ''
      this.queryParams.enabled = ''
      this.handleQuery()
    },
    tierCN(key) {
      return { default: '基础月卡', vip: 'VIP 月卡', mvp: 'MVP 月卡' }[key] || key
    },
    setCurrent(row) {
      this.currentTierKey = row.key
      this.currentTierName = this.tierCN(row.key)
    },
    openClaim(row) {
      this.setCurrent(row)
      this.claimVisible = true
    },
    openReset(row) {
      this.setCurrent(row)
      this.resetVisible = true
    },
    openGiftConfig(row) {
      this.setCurrent(row)
      this.giftVisible = true
    },
    openTierConfig(row) {
      this.setCurrent(row)
      this.configVisible = true
    },
    openRecords(row) {
      this.setCurrent(row)
      this.recordsVisible = true
    },
    handleToggle(row) {
      this.$confirm(`确定要${row.enabled ? '禁用' : '启用'}「${this.tierCN(row.key)}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        toggleErpTier(row.key).then(res => {
          if (res.code === 200) {
            this.$message.success(res.msg || '状态已切换')
            this.loadData()
          } else {
            this.$message.error(res.msg || '操作失败')
          }
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.tier-key-cell { display: flex; flex-direction: column; gap: 2px; }
.tier-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  width: fit-content;
}
.tier-default { background: rgba(70,207,122,0.12); color: #2d9c56; }
.tier-vip { background: rgba(73,214,255,0.12); color: #00a8c7; }
.tier-mvp { background: rgba(255,182,39,0.15); color: #c9821a; }
.tier-key-en { font-size: 11px; color: #909399; font-family: monospace; }
.mono-text { font-family: "Courier New", monospace; font-size: 12px; }
.muted-text { color: #c0c4cc; }
.row-actions { display: flex; flex-wrap: wrap; gap: 4px; justify-content: center; }
.empty-state { text-align: center; padding: 40px 0; color: #909399; }
</style>
