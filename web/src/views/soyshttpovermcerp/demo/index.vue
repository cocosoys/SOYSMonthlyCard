<template>
  <!--
    ============================================================
    开发者模板页（DEMO）
    ============================================================
    用途：作为 MCERP 微服务前端「列表管理页」的标准模板。
    特点：
      1. 数据为本地 mock（开箱即跑，clone 后 npm run dev 即可看到完整效果）；
      2. 每个区块都标注了「范式要点」和「真实业务替换点」；
      3. 如需接入真实后端，把 getList / submitForm / handleDelete
         中的本地数据操作替换为 src/api/ 下的接口调用即可（见文末示例代码）。

    参照真实业务页：src/views/soyshttpovermcerp/user/index.vue（游戏用户列表，已接真实接口）。
    ============================================================
  -->
  <div class="app-container">

    <!-- ① 搜索栏：范式 = el-form inline + 查询/重置按钮 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称（支持模糊）"
          clearable
          size="small"
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable size="small" style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ② 工具栏：范式 = 新增按钮 + right-toolbar（控制搜索栏显隐 / 刷新） -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- ③ 数据表格：范式 = el-table + v-loading + 操作列 -->
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="名称" align="center" prop="name" show-overflow-tooltip />
      <el-table-column label="编码" align="center" prop="code" width="160" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- ④ 分页：范式 = 全局 Pagination 组件（已在 main.js 注册） -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- ⑤ 新增 / 编辑弹窗：范式 = el-dialog + el-form + 校验规则 -->
    <el-dialog :title="form.id ? '修改示例' : '新增示例'" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入编码（唯一）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
/**
 * ============================================================
 * 真实后端接入方式（替换下方 getList / submitForm / handleDelete 即可）：
 *
 * 1) 在 src/api/ 下新建业务接口文件（如 src/api/demo.js），统一走 @/utils/request：
 *
 *    import request from '@/utils/request'
 *    // 列表：路径与后端 Controller 类级前缀一一对应（如后端 /erp/demo/list）
 *    export function listDemo(params) {
 *      return request({ url: '/erp/demo/list', method: 'get', params })
 *    }
 *    export function addDemo(data) {
 *      return request({ url: '/erp/demo/add', method: 'post', data })
 *    }
 *    export function updateDemo(data) {
 *      return request({ url: '/erp/demo/update', method: 'post', data })
 *    }
 *    export function delDemo(id) {
 *      return request({ url: '/erp/demo/remove', method: 'post', data: { id } })
 *    }
 *
 *    // 说明：baseURL 由 SOYS 契约自动注入（生产=/api/plugins/SOYSHTTPOverMC-ERP，开发=/dev-api 代理），
 *    // 请求会自动携带 Authorization 头、做 401 弹窗与防重复提交，无需自己处理。
 *    // 注意：接口路径只写相对路径（/erp/demo/*），不要手拼 host:port 或 /api/plugins 前缀。
 *
 * 2) 页面内替换：
 *    getList()      → listDemo(this.queryParams).then(res => { this.list = res.rows; this.total = res.total })
 *    submitForm()   → addDemo(this.form) / updateDemo(this.form)
 *    handleDelete() → delDemo(row.id)
 * ============================================================
 */

// ---- 本地 mock 数据（演示用；接入真实接口后删除） ----
const MOCK = []
for (let i = 1; i <= 35; i++) {
  MOCK.push({
    id: i,
    name: '示例记录 ' + i,
    code: 'DEMO-' + String(i).padStart(4, '0'),
    status: i % 3 === 0 ? 0 : 1,
    createTime: new Date(Date.now() - i * 86400000).toISOString().slice(0, 19).replace('T', ' ')
  })
}
// ------------------------------------------------

export default {
  name: 'ErpDemo',
  data() {
    return {
      // 列表
      loading: true,
      showSearch: true,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, name: undefined, status: undefined },
      // 弹窗
      open: false,
      submitting: false,
      form: {},
      rules: {
        name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        code: [{ required: true, message: '编码不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询列表（本地 mock 分页过滤；接入真实接口后改为调用 api） */
    getList() {
      this.loading = true
      const { pageNum, pageSize, name, status } = this.queryParams
      let rows = MOCK.slice()
      if (name) rows = rows.filter(r => r.name.indexOf(name) >= 0)
      if (status !== undefined && status !== null && status !== '') rows = rows.filter(r => r.status === status)
      this.total = rows.length
      this.list = rows.slice((pageNum - 1) * pageSize, pageNum * pageSize)
      this.loading = false
    },
    /** 搜索 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置搜索 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 新增 */
    handleAdd() {
      this.form = { status: 1 }
      this.open = true
    },
    /** 修改 */
    handleUpdate(row) {
      this.form = Object.assign({}, row)
      this.open = true
    },
    /** 提交新增/修改 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        // ---- 接入真实接口时替换为：addDemo / updateDemo ----
        setTimeout(() => {
          if (this.form.id) {
            const idx = MOCK.findIndex(r => r.id === this.form.id)
            if (idx >= 0) MOCK.splice(idx, 1, Object.assign({}, this.form))
          } else {
            MOCK.unshift(Object.assign({ id: Date.now() }, this.form))
          }
          this.submitting = false
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        }, 200)
      })
    },
    /** 删除 */
    handleDelete(row) {
      this.$confirm('确认删除「' + row.name + '」吗？', '提示', { type: 'warning' }).then(() => {
        // ---- 接入真实接口时替换为：delDemo(row.id) ----
        const idx = MOCK.findIndex(r => r.id === row.id)
        if (idx >= 0) MOCK.splice(idx, 1)
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    /** 取消弹窗 */
    cancel() {
      this.open = false
    }
  }
}
</script>
