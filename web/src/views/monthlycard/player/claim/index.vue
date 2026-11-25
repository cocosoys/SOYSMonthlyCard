<template>
  <div class="player-claim-wrap">
    <iframe
      ref="frame"
      class="player-claim-frame"
      :src="pageUrl"
      frameborder="0"
      sandbox="allow-scripts allow-same-origin allow-modals allow-popups"
      title="月卡玩家中心"
      @load="onLoad"
    />
    <div v-if="loading" class="player-claim-loading">月卡玩家中心加载中…</div>
  </div>
</template>

<script>
/**
 * 玩家领取页（v7 票券凭证 · 验票打孔盘）—— 以独立静态页 100% 保真承载概念稿视觉。
 *
 * <p>视觉本体为 web/public/player/claim.html（由 concepts/claim-page-cyberpunk-v7.html
 * 原样复制 + assets 资产 + 真实数据桥接脚本）。本视图只做 iframe 容器：
 * ① 静态页与资产在同源下由框架托管，iframe 内脚本自行 fetch
 *    /api/plugins/SOYSMonthlyCard/view/tiers 与 /player/state 获取真实数据；
 * ② 用 iframe 隔离概念稿的 body 级背景/全屏 CSS，避免污染 SPA；
 * ③ 领取按钮引导玩家回游戏内 /monthlycard（后端无网页在线发放端点）。</p>
 *
 * <p>pageUrl 由 soysContext.pageBase（插件 SPA 根）拼接 publicPath 下的 player/claim.html。
 * hash 路由模式下 iframe src 用相对路径 "./player/claim.html" 更稳妥，故默认相对路径。</p>
 */
export default {
  name: 'MonthlyCardPlayerClaim',
  data() {
    return {
      loading: true,
      // public/ 下的静态页在打包后位于应用根；相对路径同时兼容 history 与 hash 模式
      pageUrl: 'player/claim.html'
    }
  },
  methods: {
    onLoad() {
      this.loading = false
    }
  }
}
</script>

<style scoped>
.player-claim-wrap {
  position: relative;
  width: 100%;
  height: calc(100vh - 84px);
  min-height: 560px;
  overflow: hidden;
  background: #0a0e14;
}
.player-claim-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}
.player-claim-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #00f0ff;
  font-family: Consolas, monospace;
  font-size: 14px;
  letter-spacing: 2px;
  pointer-events: none;
}
</style>
