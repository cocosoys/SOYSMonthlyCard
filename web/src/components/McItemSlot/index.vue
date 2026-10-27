<template>
  <div
    class="mc-slot"
    :class="{ enchanted: hasEnchant, empty: !item }"
    @click="handleClick"
    @mouseenter="showTip = true"
    @mouseleave="showTip = false"
    @mousemove="moveTip"
  >
    <div v-if="item" class="mc-px">
      <span v-if="!iconSvg" class="mc-ph">{{ firstChar }}</span>
      <span v-else class="mc-icon-svg" v-html="iconSvg"></span>
    </div>
    <span v-if="item && amount > 1" class="mc-amt">{{ amount }}</span>
    <div v-if="showTip && item" class="mc-tooltip" :style="tipStyle">
      <div class="mc-tt-name" :style="{ color: item.name ? '#fff' : '#55ff55' }">{{ displayName }}</div>
      <div v-if="item.lore && item.lore.length" class="mc-tt-lore">
        <div v-for="(line, i) in item.lore" :key="i">{{ line }}</div>
      </div>
      <div v-if="hasEnchant" class="mc-tt-extra">
        <div v-for="(e, i) in item.enchants" :key="i">{{ formatEnchant(e) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'McItemSlot',
  props: {
    item: { type: Object, default: null },
    size: { type: Number, default: 52 }
  },
  data() {
    return {
      showTip: false,
      tipX: 0,
      tipY: 0
    }
  },
  computed: {
    amount() {
      return parseInt(this.item && this.item.amount, 10) || 1
    },
    hasEnchant() {
      return this.item && this.item.enchants && this.item.enchants.length > 0
    },
    firstChar() {
      return this.item && this.item.material ? this.item.material.charAt(0) : '?'
    },
    displayName() {
      if (this.item && this.item.name) return this.item.name
      return this.item ? this.prettyMaterial(this.item.material) : ''
    },
    iconSvg() {
      return null
    },
    tipStyle() {
      return { left: this.tipX + 'px', top: this.tipY + 'px' }
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.item)
    },
    moveTip(e) {
      this.tipX = e.clientX + 14
      this.tipY = e.clientY + 12
    },
    prettyMaterial(mat) {
      if (!mat) return ''
      return mat.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
    },
    formatEnchant(e) {
      const parts = e.split(':')
      const names = {
        DAMAGE_ALL: '锋利', DAMAGE_UNDEAD: '亡灵杀手', DAMAGE_ARTHROPODS: '节肢杀手',
        KNOCKBACK: '击退', FIRE_ASPECT: '火焰附加', LOOT_BONUS_MOBS: '抢夺',
        DIG_SPEED: '效率', SILK_TOUCH: '精准采集', DURABILITY: '耐久', LOOT_BONUS_BLOCKS: '时运',
        PROTECTION_ENVIRONMENTAL: '保护', PROTECTION_FIRE: '火焰保护', PROTECTION_FALL: '摔落保护',
        PROTECTION_EXPLOSIONS: '爆炸保护', PROTECTION_PROJECTILE: '弹射物保护', THORNS: '荆棘',
        OXYGEN: '水下呼吸', WATER_WORKER: '水下速掘',
        ARROW_DAMAGE: '力量', ARROW_KNOCKBACK: '冲击', ARROW_FIRE: '火矢', ARROW_INFINITE: '无限'
      }
      const roman = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']
      return (names[parts[0]] || parts[0]) + ' ' + (roman[parseInt(parts[1], 10)] || parts[1] || 'I')
    }
  }
}
</script>

<style scoped>
.mc-slot {
  width: 52px;
  height: 52px;
  background: #8b8b8b;
  border-radius: 4px;
  position: relative;
  border-top: 3px solid #373737;
  border-left: 3px solid #373737;
  border-right: 3px solid #ffffff;
  border-bottom: 3px solid #ffffff;
  cursor: pointer;
  flex-shrink: 0;
}
.mc-slot.empty { cursor: default; opacity: 0.6; }
.mc-slot:hover:not(.empty) { filter: brightness(1.15); }
.mc-px {
  position: absolute;
  inset: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mc-ph {
  font-family: "Courier New", monospace;
  font-weight: 800;
  font-size: 20px;
  color: #5a5a5a;
  text-shadow: 1px 1px 0 #c6c6c6;
}
.mc-amt {
  position: absolute;
  right: 3px;
  bottom: 1px;
  font-size: 14px;
  font-weight: 800;
  color: #fff;
  text-shadow: 1px 1px 0 #3f3f3f, 2px 2px 0 #3f3f3f;
  font-family: "Courier New", monospace;
  z-index: 2;
}
.mc-slot.enchanted::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(120,80,255,0.3) 0%, transparent 50%, rgba(120,80,255,0.2) 100%);
  border-radius: 2px;
  pointer-events: none;
  animation: glint 2s linear infinite;
}
@keyframes glint {
  0% { background-position: -100% 0; }
  100% { background-position: 200% 0; }
}
.mc-tooltip {
  position: fixed;
  z-index: 9999;
  background: rgba(16, 4, 32, 0.94);
  border: 2px solid #2a0a50;
  border-radius: 4px;
  padding: 7px 10px;
  font-family: "Courier New", monospace;
  font-size: 13px;
  pointer-events: none;
  max-width: 280px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.6);
  color: #aaa;
}
.mc-tt-name { font-weight: 800; margin-bottom: 3px; }
.mc-tt-lore { color: #a9a9a9; white-space: pre-line; }
.mc-tt-extra { color: #754fd0; margin-top: 3px; }
</style>
