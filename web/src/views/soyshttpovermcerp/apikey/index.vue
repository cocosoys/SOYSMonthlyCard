<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="玩家名" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="玩家名 / 指纹 / 备注" clearable size="small" style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-key" size="mini" @click="openGenerate">生成 KEY</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="keyList">
      <el-table-column label="指纹" align="center" prop="fingerprint" width="120">
        <template slot-scope="scope"><code>{{ scope.row.fingerprint }}</code></template>
      </el-table-column>
      <el-table-column label="绑定玩家" align="center" width="140">
        <template slot-scope="scope">
          <span v-if="scope.row.player">{{ scope.row.player }}</span>
          <span v-else-if="scope.row.uuid" style="color:#64748b">{{ scope.row.uuid }}</span>
          <el-tag v-else type="info" size="mini">未绑定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-switch :value="scope.row.enabled" @change="val => handleToggle(scope.row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="过期时间" align="center" width="150">
        <template slot-scope="scope">
          <span v-if="!scope.row.expiry || scope.row.expiry === '0'" style="color:#15803d">永久</span>
          <span v-else>{{ parseTime(scope.row.expiry, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="使用次数" align="center" prop="usedCount" width="80" />
      <el-table-column label="创建时间" align="center" width="150">
        <template slot-scope="scope">
          <span v-if="scope.row.createTime">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-lock" @click="openPerm(scope.row)">权限</el-button>
          <el-button size="mini" type="text" icon="el-icon-link" @click="openBind(scope.row)">绑定</el-button>
          <el-button size="mini" type="text" icon="el-icon-time" @click="openExpiry(scope.row)">过期</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click="handleRemove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- ============ 生成 KEY ============ -->
    <el-dialog title="生成 X-API-KEY" :visible.sync="generateOpen" width="540px">
      <div v-if="!genResult">
        <el-form label-width="80px">
          <el-form-item label="备注">
            <el-input v-model="genRemark" placeholder="可选，如：用于某外部系统的密钥" maxlength="64" />
          </el-form-item>
        </el-form>
        <el-button type="warning" icon="el-icon-key" :loading="genLoading" @click="submitGenerate">生成</el-button>
      </div>
      <div v-else>
        <el-alert type="success" :closable="false" title="密钥已生成！明文仅展示一次，请立即复制保存。" style="margin-bottom: 12px" />
        <el-input v-model="genResult.plain" readonly>
          <template slot="prepend">密钥</template>
          <template slot="append">
            <el-button icon="el-icon-document-copy" @click="copyPlain"></el-button>
          </template>
        </el-input>
        <p style="font-size: 13px; color: #666; margin-top: 8px">指纹：<code>{{ genResult.fingerprint }}</code></p>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="generateOpen = false; genResult = null">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- ============ 权限管理 ============ -->
    <el-dialog :title="'权限管理 · ' + (currentKey && currentKey.fingerprint)" :visible.sync="permOpen" width="620px">
      <el-form :inline="true" style="margin-bottom:10px">
        <el-form-item label="权限节点">
          <el-input v-model="permInput" placeholder="如 soyshttp.api.*；'-' 前缀 = 否定" clearable size="small" style="width: 340px" @keyup.enter.native="submitPermAdd" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="submitPermAdd">添加</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="permList" v-loading="permLoading" size="mini" max-height="300">
        <el-table-column label="权限节点" align="left" prop="permission" show-overflow-tooltip />
        <el-table-column label="类型" align="center" width="80">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.negative" type="danger" size="mini">否定</el-tag>
            <el-tag v-else type="success" size="mini">允许</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="70">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-delete" @click="submitPermRemove(scope.row)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- ============ 绑定 / 解绑 ============ -->
    <el-dialog :title="'绑定 · ' + (currentKey && currentKey.fingerprint)" :visible.sync="bindOpen" width="460px">
      <div v-if="!currentKey || !currentKey.uuid">
        <el-form label-width="80px">
          <el-form-item label="玩家名">
            <el-input v-model="bindPlayer" placeholder="输入玩家名进行绑定" @keyup.enter.native="submitBind" />
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="bindLoading" @click="submitBind">绑定</el-button>
      </div>
      <div v-else>
        <el-alert type="info" :closable="false" :title="'当前绑定：' + (currentKey.player || currentKey.uuid)" style="margin-bottom: 12px" />
        <el-button type="danger" plain :loading="bindLoading" @click="submitUnbind">解除绑定</el-button>
      </div>
    </el-dialog>

    <!-- ============ 过期设置 ============ -->
    <el-dialog :title="'过期设置 · ' + (currentKey && currentKey.fingerprint)" :visible.sync="expiryOpen" width="460px">
      <el-form label-width="90px">
        <el-form-item label="过期时间">
          <el-date-picker v-model="expiryValue" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择过期时间" style="width: 100%" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="expiryForever">设为永久</el-checkbox>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="expiryLoading" @click="submitExpiry">保存</el-button>
        <el-button @click="expiryOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApiKey, generateApiKey, toggleApiKey, setApiKeyExpiry, bindApiKey, unbindApiKey, removeApiKey, listApiKeyPerms, addApiKeyPerm, removeApiKeyPerm } from '@/api/erp'

export default {
  name: 'ErpApiKey',
  data() {
    return {
      loading: true,
      showSearch: true,
      keyList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, keyword: undefined },
      // 生成
      generateOpen: false,
      genLoading: false,
      genRemark: '',
      genResult: null,
      // 权限
      permOpen: false,
      permLoading: false,
      permInput: '',
      permList: [],
      currentKey: null,
      // 绑定
      bindOpen: false,
      bindLoading: false,
      bindPlayer: '',
      // 过期
      expiryOpen: false,
      expiryLoading: false,
      expiryValue: '',
      expiryForever: false
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listApiKey(this.queryParams).then(res => {
        this.keyList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    isPermanent(expiry) { return !expiry || expiry === '0' || expiry === '' || expiry === null },
    /** 生成 */
    openGenerate() { this.genResult = null; this.genRemark = ''; this.generateOpen = true },
    submitGenerate() {
      this.genLoading = true
      generateApiKey({ remark: this.genRemark }).then(res => {
        this.genLoading = false
        this.genResult = res.data || {}
        this.getList()
      }).catch(() => { this.genLoading = false })
    },
    copyPlain() {
      const input = document.createElement('input')
      input.value = this.genResult.plain || ''
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$modal.msgSuccess('已复制到剪贴板')
    },
    /** 启停 */
    handleToggle(row, val) {
      toggleApiKey({ keyId: row.id, enabled: val }).then(() => {
        this.$modal.msgSuccess(val ? '已启用' : '已停用')
        this.getList()
      }).catch(() => { this.getList() })
    },
    /** 权限 */
    openPerm(row) {
      this.currentKey = row
      this.permOpen = true
      this.permInput = ''
      this.permLoading = true
      listApiKeyPerms(row.id).then(res => {
        this.permList = (res.data && res.data.rows) || (res.rows) || []
        this.permLoading = false
      }).catch(() => { this.permLoading = false })
    },
    submitPermAdd() {
      if (!this.permInput) return this.$message.warning('请输入权限节点')
      addApiKeyPerm({ keyId: this.currentKey.id, permission: this.permInput }).then(() => {
        this.$modal.msgSuccess('添加成功')
        this.permInput = ''
        listApiKeyPerms(this.currentKey.id).then(res => {
          this.permList = (res.data && res.data.rows) || (res.rows) || []
        })
      })
    },
    submitPermRemove(row) {
      this.$confirm('确定移除权限节点 ' + row.permission + ' 吗？', '提示', { type: 'warning' }).then(() => {
        removeApiKeyPerm({ keyId: this.currentKey.id, permission: row.permission }).then(() => {
          this.$modal.msgSuccess('移除成功')
          listApiKeyPerms(this.currentKey.id).then(res => {
            this.permList = (res.data && res.data.rows) || (res.rows) || []
          })
        })
      }).catch(() => {})
    },
    /** 绑定 */
    openBind(row) {
      this.currentKey = row
      this.bindPlayer = ''
      this.bindOpen = true
    },
    submitBind() {
      if (!this.bindPlayer) return this.$message.warning('请输入玩家名')
      this.bindLoading = true
      bindApiKey({ keyId: this.currentKey.id, player: this.bindPlayer }).then(() => {
        this.bindLoading = false
        this.$modal.msgSuccess('绑定成功')
        this.bindOpen = false
        this.getList()
      }).catch(() => { this.bindLoading = false })
    },
    submitUnbind() {
      this.$confirm('确定解除该 KEY 与玩家的绑定吗？', '提示', { type: 'warning' }).then(() => {
        this.bindLoading = true
        unbindApiKey({ keyId: this.currentKey.id }).then(() => {
          this.bindLoading = false
          this.$modal.msgSuccess('已解绑')
          this.bindOpen = false
          this.getList()
        }).catch(() => { this.bindLoading = false })
      }).catch(() => {})
    },
    /** 过期 */
    openExpiry(row) {
      this.currentKey = row
      this.expiryForever = this.isPermanent(row.expiry)
      this.expiryValue = this.expiryForever ? '' : this.parseTime(row.expiry, '{y}-{m}-{d} {h}:{i}')
      this.expiryOpen = true
    },
    submitExpiry() {
      this.expiryLoading = true
      let expiry = null
      if (!this.expiryForever) {
        if (!this.expiryValue) { this.expiryLoading = false; return this.$message.warning('请选择过期时间或勾选设为永久') }
        expiry = String(new Date(this.expiryValue.replace(/-/g, '/')).getTime())
      }
      setApiKeyExpiry({ keyId: this.currentKey.id, expiry }).then(() => {
        this.expiryLoading = false
        this.$modal.msgSuccess('已更新')
        this.expiryOpen = false
        this.getList()
      }).catch(() => { this.expiryLoading = false })
    },
    /** 删除 */
    handleRemove(row) {
      this.$confirm('确定删除指纹 ' + row.fingerprint + ' 的密钥吗？删除后将无法恢复。', '危险操作', { type: 'warning' }).then(() => {
        removeApiKey(row.id).then(() => {
          this.$modal.msgSuccess('已删除')
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>
