<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    width="780px"
    :close-on-click-modal="false"
    append-to-body
    custom-class="item-editor-dialog"
  >
    <div class="item-editor">
      <!-- 左侧预览 -->
      <div class="ie-preview">
        <div class="ie-big-item" :class="{ enchanted: draft.enchants.length > 0 }">
          <div class="ie-icon">
            <span v-if="!iconMatrix" class="ie-ph">{{ firstChar }}</span>
          </div>
          <div class="ie-glint"></div>
        </div>
        <div class="ie-tooltip">
          <div class="ie-tt-name" :style="{ color: draft.name ? '#fff' : '#55ff55' }">{{ displayName }}</div>
          <div v-if="draft.lore && draft.lore.join('').length" class="ie-tt-lore">
            <div v-for="(line, i) in draft.lore" :key="i">{{ line }}</div>
          </div>
          <div v-if="draft.enchants.length" class="ie-tt-extra">
            <div v-for="(e, i) in draft.enchants" :key="i">{{ formatEnchant(e) }}</div>
          </div>
        </div>
      </div>
      <!-- 右侧表单 -->
      <div class="ie-form">
        <el-form label-width="90px" size="small">
          <el-form-item label="物品材质">
            <el-input v-model="draft.material" list="mat-list" placeholder="如 DIAMOND, NETHER_STAR" />
            <datalist id="mat-list">
              <option v-for="m in commonMats" :key="m" :value="m" />
            </datalist>
          </el-form-item>
          <el-row :gutter="10">
            <el-col :span="12">
              <el-form-item label="数量">
                <el-input-number v-model="draft.amount" :min="1" :max="64" controls-position="right" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="数据值">
                <el-input-number v-model="draft.data" :min="0" :max="15" controls-position="right" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="自定义名称">
            <el-input v-model="draft.name" placeholder="支持 & 颜色码，如 &bVIP 专属" />
          </el-form-item>
          <el-form-item label="物品描述">
            <el-input type="textarea" v-model="loreText" :rows="3" placeholder="每行一条，支持 & 颜色码" />
          </el-form-item>
          <el-form-item label="附魔列表">
            <div class="ie-ench-list">
              <div v-for="(e, i) in draft.enchants" :key="i" class="ie-ench-row">
                <el-select v-model="draft.enchants[i]" size="small" style="flex:1">
                  <el-option v-for="en in enchants" :key="en[0]" :label="en[1] + ' (' + en[0] + ')'" :value="en[0] + ':' + (e.split(':')[1] || 1)" />
                </el-select>
                <el-input-number
                  :value="parseInt(e.split(':')[1], 10) || 1"
                  :min="1" :max="10" size="small" controls-position="right"
                  style="width:90px"
                  @input="updateEnchantLevel(i, $event)"
                />
                <el-button type="danger" icon="el-icon-delete" circle size="mini" @click="removeEnchant(i)" />
              </div>
              <el-button type="primary" plain size="small" icon="el-icon-plus" @click="addEnchant">添加附魔</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <div slot="footer">
      <el-button v-if="showDelete" type="danger" icon="el-icon-delete" @click="handleDelete" style="float:left">删除此物品</el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="warning" @click="handleSave">保存物品</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ItemEditor',
  props: {
    value: { type: Boolean, default: false },
    initial: { type: Object, default: () => ({ material: 'DIAMOND', amount: 1 }) },
    title: { type: String, default: '物品编辑器' },
    showDelete: { type: Boolean, default: false }
  },
  data() {
    return {
      visible: false,
      draft: { material: 'DIAMOND', amount: 1, data: 0, name: '', lore: [], enchants: [] },
      loreText: '',
      commonMats: ['DIAMOND','EMERALD','IRON_INGOT','GOLD_INGOT','NETHER_STAR','TOTEM','DIAMOND_SWORD',
        'DIAMOND_PICKAXE','ENDER_PEARL','GOLDEN_APPLE','EXPERIENCE_BOTTLE','CHEST','BOOK','REDSTONE','OBSIDIAN',
        'BEDROCK','APPLE','BREAD','COOKED_BEEF','CAKE','ARROW','BOW','GOLDEN_HELMET','IRON_CHESTPLATE','SKULL_ITEM'],
      enchants: [
        ['DAMAGE_ALL', '锋利'], ['DAMAGE_UNDEAD', '亡灵杀手'], ['DAMAGE_ARTHROPODS', '节肢杀手'],
        ['KNOCKBACK', '击退'], ['FIRE_ASPECT', '火焰附加'], ['LOOT_BONUS_MOBS', '抢夺'],
        ['DIG_SPEED', '效率'], ['SILK_TOUCH', '精准采集'], ['DURABILITY', '耐久'], ['LOOT_BONUS_BLOCKS', '时运'],
        ['PROTECTION_ENVIRONMENTAL', '保护'], ['PROTECTION_FIRE', '火焰保护'], ['PROTECTION_FALL', '摔落保护'],
        ['PROTECTION_EXPLOSIONS', '爆炸保护'], ['PROTECTION_PROJECTILE', '弹射物保护'], ['THORNS', '荆棘'],
        ['OXYGEN', '水下呼吸'], ['WATER_WORKER', '水下速掘'],
        ['ARROW_DAMAGE', '力量'], ['ARROW_KNOCKBACK', '冲击'], ['ARROW_FIRE', '火矢'], ['ARROW_INFINITE', '无限']
      ]
    }
  },
  computed: {
    firstChar() {
      return this.draft.material ? this.draft.material.charAt(0) : '?'
    },
    displayName() {
      if (this.draft.name) return this.draft.name
      return this.prettyMaterial(this.draft.material)
    },
    iconMatrix() { return null }
  },
  watch: {
    value(val) {
      this.visible = val
      if (val) this.initDraft()
    },
    visible(val) {
      this.$emit('input', val)
    }
  },
  methods: {
    initDraft() {
      const init = this.initial || {}
      this.draft = {
        material: init.material || 'DIAMOND',
        amount: init.amount || 1,
        data: init.data || 0,
        name: init.name || '',
        lore: init.lore ? init.lore.slice() : [],
        enchants: init.enchants ? init.enchants.slice() : []
      }
      this.loreText = (this.draft.lore || []).join('\n')
    },
    addEnchant() {
      this.draft.enchants.push('DAMAGE_ALL:1')
    },
    removeEnchant(i) {
      this.draft.enchants.splice(i, 1)
    },
    updateEnchantLevel(i, level) {
      const name = this.draft.enchants[i].split(':')[0]
      this.$set(this.draft.enchants, i, name + ':' + level)
    },
    prettyMaterial(mat) {
      if (!mat) return ''
      return mat.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
    },
    formatEnchant(e) {
      const parts = e.split(':')
      const names = {}
      this.enchants.forEach(en => { names[en[0]] = en[1] })
      const roman = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']
      return (names[parts[0]] || parts[0]) + ' ' + (roman[parseInt(parts[1], 10)] || parts[1] || 'I')
    },
    handleSave() {
      if (!this.draft.material) {
        this.$message.error('请填写物品材质')
        return
      }
      const lore = this.loreText.split('\n')
      const item = { material: this.draft.material.toUpperCase() }
      if (parseInt(this.draft.amount, 10) > 1) item.amount = parseInt(this.draft.amount, 10)
      if (parseInt(this.draft.data, 10) > 0) item.data = parseInt(this.draft.data, 10)
      if (this.draft.name) item.name = this.draft.name
      if (lore.length && lore.join('').trim()) item.lore = lore
      if (this.draft.enchants.length) item.enchants = this.draft.enchants
      this.$emit('save', item)
      this.visible = false
    },
    handleDelete() {
      this.$emit('delete')
      this.visible = false
    }
  }
}
</script>

