<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" @submit.native.prevent>
      <el-form-item label="档位" prop="tierKey">
        <el-select v-model="queryParams.tierKey" clearable placeholder="全部档位" size="small" style="width:160px" @change="handleQuery">
          <el-option v-for="t in tierOptions" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
      </el-form-item>
      <el-form-item label="玩家" prop="player">
        <el-input
          v-model="queryParams.player"
          clearable
          placeholder="玩家名（模糊匹配）"
          size="small"
          style="width:190px"
          @keyup.enter.native="handleQuery"
          @clear="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-present"
          size="mini"
          :disabled="!queryParams.tierKey"
          @click="handleClaim"
        >代玩家领取</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="loadList"></right-toolbar>
    </el-row>

    <!-- 记录表 -->
    <el-table v-loading="loading" :data="rows" border stripe size="small">
      <el-table-column type="selection" width="50" align="center" />
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

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="loadList"
    />

    <div v-if="!loading && !rows.length" class="empty-state">
      暂无领取记录；玩家领取或管理员代领后会出现在这里
    </div>

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
      showSearch: true,
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
.top-card { border-radius: 8px; margin-top: 16px; }
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
</style>
