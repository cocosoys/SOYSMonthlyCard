<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="组关键字" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="组 ID / 显示名" clearable size="small" style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd">新增组</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="groupList">
      <el-table-column label="组ID" align="center" prop="id" width="120" />
      <el-table-column label="显示名" align="center" prop="display" />
      <el-table-column label="前缀" align="center" prop="prefix" width="120" />
      <el-table-column label="权重" align="center" prop="weight" width="80" />
      <el-table-column label="描述" align="center" prop="description" show-overflow-tooltip />
      <el-table-column label="成员数" align="center" prop="memberCount" width="80" />
      <el-table-column label="权限数" align="center" prop="permCount" width="80" />
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-lock" @click="openPerm(scope.row)">权限管理</el-button>
          <el-button size="mini" type="text" icon="el-icon-user" @click="openMember(scope.row)">成员</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click="handleRemove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- ============ 新增 / 编辑组 ============ -->
    <el-dialog :title="form.id ? '编辑权限组' : '新增权限组'" :visible.sync="formOpen" width="480px">
      <el-form ref="groupForm" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="组ID" prop="id">
          <el-input v-model="form.id" :disabled="!!form.originId" placeholder="小写字母/数字，创建后不可修改" />
        </el-form-item>
        <el-form-item label="显示名" prop="display">
          <el-input v-model="form.display" placeholder="如 VIP 会员" />
        </el-form-item>
        <el-form-item label="聊天前缀" prop="prefix">
          <el-input v-model="form.prefix" placeholder="如 [VIP]" />
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number v-model="form.weight" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="formOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- ============ 权限管理（含组间批量复制） ============ -->
    <el-dialog :title="'权限管理 · ' + (currentGroup && (currentGroup.display || currentGroup.id))" :visible.sync="permOpen" width="680px">
      <el-divider content-position="left">当前组权限</el-divider>
      <el-form :inline="true" style="margin-bottom:10px">
        <el-form-item label="权限节点">
          <el-input v-model="permInput" placeholder="如 soyshttp.api.*；'-' 前缀 = 否定" clearable size="small" style="width: 360px" @keyup.enter.native="submitPermAdd" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="submitPermAdd">添加</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="permList" v-loading="permLoading" size="mini" max-height="240">
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

      <el-divider content-position="left">组间批量复制 / 移除</el-divider>
      <el-alert type="info" :closable="false" title="勾选一个或多个其他权限组：复制加入 = 将其全部权限节点复制到本组（跳过已存在，保留否定标记）；按此移除 = 删除本组中与选中组相同的节点（不影响源组）。" style="margin-bottom: 10px" />
      <el-checkbox-group v-model="sourceGroupIds" style="margin-bottom: 12px">
        <el-checkbox v-for="g in otherGroups" :key="g.id" :label="g.id">{{ g.display || g.id }}（{{ g.id }}）</el-checkbox>
      </el-checkbox-group>
      <div>
        <el-button type="success" icon="el-icon-plus" size="small" :disabled="!sourceGroupIds.length" :loading="copyLoading" @click="submitCopy">复制加入</el-button>
        <el-button type="danger" icon="el-icon-minus" size="small" :disabled="!sourceGroupIds.length" :loading="copyLoading" @click="submitRemoveBy">按此移除</el-button>
      </div>
    </el-dialog>

    <!-- ============ 成员管理 ============ -->
    <el-dialog :title="'成员管理 · ' + (currentGroup && (currentGroup.display || currentGroup.id))" :visible.sync="memberOpen" width="520px">
      <el-form :inline="true" style="margin-bottom:10px">
        <el-form-item label="玩家名">
          <el-input v-model="memberPlayer" placeholder="输入玩家名加入该组" clearable size="small" style="width: 240px" @keyup.enter.native="submitMemberAdd" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="submitMemberAdd">添加</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="memberList" v-loading="memberLoading" size="mini" max-height="320">
        <el-table-column label="玩家名" align="center" prop="player" />
        <el-table-column label="UUID" align="center" prop="uuid" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="80">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-remove-outline" style="color:#f56c6c" @click="submitMemberRemove(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listGroup, createGroup, updateGroup, removeGroup, listGroupPerms, addGroupPerm, removeGroupPerm, copyGroupPerms, removeByGroupPerms, listGroupMembers, addGroupMember, removeGroupMember } from '@/api/erp'

