<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="16" style="padding-left: 20px">
        <h2>SOYSMonthlyCard 月卡管理台</h2>
        <p>
          欢迎回来，<b>{{ nickName || name }}</b>。这里是 SOYSMonthlyCard 月卡插件的 ERP 专属管理界面，
          提供礼包档位管理、礼品可视化配置、插件设置与领取审计日志的统一管理。
        </p>
        <el-descriptions :column="2" border size="small" style="margin-top:16px">
          <el-descriptions-item label="当前月份">{{ overview.currentMonth || '—' }}</el-descriptions-item>
          <el-descriptions-item label="领取日">每月 {{ overview.claimDay || 1 }} 号</el-descriptions-item>
          <el-descriptions-item label="在线玩家">{{ overview.onlinePlayers || 0 }} 人</el-descriptions-item>
          <el-descriptions-item label="本月领取">{{ overview.claimedThisMonth || 0 }} 次</el-descriptions-item>
        </el-descriptions>
      </el-col>
      <el-col :sm="24" :lg="8">
        <el-card shadow="hover" style="margin-top: 20px">
          <div slot="header"><b>快捷入口</b></div>
          <div style="line-height: 2.6">
            <el-button type="primary" size="mini" plain icon="el-icon-present" @click="go('/monthlycard/tiers')">礼包列表</el-button>
            <el-button type="warning" size="mini" plain icon="el-icon-setting" @click="go('/monthlycard/settings')">插件设置</el-button>
            <el-button type="info" size="mini" plain icon="el-icon-document" @click="go('/monthlycard/logs')">审计日志</el-button>
          </div>
        </el-card>
        <el-card shadow="hover" style="margin-top: 16px">
          <div slot="header"><b>功能说明</b></div>
          <ul style="font-size:13px;color:#606266;line-height:2;padding-left:18px;margin:0">
            <li>礼包列表：6 种操作（领取/重置/礼品配置/配置/记录/启停）</li>
            <li>礼品配置：MC 物品栏 + 仿 mcmod 编辑器 + 背包一键拷贝</li>
            <li>设置：基础/审计/存储后端/镜像策略</li>
            <li>日志：领取审计日志实时查看</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getOverview } from '@/api/monthlycard'

export default {
  name: 'Index',
  data() {
    return {
      overview: {}
    }
  },
  computed: {
    ...mapGetters(['name', 'nickName'])
  },
  created() {
    this.loadOverview()
  },
  methods: {
    loadOverview() {
      getOverview().then(res => {
        if (res.code === 200) this.overview = res.data || {}
      }).catch(() => {})
    },
    go(path) {
      if (window.__POWERED_BY_WUJIE__ && window.$wujie && window.$wujie.bus) {
        window.$wujie.bus.$emit('router-push', path)
      } else {
        this.$router.push(path)
      }
    }
  }
}
</script>

<style scoped>
.home h2 { margin-bottom: 8px; }
.home p { color: #606266; font-size: 14px; line-height: 1.7; }
</style>
