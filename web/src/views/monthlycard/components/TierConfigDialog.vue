<template>
  <el-dialog
    :title="'礼包配置 · ' + tierName"
    :visible.sync="visible"
    width="640px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form v-loading="loading" label-width="100px" size="small">
      <el-divider content-position="left">访问与状态</el-divider>
      <el-form-item label="权限节点">
        <el-input v-model="form.permission" placeholder="留空表示所有玩家可领" />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch v-model="form.enabled" active-text="启用该礼包档位（禁用后玩家无法领取，用户页也不展示）" />
      </el-form-item>

      <el-divider content-position="left">经济奖励</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="金币">
            <el-input-number v-model="form.money" :min="0" controls-position="right" style="width:100%" />
            <div class="form-tip">Vault 经济，0 表示不发放</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="点券">
            <el-input-number v-model="form.points" :min="0" controls-position="right" style="width:100%" />
            <div class="form-tip">PlayerPoints，0 表示不发放</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">领取时执行指令</el-divider>
      <el-form-item label="指令列表">
        <el-input type="textarea" v-model="commandsText" :rows="4" placeholder="每行一条，无需以 / 开头；可用 %player% 占位" />
        <div class="form-tip">物品奖励请在礼包列表中点击【礼品配置】进行可视化编辑。</div>
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getTier, updateTier } from '@/api/monthlycard'

export default {
  name: 'TierConfigDialog',
  props: {
    value: { type: Boolean, default: false },
    tierKey: { type: String, default: '' },
    tierName: { type: String, default: '' }
  },
  data() {
    return {
      visible: false,
      loading: false,
      saving: false,
      form: { permission: '', enabled: true, money: 0, points: 0 },
      commandsText: ''
    }
  },
  watch: {
    value(val) {
      this.visible = val
      if (val) this.loadConfig()
    },
    visible(val) {
      this.$emit('input', val)
    }
  },
  methods: {
    loadConfig() {
      this.loading = true
      getTier(this.tierKey).then(res => {
        this.loading = false
        if (res.code === 200 && res.data) {
          const d = res.data
          const rw = d.rewards || {}
          this.form = {
            permission: d.permission || '',
            enabled: d.enabled !== false,
            money: rw.money || 0,
            points: rw.points || 0
          }
          this.commandsText = (rw.commands || []).join('\n')
        }
      }).catch(() => {
        this.loading = false
      })
    },
    handleSave() {
      const commands = this.commandsText.split('\n').map(s => s.trim()).filter(Boolean)
      this.saving = true
      updateTier(this.tierKey, {
        permission: this.form.permission,
        enabled: this.form.enabled,
        money: this.form.money,
        points: this.form.points,
        commands: commands
      }).then(res => {
        this.saving = false
        if (res.code === 200) {
          this.$message.success(res.msg || '配置已保存')
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
.form-tip { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