<style scoped>
.item-editor { display: flex; gap: 20px; }
.ie-preview {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #1a1a2e;
  border-radius: 8px;
  padding: 20px;
}
.ie-big-item {
  width: 120px;
  height: 120px;
  background: #8b8b8b;
  border-radius: 6px;
  border-top: 4px solid #373737;
  border-left: 4px solid #373737;
  border-right: 4px solid #fff;
  border-bottom: 4px solid #fff;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.ie-icon { width: 80px; height: 80px; display: flex; align-items: center; justify-content: center; }
.ie-ph {
  font-family: "Courier New", monospace;
  font-weight: 800;
  font-size: 40px;
  color: #5a5a5a;
  text-shadow: 2px 2px 0 #c6c6c6;
}
.ie-glint {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 30%, rgba(180,140,255,0.4) 50%, transparent 70%);
  background-size: 200% 200%;
  animation: ieGlint 2.5s linear infinite;
  pointer-events: none;
  opacity: 0;
}
.ie-big-item.enchanted .ie-glint { opacity: 1; }
@keyframes ieGlint {
  0% { background-position: -100% -100%; }
  100% { background-position: 200% 200%; }
}
.ie-tooltip {
  margin-top: 16px;
  background: rgba(16, 4, 32, 0.94);
  border: 2px solid #2a0a50;
  border-radius: 4px;
  padding: 8px 12px;
  font-family: "Courier New", monospace;
  font-size: 12px;
  color: #aaa;
  max-width: 200px;
  width: 100%;
}
.ie-tt-name { font-weight: 800; margin-bottom: 4px; }
.ie-tt-lore { color: #a9a9a9; }
.ie-tt-extra { color: #754fd0; margin-top: 4px; }
.ie-form { flex: 1; min-width: 0; }
.ie-ench-list { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.ie-ench-row { display: flex; gap: 6px; align-items: center; }
</style>
