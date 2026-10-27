<template>
  <div class="mc-inventory">
    <div class="mc-inv-grid" :style="{ gridTemplateColumns: `repeat(${cols}, 52px)` }">
      <McItemSlot
        v-for="(item, idx) in displayItems"
        :key="idx"
        :item="item"
        @click="handleSlotClick(idx, item)"
      />
    </div>
  </div>
</template>

<script>
import McItemSlot from '@/components/McItemSlot/index.vue'

export default {
  name: 'McInventory',
  components: { McItemSlot },
  props: {
    items: { type: Array, default: () => [] },
    slots: { type: Number, default: 27 },
    cols: { type: Number, default: 9 }
  },
  computed: {
    displayItems() {
      const arr = this.items.slice(0, this.slots)
      while (arr.length < this.slots) arr.push(null)
      return arr
    }
  },
  methods: {
    handleSlotClick(idx, item) {
      this.$emit('slot-click', { index: idx, item: item })
    }
  }
}
</script>

<style scoped>
.mc-inventory { display: inline-block; }
.mc-inv-grid {
  display: grid;
  gap: 4px;
  background: #c6c6c6;
  padding: 8px;
  border-radius: 6px;
  border: 2px solid #373737;
}
</style>
