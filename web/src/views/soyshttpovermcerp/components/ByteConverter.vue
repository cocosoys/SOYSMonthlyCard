<template>
  <div class="byte-converter">
    <!-- 左：字节框（固定字节单位，主值，v-model 直连配置） -->
    <div class="bc-side">
      <el-input-number :value="value" :step="step" :min="0" size="small" style="width:160px" @input="onByteInput" />
      <span class="bc-unit">字节</span>
    </div>

    <!-- 中间：方向箭头图标（← / →）+ 转向按钮 -->
    <div class="bc-mid">
      <el-button type="text" class="bc-convert" :title="arrowLeft ? '把右侧数值按单位换算为字节填入左侧' : '把左侧字节换算为右侧数值'"
                 @click="convert">
        <i :class="arrowLeft ? 'el-icon-arrow-left' : 'el-icon-arrow-right'" />
      </el-button>
      <el-button type="text" class="bc-switch" title="调转方向" @click="arrowLeft = !arrowLeft">
        <i class="el-icon-refresh" />
      </el-button>
    </div>

    <!-- 右：数字 + 单位（随左侧字节值同步换算显示） -->
    <div class="bc-side">
      <el-input-number v-model="inputValue" :controls="false" :min="0" size="small" style="width:120px" />
      <el-select v-model="inputUnit" size="small" style="width:80px">
        <el-option v-for="u in units" :key="u" :label="u" :value="u" />
      </el-select>
    </div>
  </div>
</template>

<script>
/**
 * 字节转换组件（修正版）
 *  - 左侧固定为字节输入框（el-input-number，带 +/- 按钮），v-model 直连配置字段；
 *    +/- 按钮只按字节步进（step）改变数值，与右侧单位无关。
 *  - 右侧为“数字 + 单位”（B/KB/MB/GB/TB，1024 进制），始终跟随左侧字节值实时换算显示。
 *  - 中间方向图标默认向左（←）：点击执行“右侧 → 左侧”，把右侧数值按单位换算为字节填入左侧；
 *    下方转向按钮点击后图标方向变为向右（→）：表示“左侧 → 右侧”，把左侧字节换算为右侧数值显示。
 */
export default {
  name: 'ByteConverter',
  props: {
    value: { type: Number, default: 0 },
    step: { type: Number, default: 1048576 }
  },
  data() {
    return {
      arrowLeft: true,      // 转换方向：true=向左（右→左，默认）；false=向右（左→右）
      inputValue: 0,        // 右侧数字
      inputUnit: 'MB',      // 右侧单位
      units: ['B', 'KB', 'MB', 'GB', 'TB']
    }
  },
  watch: {
    // 左侧字节值变化（直接输入 / +/- 按钮 / 外部回填）→ 右侧实时同步换算显示
    value: {
      immediate: true,
      handler(v) {
        const s = this.smartUnit(v || 0)
        this.inputValue = s.v
        this.inputUnit = s.u
      }
    }
  },
  methods: {
    // 字节值 → 智能单位显示（>=1 的最大单位，保留 2 位小数）
    smartUnit(bytes) {
      const units = this.units
      if (!bytes || bytes <= 0) return { v: 0, u: 'B' }
      let u = 0
      let v = bytes
      while (v >= 1024 && u < units.length - 1) {
        v /= 1024
        u++
      }
      return { v: Math.round(v * 100) / 100, u: units[u] }
    },
    // 数字 + 单位 → 字节（取整）
    toBytes(v, u) {
      const idx = this.units.indexOf(u)
      if (idx < 0) return Math.round(v || 0)
      return Math.round((v || 0) * Math.pow(1024, idx))
    },
    // 点击方向箭头：按当前方向执行换算
    convert() {
      if (this.arrowLeft) {
        // 向左（默认）：右侧数值 × 单位 → 填入左侧字节框
        this.$emit('input', this.toBytes(this.inputValue, this.inputUnit))
      } else {
        // 向右：左侧字节 → 换算为右侧数值显示
        const s = this.smartUnit(this.value || 0)
        this.inputValue = s.v
        this.inputUnit = s.u
      }
    },
    // 左侧字节框直接编辑（+/- 按钮与直接输入都走这里）
    onByteInput(v) {
      this.$emit('input', v)
    }
  }
}
</script>

<style scoped>
.byte-converter {
  display: inline-flex;
  align-items: center;
}
.bc-side {
  display: inline-flex;
  align-items: center;
}
.bc-mid {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  margin: 0 4px;
}
.bc-convert {
  padding: 0 4px;
  font-size: 16px;
  line-height: 1.4;
}
.bc-switch {
  padding: 0 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #909399;
}
.bc-unit {
  margin-left: 4px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
</style>
