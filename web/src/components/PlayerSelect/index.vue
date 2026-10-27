<template>
  <el-select
    v-model="selected"
    filterable
    allow-create
    default-first-option
    :placeholder="placeholder"
    style="width:100%"
    @change="handleChange"
    @visible-change="handleVisible"
  >
    <el-option
      v-for="p in players"
      :key="p.name"
      :label="p.name + (p.op ? ' [OP]' : '')"
      :value="p.name"
    >
      <span>{{ p.name }}</span>
      <span v-if="p.op" style="float:right;color:#E6A23C;font-size:12px;">OP</span>
    </el-option>
  </el-select>
</template>

<script>
import { getOnlinePlayers } from '@/api/monthlycard'

export default {
  name: 'PlayerSelect',
  props: {
    value: { type: String, default: '' },
    placeholder: { type: String, default: '输入玩家名或从在线玩家选择' }
  },
  data() {
    return {
      selected: '',
      players: [],
      loaded: false
    }
  },
  watch: {
    value(val) { this.selected = val }
  },
  created() {
    this.selected = this.value
  },
  methods: {
    handleVisible(visible) {
      if (visible && !this.loaded) {
        this.loadPlayers()
      }
    },
    loadPlayers() {
      getOnlinePlayers().then(res => {
        if (res.code === 200) {
          this.players = res.data || []
          this.loaded = true
        }
      }).catch(() => {})
    },
    handleChange(val) {
      this.$emit('input', val)
      this.$emit('change', val)
    },
    refresh() {
      this.loaded = false
      this.loadPlayers()
    }
  }
}
</script>
