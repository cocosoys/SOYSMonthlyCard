<template>
  <el-dialog
    :title="'礼品配置 · ' + tierName"
    :visible.sync="visible"
    width="860px"
    :close-on-click-modal="false"
    append-to-body
    custom-class="gift-config-dialog"
  >
    <div class="gift-config">
      <!-- 工具栏 -->
      <div class="gc-tools">
        <el-button type="warning" icon="el-icon-download" :loading="copyLoading" @click="loadInventory">
          读取我的背包并拷贝
        </el-button>
        <span class="gc-hint">点击空格添加物品，点击已有物品进行编辑</span>
      </div>

      <!-- 背包拷贝区 -->
      <div v-if="copyAreaVisible" class="gc-copy-area">
        <div class="gc-copy-title">
          背包玩家：<b style="color:#409EFF">{{ inventoryData.operator }}</b> · 点击物品追加到礼品列表
        </div>
        <div class="mc-inv-grid mc-copy-grid">
          <McItemSlot
            v-for="(item, idx) in inventoryItems"
            :key="'inv-' + idx"
            :item="item"
            @click="appendFromInventory(item)"
          />
        </div>
      </div>

      <!-- 物品栏网格 -->
      <div class="gc-inventory-wrap">
        <div class="mc-inv-grid">
          <McItemSlot
            v-for="(item, idx) in displayItems"
            :key="'slot-' + idx"
            :item="item"
            @click="handleSlotClick(idx, item)"
          />
        </div>
      </div>
    </div>

    <!-- 物品编辑器 -->
    <ItemEditor
      v-model="editorVisible"
      :initial="editorInitial"
      :show-delete="editorIndex !== -1"
      @save="handleEditorSave"
      @delete="handleEditorDelete"
    />

    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="warning" :loading="saving" @click="handleSave">保存礼品配置</el-button>
    </div>
  </el-dialog>
</template>

<script>
import McItemSlot from '@/components/McItemSlot/index.vue'
import ItemEditor from '@/components/ItemEditor/index.vue'
import { getTier, updateTier, getAdminInventory } from '@/api/monthlycard'

export default {
  name: 'GiftConfigDialog',
  components: { McItemSlot, ItemEditor },
  props: {
    value: { type: Boolean, default: false },
    tierKey: { type: String, default: '' },
    tierName: { type: String, default: '' }
  },
  data() {
    return {
      visible: false,
      working: [],
      saving: false,
      copyLoading: false,
      copyAreaVisible: false,
      inventoryData: { operator: '', items: [] },
      editorVisible: false,
      editorInitial: {},
      editorIndex: -1
    }
  },
  computed: {
    displayItems() {
      const arr = this.working.slice(0, 27)
      while (arr.length < 27) arr.push(null)
      return arr
    },
    inventoryItems() {
      const arr = (this.inventoryData.items || []).slice(0, 36)
      while (arr.length < 36) arr.push(null)
      return arr
    }
  },
  watch: {
    value(val) {
      this.visible = val
      if (val) this.loadItems()
    },
    visible(val) {
      this.$emit('input', val)
    }
  },
  methods: {
    loadItems() {
      this.working = []
      this.copyAreaVisible = false
      getTier(this.tierKey).then(res => {
        if (res.code === 200 && res.data) {
          const items = ((res.data.rewards || {}).items || []).map(this.cloneItem)
          this.working = items
        }
      }).catch(() => {})
    },
    cloneItem(it) {
      const c = { material: it.material }
      if (it.amount != null) c.amount = it.amount
      if (it.data != null) c.data = it.data
      if (it.name) c.name = it.name
      if (it.lore && it.lore.length) c.lore = it.lore.slice()
      if (it.enchants && it.enchants.length) c.enchants = it.enchants.slice()
      return c
    },
    handleSlotClick(idx, item) {
      if (item) {
        this.editorIndex = idx
        this.editorInitial = this.cloneItem(item)
      } else {
        this.editorIndex = -1
        this.editorInitial = { material: 'DIAMOND', amount: 1 }
      }
      this.editorVisible = true
    },
    handleEditorSave(item) {
      if (this.editorIndex === -1) {
        this.working.push(item)
      } else {
        this.$set(this.working, this.editorIndex, item)
      }
      this.$message.success('物品已更新')
    },
    handleEditorDelete() {
      if (this.editorIndex !== -1) {
        this.working.splice(this.editorIndex, 1)
        this.$message.success('物品已删除')
      }
    },
    loadInventory() {
      this.copyLoading = true
      getAdminInventory().then(res => {
        this.copyLoading = false
        if (res.code === 200 && res.data) {
          this.inventoryData = res.data
          this.copyAreaVisible = true
        } else {
          this.$message.error(res.msg || '读取背包失败（需管理员在线）')
        }
      }).catch(() => {
        this.copyLoading = false
      })
    },
    appendFromInventory(item) {
      if (!item) return
      this.working.push(this.cloneItem(item))
      this.$message.success('已追加 ' + (item.name || item.material))
    },
    handleSave() {
      const items = this.working.filter(Boolean)
      this.saving = true
      updateTier(this.tierKey, { items: items }).then(res => {
        this.saving = false
        if (res.code === 200) {
          this.$message.success(res.msg || '礼品配置已保存')
          this.$emit('saved')
          this.visible = false
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      }).catch(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.gift-config { display: flex; flex-direction: column; gap: 14px; }
.gc-tools { display: flex; align-items: center; gap: 14px; }
.gc-hint { color: #909399; font-size: 13px; }
.gc-copy-area {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
}
.gc-copy-title { font-size: 13px; color: #606266; margin-bottom: 10px; }
.mc-inv-grid {
  display: grid;
  grid-template-columns: repeat(9, 52px);
  gap: 4px;
  background: #c6c6c6;
  padding: 8px;
  border-radius: 6px;
  border: 2px solid #373737;
  width: fit-content;
}
.mc-copy-grid { grid-template-columns: repeat(9, 52px); }
.gc-inventory-wrap { display: flex; justify-content: center; }
</style>
