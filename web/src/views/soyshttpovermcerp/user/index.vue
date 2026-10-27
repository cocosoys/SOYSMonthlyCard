<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="玩家名" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="请输入玩家名 / UUID" clearable size="small" style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="玩家名" align="center" prop="player" />
      <el-table-column label="UUID" align="center" prop="uuid" width="180" show-overflow-tooltip />
      <el-table-column label="权限过期" align="center" width="150">
        <template slot-scope="scope">
          <span v-if="isPermanent(scope.row.expiry)" style="color:#15803d">永久</span>
          <span v-else>{{ parseTime(scope.row.expiry, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="150">
        <template slot-scope="scope">
          <span v-if="scope.row.createTime">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="150">
        <template slot-scope="scope">
          <span v-if="scope.row.updateTime">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="绑定KEY" align="center" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.hasKey" type="success" size="mini">✓ 已绑定</el-tag>
          <el-tag v-else type="info" size="mini">未绑定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="所属组" align="center" width="180">
        <template slot-scope="scope">
          <el-tag v-for="(g, i) in (scope.row.groupsDisplay && scope.row.groupsDisplay.length ? scope.row.groupsDisplay : scope.row.groups)" :key="i" size="mini" style="margin-right:4px">{{ g }}</el-tag>
          <span v-if="!scope.row.groups || !scope.row.groups.length" style="color:#909399">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" icon="el-icon-lock" @click="openPermDialog(scope.row)">权限</el-button>
          <el-button size="mini" type="success" icon="el-icon-user" @click="openGroupDialog(scope.row)">权限组</el-button>
          <el-button size="mini" type="warning" icon="el-icon-key" @click="openAssignKeyDialog(scope.row)">分配KEY</el-button>
          <el-button size="mini" type="info" icon="el-icon-time" @click="openExpiryDialog(scope.row)">延期</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- ============ 权限管理 ============ -->
    <el-dialog :title="'权限管理 · ' + (currentUser && currentUser.player)" :visible.sync="permOpen" width="640px">
      <el-form :inline="true" style="margin-bottom:10px">
        <el-form-item label="权限节点">
          <el-input v-model="permInput" placeholder="如 soyshttp.page.home；'-' 前缀 = 否定；':' 等同 '.'" clearable size="small" style="width: 360px" @keyup.enter.native="submitPermAdd" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="submitPermAdd">添加</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="permList" v-loading="permLoading" size="mini" max-height="360">
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

    <!-- ============ 权限组（多选） ============ -->
    <el-dialog :title="'权限组 · ' + (currentUser && currentUser.player)" :visible.sync="groupOpen" width="480px">
      <el-select v-model="userGroupIds" multiple placeholder="选择该用户所属的权限组" style="width: 100%">
        <el-option v-for="g in allGroups" :key="g.id" :label="(g.display || g.id) + ' (' + g.id + ')'" :value="g.id" />
      </el-select>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitUserGroups">保存</el-button>
        <el-button @click="groupOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- ============ 分配 X-API-KEY ============ -->
    <el-dialog :title="'分配 X-API-KEY · ' + (currentUser && currentUser.player)" :visible.sync="assignOpen" width="520px">
      <div v-if="!assignResult">
        <el-alert type="warning" :closable="false" title="点击生成后将为该用户生成一个新的 X-API-KEY 并立即绑定。" style="margin-bottom: 12px" />
        <el-button type="primary" icon="el-icon-key" :loading="assignLoading" @click="submitAssignKey">生成并绑定</el-button>
      </div>
      <div v-else>
        <el-alert type="success" :closable="false" title="密钥已生成并绑定！明文仅展示一次，请立即复制保存。" style="margin-bottom: 12px" />
        <el-input v-model="assignResult.plain" readonly>
          <template slot="prepend">密钥</template>
          <template slot="append">
            <el-button icon="el-icon-document-copy" @click="copyPlain"></el-button>
          </template>
        </el-input>
        <p style="font-size: 13px; color: #666; margin-top: 8px">指纹：<code>{{ assignResult.fingerprint }}</code>（此密钥无法再次查看明文）</p>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="assignOpen = false; assignResult = null">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- ============ 权限时间延期 ============ -->
    <el-dialog :title="'权限时间延期 · ' + (currentUser && currentUser.player)" :visible.sync="expiryOpen" width="480px">
      <el-form label-width="90px">
        <el-form-item label="过期时间">
          <el-date-picker v-model="expiryValue" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择权限过期时间" style="width: 100%" />
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
import { listUser, listUserGroups, setUserGroups, listUserPerms, addUserPerm, removeUserPerm, setUserExpiry, assignApiKey, listGroup } from '@/api/erp'

export default {
  name: 'ErpUser',
  data() {
    return {
      loading: true,
      showSearch: true,
      userList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, keyword: undefined },
      // 权限
      permOpen: false,
      permLoading: false,
      permInput: '',
      permList: [],
      // 权限组
      groupOpen: false,
      allGroups: [],
      userGroupIds: [],
      // 分配KEY
      assignOpen: false,
      assignLoading: false,
      assignResult: null,
      // 延期
      expiryOpen: false,
      expiryLoading: false,
      expiryValue: '',
      expiryForever: false,
      currentUser: null
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询用户列表 */
    getList() {
      this.loading = true
      listUser(this.queryParams).then(res => {
        this.userList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleSelectionChange() {},
    isPermanent(expiry) { return !expiry || expiry === '0' || expiry === '' || expiry === null },
    /** 权限弹窗 */
    openPermDialog(row) {
      this.currentUser = row
      this.permOpen = true
      this.permInput = ''
      this.loadPerms()
    },
    loadPerms() {
      this.permLoading = true
      listUserPerms(this.currentUser.uuid).then(res => {
        this.permList = (res.data && res.data.rows) || (res.rows) || []
        this.permLoading = false
      }).catch(() => { this.permLoading = false })
    },
    submitPermAdd() {
      if (!this.permInput) return this.$message.warning('请输入权限节点')
      addUserPerm({ uuid: this.currentUser.uuid, permission: this.permInput }).then(() => {
        this.$modal.msgSuccess('添加成功')
        this.permInput = ''
        this.loadPerms()
      })
    },
    submitPermRemove(row) {
      this.$confirm('确定移除权限节点 ' + row.permission + ' 吗？', '提示', { type: 'warning' }).then(() => {
        removeUserPerm({ uuid: this.currentUser.uuid, permission: row.permission }).then(() => {
          this.$modal.msgSuccess('移除成功')
          this.loadPerms()
        })
      }).catch(() => {})
    },
    /** 权限组弹窗 */
    openGroupDialog(row) {
      this.currentUser = row
      this.groupOpen = true
      this.userGroupIds = (row.groups || []).slice()
      listGroup({ pageNum: 1, pageSize: 999 }).then(res => {
        this.allGroups = res.rows || []
      })
    },
    submitUserGroups() {
      setUserGroups({ uuid: this.currentUser.uuid, groups: this.userGroupIds }).then(() => {
        this.$modal.msgSuccess('已保存')
        this.groupOpen = false
        this.getList()
      })
    },
    /** 分配 KEY */
    openAssignKeyDialog(row) {
      this.currentUser = row
      this.assignResult = null
      this.assignOpen = true
    },
    submitAssignKey() {
      this.assignLoading = true
      assignApiKey(this.currentUser.uuid).then(res => {
        this.assignLoading = false
        this.assignResult = res.data || {}
        this.$modal.msgSuccess('已生成并绑定')
        this.getList()
      }).catch(() => { this.assignLoading = false })
    },
    copyPlain() {
      const that = this
      const input = document.createElement('input')
      input.value = that.assignResult.plain || ''
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      that.$modal.msgSuccess('已复制到剪贴板')
    },
    /** 延期 */
    openExpiryDialog(row) {
      this.currentUser = row
      this.expiryOpen = true
      this.expiryForever = this.isPermanent(row.expiry)
      this.expiryValue = this.expiryForever ? '' : this.parseTime(row.expiry, '{y}-{m}-{d} {h}:{i}')
    },
    submitExpiry() {
      this.expiryLoading = true
      let expiry = null
      if (!this.expiryForever) {
        if (!this.expiryValue) { this.expiryLoading = false; return this.$message.warning('请选择过期时间或勾选设为永久') }
        expiry = String(new Date(this.expiryValue.replace(/-/g, '/')).getTime())
      }
      setUserExpiry({ uuid: this.currentUser.uuid, expiry }).then(() => {
        this.expiryLoading = false
        this.$modal.msgSuccess('已更新')
        this.expiryOpen = false
        this.getList()
      }).catch(() => { this.expiryLoading = false })
    }
  }
}
</script>