export default {
  name: 'ErpGroup',
  data() {
    return {
      loading: true,
      showSearch: true,
      groupList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, keyword: undefined },
      // 表单
      formOpen: false,
      form: {},
      formRules: {
        id: [{ required: true, message: '组ID不能为空', trigger: 'blur' }],
        display: [{ required: true, message: '显示名不能为空', trigger: 'blur' }]
      },
      // 权限
      permOpen: false,
      permLoading: false,
      permInput: '',
      permList: [],
      currentGroup: null,
      otherGroups: [],
      sourceGroupIds: [],
      copyLoading: false,
      // 成员
      memberOpen: false,
      memberLoading: false,
      memberList: [],
      memberPlayer: ''
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listGroup(this.queryParams).then(res => {
        this.groupList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    openAdd() {
      this.form = { id: '', display: '', prefix: '', weight: 0, description: '', originId: '' }
      this.formOpen = true
      this.$nextTick(() => { this.$refs.groupForm && this.$refs.groupForm.clearValidate() })
    },
    openEdit(row) {
      this.form = { id: row.id, display: row.display, prefix: row.prefix, weight: row.weight, description: row.description, originId: row.id }
      this.formOpen = true
      this.$nextTick(() => { this.$refs.groupForm && this.$refs.groupForm.clearValidate() })
    },
    submitForm() {
      this.$refs.groupForm.validate(valid => {
        if (!valid) return
        const payload = { id: this.form.id, display: this.form.display, prefix: this.form.prefix, weight: this.form.weight, description: this.form.description }
        if (this.form.originId) {
          updateGroup(payload).then(() => {
            this.$modal.msgSuccess('已更新')
            this.formOpen = false
            this.getList()
          })
        } else {
          createGroup(payload).then(() => {
            this.$modal.msgSuccess('已创建')
            this.formOpen = false
            this.getList()
          })
        }
      })
    },
    handleRemove(row) {
      this.$confirm('确定删除权限组 [' + row.id + '] 吗？将同时清理其权限节点与成员引用，不可恢复。', '危险操作', { type: 'warning' }).then(() => {
        removeGroup(row.id).then(() => {
          this.$modal.msgSuccess('已删除')
          this.getList()
        })
      }).catch(() => {})
    },
    /** 权限管理 */
    openPerm(row) {
      this.currentGroup = row
      this.permOpen = true
      this.permInput = ''
      this.sourceGroupIds = []
      this.loadGroupPerms()
      this.loadOtherGroups()
    },
    loadGroupPerms() {
      this.permLoading = true
      listGroupPerms(this.currentGroup.id).then(res => {
        this.permList = (res.data && res.data.rows) || (res.rows) || []
        this.permLoading = false
      }).catch(() => { this.permLoading = false })
    },
    loadOtherGroups() {
      listGroup({ pageNum: 1, pageSize: 999 }).then(res => {
        this.otherGroups = (res.rows || []).filter(g => g.id !== this.currentGroup.id)
      })
    },
    submitPermAdd() {
      if (!this.permInput) return this.$message.warning('请输入权限节点')
      addGroupPerm({ id: this.currentGroup.id, permission: this.permInput }).then(() => {
        this.$modal.msgSuccess('添加成功')
        this.permInput = ''
        this.loadGroupPerms()
      })
    },
    submitPermRemove(row) {
      this.$confirm('确定移除权限节点 ' + row.permission + ' 吗？', '提示', { type: 'warning' }).then(() => {
        removeGroupPerm({ id: this.currentGroup.id, permission: row.permission }).then(() => {
          this.$modal.msgSuccess('移除成功')
          this.loadGroupPerms()
        })
      }).catch(() => {})
    },
    submitCopy() {
      this.copyLoading = true
      copyGroupPerms({ targetGroup: this.currentGroup.id, sourceGroups: this.sourceGroupIds }).then(res => {
        this.copyLoading = false
        const r = res.data || {}
        this.$modal.msgSuccess('复制完成：新增 ' + (r.added != null ? r.added : '?') + '，跳过 ' + (r.skipped != null ? r.skipped : 0))
        this.loadGroupPerms()
      }).catch(() => { this.copyLoading = false })
    },
    submitRemoveBy() {
      this.copyLoading = true
      removeByGroupPerms({ targetGroup: this.currentGroup.id, sourceGroups: this.sourceGroupIds }).then(res => {
        this.copyLoading = false
        const r = res.data || {}
        this.$modal.msgSuccess('移除完成：共 ' + (r.removed != null ? r.removed : '?') + ' 个节点')
        this.loadGroupPerms()
      }).catch(() => { this.copyLoading = false })
    },
    /** 成员 */
    openMember(row) {
      this.currentGroup = row
      this.memberOpen = true
      this.memberPlayer = ''
      this.loadMembers()
    },
    loadMembers() {
      this.memberLoading = true
      listGroupMembers(this.currentGroup.id).then(res => {
        this.memberList = (res.data && res.data.rows) || (res.rows) || []
        this.memberLoading = false
      }).catch(() => { this.memberLoading = false })
    },
    submitMemberAdd() {
      if (!this.memberPlayer) return this.$message.warning('请输入玩家名')
      addGroupMember({ id: this.currentGroup.id, player: this.memberPlayer }).then(() => {
        this.$modal.msgSuccess('已加入')
        this.memberPlayer = ''
        this.loadMembers()
        this.getList()
      })
    },
    submitMemberRemove(row) {
      this.$confirm('确定将玩家 ' + (row.player || row.uuid) + ' 移出该组吗？', '提示', { type: 'warning' }).then(() => {
        removeGroupMember({ id: this.currentGroup.id, uuid: row.uuid }).then(() => {
          this.$modal.msgSuccess('已移除')
          this.loadMembers()
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>
