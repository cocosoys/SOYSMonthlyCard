<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <div slot="header" style="display:flex;align-items:center;gap:12px">
        <span style="font-weight:600">语言管理</span>
        <el-tag size="mini">{{ currentFile }}</el-tag>
        <span style="flex:1"></span>
        <el-select v-model="currentFile" placeholder="选择语言文件" style="width:180px" @change="loadEntries">
          <el-option v-for="f in langFiles" :key="f.file" :label="f.code" :value="f.file" />
        </el-select>
        <el-input v-model="keyword" placeholder="按 key 或 value 模糊搜索" style="width:260px" clearable />
        <el-button size="mini" icon="el-icon-plus" @click="addRow">新增条目</el-button>
        <el-button size="mini" type="primary" icon="el-icon-check" :loading="saving" @click="save">保存</el-button>
      </div>

      <el-table :data="filteredRows" size="small" border stripe height="calc(100vh - 230px)">
        <el-table-column label="#" type="index" width="50" />
        <el-table-column label="键 (key)" width="320">
          <template slot-scope="scope">
            <el-input v-model="scope.row.key" size="mini" :disabled="!scope.row.isNew" placeholder="如 ajax.auth.login-success" />
          </template>
        </el-table-column>
        <el-table-column label="值 (value)">
          <template slot-scope="scope">
            <el-input v-model="scope.row.value" size="mini" type="textarea" :autosize="{ minRows: 1, maxRows: 3 }" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template slot-scope="scope">
            <el-button size="mini" type="text" style="color:#f56c6c" icon="el-icon-delete" @click="removeRow(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listLangFiles, loadLangEntries, saveLangEntries } from '@/api/erp'

export default {
  name: 'ErpLang',
  data() {
    return {
      loading: false,
      saving: false,
      langFiles: [],
      currentFile: '',
      keyword: '',
      rows: [] // [{ key, value, isNew }]
    }
  },
  computed: {
    filteredRows() {
      if (!this.keyword) return this.rows
      const k = this.keyword.toLowerCase()
      return this.rows.filter(r =>
        (r.key && r.key.toLowerCase().includes(k)) ||
        (r.value && String(r.value).toLowerCase().includes(k))
      )
    }
  },
  created() {
    this.init()
  },
  methods: {
    async init() {
      this.loading = true
      try {
        const res = await listLangFiles()
        this.langFiles = res.data || []
        if (this.langFiles.length) {
          this.currentFile = this.langFiles[0].file
          await this.loadEntries()
        }
      } finally {
        this.loading = false
      }
    },
    async loadEntries() {
      if (!this.currentFile) return
      this.loading = true
      try {
        const res = await loadLangEntries(this.currentFile)
        const map = res.data || {}
        this.rows = Object.keys(map).sort().map(k => ({ key: k, value: map[k], isNew: false }))
      } finally {
        this.loading = false
      }
    },
    addRow() {
      this.rows.push({ key: '', value: '', isNew: true })
    },
    removeRow(idx) {
      this.rows.splice(idx, 1)
    },
    async save() {
      // 校验 key 非空
      for (const r of this.rows) {
        if (!r.key || !r.key.trim()) {
          this.$message.error('存在空键，请填写或删除')
          return
        }
      }
      const entries = {}
      this.rows.forEach(r => { entries[r.key.trim()] = r.value || '' })
      this.saving = true
      try {
        const res = await saveLangEntries(this.currentFile, entries)
        this.$message.success(res.data && res.data.tip || '已保存')
        await this.loadEntries()
      } finally {
        this.saving = false
      }
    }
  }
}
</script>
