<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-cyan">
          <div class="stat-lab">在线玩家</div>
          <div class="stat-num">{{ overview.onlinePlayers || 0 }} <small>人</small></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-blue">
          <div class="stat-lab">记录玩家</div>
          <div class="stat-num">{{ overview.recordedPlayers || 0 }} <small>人</small></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-gold">
          <div class="stat-lab">本月领取</div>
          <div class="stat-num">{{ overview.claimedThisMonth || 0 }} <small>次</small></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-green">
          <div class="stat-lab">礼包档位</div>
          <div class="stat-num">{{ overview.tiersCount || 0 }} <small>个 · {{ overview.currentMonth || '' }}</small></div>
        </div>
      </el-col>
    </el-row>

    <!-- 档位表格 -->
    <el-card shadow="never" class="tiers-card">
      <div slot="header" class="card-header">
        <span>月卡礼包档位</span>
        <el-button type="primary" icon="el-icon-refresh" size="small" @click="loadData" :loading="loading">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="tiers" border stripe size="small">
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
      <div v-if="!loading && !tiers.length" class="empty-state">
        暂无礼包档位，请检查 rewards.yml
      </div>
    </el-card>

    <!-- 弹窗 -->
    <PlayerActionDialog v-model="claimVisible" :tier-key="currentTierKey" :tier-name="currentTierName" mode="claim" @success="loadData" />
    <PlayerActionDialog v-model="resetVisible" :tier-key="currentTierKey" :tier-name="currentTierName" mode="reset" @success="loadData" />
    <GiftConfigDialog v-model="giftVisible" :tier-key="currentTierKey" :tier-name="currentTierName" @saved="loadData" />
    <TierConfigDialog v-model="configVisible" :tier-key="currentTierKey" :tier-name="currentTierName" @saved="loadData" />
    <RecordsDialog v-model="recordsVisible" :tier-key="currentTierKey" :tier-name="currentTierName" />
  </div>
</template>

<script>
import { getOverview, getTiers, toggleTier } from '@/api/monthlycard'
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
      overview: {},
      tiers: [],
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
      getOverview().then(res => {
        if (res.code === 200) this.overview = res.data || {}
      }).catch(() => {})
      getTiers().then(res => {
        this.loading = false
        if (res.code === 200) {
          this.tiers = res.data || []
        } else {
          this.$message.error(res.msg || '加载档位列表失败')
        }
      }).catch(() => {
        this.loading = false
      })
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
        toggleTier(row.key).then(res => {
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
.stat-row { margin-bottom: 18px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border-left: 4px solid;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}
.stat-cyan { border-left-color: #49d6ff; }
.stat-cyan .stat-num { color: #00a8c7; }
.stat-blue { border-left-color: #4a86f0; }
.stat-blue .stat-num { color: #2b5cb8; }
.stat-gold { border-left-color: #ffb627; }
.stat-gold .stat-num { color: #c9821a; }
.stat-green { border-left-color: #46cf7a; }
.stat-green .stat-num { color: #2d9c56; }
.stat-lab { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-num { font-size: 24px; font-weight: 700; }
.stat-num small { font-size: 12px; font-weight: 400; color: #909399; }
.tiers-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
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
