<template>
  <el-dialog
    :title="dialogTitle"
    :visible.sync="visible"
    width="480px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form label-width="80px" size="default">
      <el-form-item label="目标玩家" required>
        <PlayerSelect v-model="playerName" ref="playerSelect" />
      </el-form-item>
      <el-alert
        :title="hintText"
        type="info"
        :closable="false"
        show-icon
        style="margin-top:8px"
      />
    </el-form>
    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button :type="mode === 'claim' ? 'success' : 'danger'" :loading="submitting" @click="handleSubmit">
        {{ mode === 'claim' ? '确认领取' : '确认重置' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import PlayerSelect from '@/components/PlayerSelect/index.vue'
import { claimTier, resetTier } from '@/api/monthlycard'

export default {
  name: 'PlayerActionDialog',
  components: { PlayerSelect },
  props: {
    value: { type: Boolean, default: false },
    tierKey: { type: String, default: '' },
    tierName: { type: String, default: '' },
    mode: { type: String, default: 'claim' } // claim | reset
  },
  data() {
    return {
      visible: false,
      playerName: '',
      submitting: false
    }
  },
  computed: {
    dialogTitle() {
      return (this.mode === 'claim' ? '为玩家领取 · ' : '重置玩家领取 · ') + this.tierName
    },
    hintText() {
      return this.mode === 'claim'
        ? '代领要求目标玩家当前在线；领取动作在服务器主线程执行。'
        : '重置后该玩家本月可再次领取；离线玩家也可重置（按正版/离线 UUID 匹配）。'
    }
  },
  watch: {
    value(val) {
      this.visible = val
      if (val) {
        this.playerName = ''
        if (this.$refs.playerSelect) this.$refs.playerSelect.refresh()
      }
    },
    visible(val) {
      this.$emit('input', val)
    }
  },
  methods: {
    handleSubmit() {
      if (!this.playerName) {
        this.$message.warning('请填写玩家名')
        return
      }
      this.submitting = true
      const action = this.mode === 'claim' ? claimTier : resetTier
      action(this.tierKey, this.playerName).then(res => {
        this.submitting = false
        if (res.code === 200) {
          this.$message.success(res.msg || '操作成功')
          this.$emit('success')
          this.visible = false
        } else {
          this.$message.error(res.msg || '操作失败')
        }
      }).catch(() => {
        this.submitting = false
      })
    }
  }
}
</script>
