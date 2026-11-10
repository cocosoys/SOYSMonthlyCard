<template>
  <div class="app-container">
    <!-- 概览 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-blue">
          <div class="stat-lab">记录总数</div>
          <div class="stat-num">{{ total }}</div>
          <div class="stat-hint">当前筛选范围内</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-green">
          <div class="stat-lab">本月领取人数</div>
          <div class="stat-num">{{ topList.length }}</div>
          <div class="stat-hint">排行榜统计口径</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-gold">
          <div class="stat-lab">覆盖档位</div>
          <div class="stat-num">{{ tierOptions.length }}</div>
          <div class="stat-hint">rewards.yml 已配置档位</div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small" @submit.native.prevent>
        <el-form-item label="档位">
          <el-select v-model="queryParams.tierKey" clearable placeholder="全部档位" style="width:160px" @change="handleQuery">
            <el-option v-for="t in tierOptions" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="玩家">
          <el-input
            v-model="queryParams.player"
            clearable
            placeholder="玩家名（模糊匹配）"
            style="width:190px"
            @keyup.enter.native="handleQuery"
            @clear="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          <el-button type="success" icon="el-icon-present" :disabled="!queryParams.tierKey" @click="handleClaim">
            代玩家领取
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 记录表 -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>领取记录 <span class="muted-text">共 {{ total }} 条</span></span>
        <el-button type="primary" icon="el-icon-refresh" size="small" :loading="loading" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column prop="name" label="玩家" width="150">
          <template slot-scope="scope">
            <span class="player-name">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tier" label="档位" width="150">
          <template slot-scope="scope">
            <span :class="'tier-badge tier-' + scope.row.tier">{{ tierCN(scope.row.tier) }}</span>
            <span class="tier-key-en">{{ scope.row.tier }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="month" label="领取月份" width="120">
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.month || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uuid" label="UUID" min-width="260">
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.uuid }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button type="danger" size="mini" icon="el-icon-refresh-left" @click="handleReset(scope.row)">
              重置
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !rows.length" class="empty-state">
        暂无领取记录；玩家领取或管理员代领后会出现在这里
      </div>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :current-page.sync="queryParams.pageNum"
        :page-size.sync="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="loadList"
        @size-change="handleQuery"
      />
    </el-card>

    <!-- 排行榜 -->
    <el-card shadow="never" class="top-card">
      <div slot="header" class="card-header">
        <span>本月领取排行榜</span>
        <span class="muted-text">按当月已领档位数量排序</span>
      </div>
      <el-table :data="topList" size="small" border stripe>
        <el-table-column prop="rank" label="名次" width="80" align="center">
          <template slot-scope="scope">
            <span :class="'rank-badge rank-' + scope.row.rank">{{ scope.row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="玩家" min-width="160" />
        <el-table-column prop="count" label="本月领取档位数" width="150" align="right" />
      </el-table>
      <div v-if="!topList.length" class="empty-state">本月还没有领取记录</div>
    </el-card>
  </div>
</template>

<script>
import { getClaimList, getClaimTop, claimFor, resetFor, getErpTiers } from '@/api/monthlycard-erp'

const DEFAULT_TIERS = ['default', 'vip', 'mvp']

export default {
  name: 'MonthlyCardClaims',
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      topList: [],
      tierOptions: DEFAULT_TIERS.map(k => ({ key: k, label: '' })),
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tierKey: '',
        player: ''
      }
    }
  },
  created() {
    this.loadTiers()
    this.loadList()
    this.loadTop()
  },
  methods: {
    tierCN(key) {
      return { default: '基础月卡', vip: 'VIP 月卡', mvp: 'MVP 月卡' }[key] || key
    },
    loadTiers() {
      getErpTiers(100).then(res => {
        if (res.code === 200 && res.rows && res.rows.length) {
          this.tierOptions = res.rows.map(r => ({ key: r.key, label: this.tierCN(r.key) }))
        } else {
          this.tierOptions = DEFAULT_TIERS.map(k => ({ key: k, label: this.tierCN(k) }))
        }
      }).catch(() => {
        this.tierOptions = DEFAULT_TIERS.map(k => ({ key: k, label: this.tierCN(k) }))
      })
    },
    loadList() {
      this.loading = true
      const params = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize
      }
      if (this.queryParams.tierKey) params.tierKey = this.queryParams.tierKey
      if (this.queryParams.player) params.player = this.queryParams.player
      getClaimList(params).then(res => {
        this.loading = false
        if (res.code === 200) {
          this.rows = res.rows || []
          this.total = res.total || 0
        } else {
          this.$message.error(res.msg || '加载领取记录失败')
        }
      }).catch(() => {
        this.loading = false
      })
    },
    loadTop() {
      getClaimTop(10).then(res => {
        if (res.code === 200) this.topList = res.data || []
      }).catch(() => {})
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams.tierKey = ''
      this.queryParams.player = ''
      this.handleQuery()
    },
    handleClaim() {
      const key = this.queryParams.tierKey
      if (!key) return
      this.$prompt('请输入在线玩家名（离线玩家无法代领）', '代玩家领取 · ' + this.tierCN(key), {
        confirmButtonText: '确定领取',
        cancelButtonText: '取消',
        inputPlaceholder: '玩家名',
        inputValidator: v => (v && v.trim() ? true : '玩家名不能为空')
      }).then(({ value }) => {
        claimFor(key, value.trim()).then(res => {
          if (res.code === 200) {
            this.$message.success(res.msg || '已代领')
            this.loadList()
            this.loadTop()
          } else {
            this.$message.error(res.msg || '代领失败')
          }
        }).catch(() => {})
      }).catch(() => {})
    },
    handleReset(row) {
      this.$confirm(
        `确定重置「${row.name}」的「${this.tierCN(row.tier)}」领取记录吗？重置后该玩家本月可重新领取。`,
        '重置领取',
        { confirmButtonText: '确定重置', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        resetFor(row.tier, row.name).then(res => {
          if (res.code === 200) {
            this.$message.success(res.msg || '已重置')
            this.loadList()
            this.loadTop()
          } else {
            this.$message.error(res.msg || '重置失败')
          }
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border-left: 4px solid;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}
.stat-blue { border-left-color: #4a86f0; }
.stat-blue .stat-num { color: #2b5cb8; }
.stat-green { border-left-color: #46cf7a; }
.stat-green .stat-num { color: #2d9c56; }
.stat-gold { border-left-color: #ffb627; }
.stat-gold .stat-num { color: #c9821a; }
.stat-lab { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-num { font-size: 24px; font-weight: 700; }
.stat-hint { font-size: 11px; color: #c0c4cc; margin-top: 4px; }

.filter-card, .table-card, .top-card { border-radius: 8px; margin-bottom: 16px; }
.top-card { margin-bottom: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }

.player-name { font-weight: 600; color: #303133; }
.tier-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-right: 6px;
}
.tier-default { background: rgba(70, 207, 122, 0.12); color: #2d9c56; }
.tier-vip { background: rgba(73, 214, 255, 0.12); color: #00a8c7; }
.tier-mvp { background: rgba(255, 182, 39, 0.15); color: #c9821a; }
.tier-key-en { font-size: 11px; color: #909399; font-family: monospace; }
.mono-text { font-family: "Courier New", monospace; font-size: 12px; }
.muted-text { color: #909399; font-size: 12px; font-weight: 400; }
.empty-state { text-align: center; padding: 32px 0; color: #909399; font-size: 13px; }

.rank-badge {
  display: inline-block;
  min-width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 11px;
  font-size: 12px;
  background: #f0f2f5;
  color: #909399;
}
.rank-1 { background: #ffb627; color: #fff; }
.rank-2 { background: #c0c4cc; color: #fff; }
.rank-3 { background: #d98f5c; color: #fff; }

.pager { margin-top: 14px; text-align: right; }
</style>
