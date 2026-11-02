<script setup>
import { onMounted, ref, watch } from 'vue'
import { auditApi } from '../api/audit'
import { fmtTime } from '../composables/format'
import { notify } from '../composables/toast'
import Panel from '../components/Panel.vue'
import BaseSelect from '../components/BaseSelect.vue'
import Pager from '../components/Pager.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const action = ref('')
const result = ref('')
const page = ref(1)
const pageSize = 20
let queryTimer

const actionOptions = [
  { value: '', label: '全部操作' },
  { value: '登录', label: '登录' },
  { value: '退出登录', label: '退出登录' },
  { value: 'API Key 申请', label: 'API Key 申请' },
  { value: 'API Key 审批', label: 'API Key 审批' },
  { value: 'API Key 禁用', label: 'API Key 禁用' },
  { value: '服务客户端维护', label: '服务客户端维护' }
]
const resultOptions = [
  { value: '', label: '全部结果' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAILURE', label: '失败' }
]

async function loadPage(targetPage = 1) {
  loading.value = true
  try {
    const data = await auditApi.list({ page: targetPage, pageSize, keyword: keyword.value.trim(), action: action.value, result: result.value })
    rows.value = data?.items || []
    total.value = data?.total || 0
    page.value = data?.page || targetPage
  } catch (e) { notify.err(e.message) }
  finally { loading.value = false }
}
function query() { loadPage(1) }
function operator(row) { return row.operatorName || row.operatorUsername || '匿名用户' }
function resultLabel(value) { return value === 'SUCCESS' ? '成功' : '失败' }
watch(() => [keyword.value, action.value, result.value], () => {
  clearTimeout(queryTimer)
  queryTimer = setTimeout(() => loadPage(1), 300)
})
onMounted(() => loadPage())
</script>

<template>
  <Panel title="审计日志" subtitle="记录用户登录及所有管理写操作，支持按操作者、接口和结果追溯">
    <div class="toolbar">
      <input v-model="keyword" class="input search-input" placeholder="搜索操作者 / 操作 / 接口 / 目标 / IP" aria-label="搜索关键字" @keyup.enter="query" />
      <BaseSelect v-model="action" class="filter-select action-select" :options="actionOptions" aria-label="操作类型" />
      <BaseSelect v-model="result" class="filter-select result-select" :options="resultOptions" aria-label="操作结果" />
      <button class="btn" @click="query">查询</button>
    </div>
    <div class="table-wrap">
      <table class="tbl audit-table">
        <thead><tr><th>时间</th><th>操作者</th><th>操作</th><th>目标</th><th>接口</th><th>结果</th><th>状态码</th><th>IP 地址</th><th>客户端</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td class="mono muted nowrap">{{ fmtTime(row.createTime) }}</td>
            <td class="operator"><b>{{ operator(row) }}</b><small v-if="row.operatorUsername">{{ row.operatorUsername }}</small></td>
            <td class="nowrap">{{ row.action }}</td>
            <td class="mono target-cell" :title="row.target || ''">{{ row.target || '-' }}</td>
            <td class="mono path-cell" :title="row.path">{{ row.method }} {{ row.path }}</td>
            <td><span class="tag" :class="row.result === 'SUCCESS' ? 'ok' : 'bad'"><span class="dot" />{{ resultLabel(row.result) }}</span></td>
            <td class="mono">{{ row.statusCode }}</td>
            <td class="mono nowrap">{{ row.ip || '-' }}</td>
            <td class="user-agent" :title="row.userAgent || ''">{{ row.userAgent || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pager v-if="total" :page="page" :size="pageSize" :total="total" @change="loadPage" />
    <div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : '暂无符合条件的审计记录' }}</div>
  </Panel>
</template>

<style scoped>
.toolbar{flex-wrap:nowrap;align-items:center;overflow-x:auto}.toolbar .search-input{flex:0 1 380px;width:380px;min-width:260px;max-width:380px}.toolbar .filter-select{flex:0 0 auto;width:150px}.toolbar .action-select{width:170px}.toolbar .result-select{width:130px}.toolbar .btn{flex:0 0 auto}.table-wrap{overflow:auto}.audit-table{min-width:1180px}.audit-table th,.audit-table td{white-space:nowrap}.operator small{display:block;margin-top:3px;color:var(--text-dim);font-size:12px;font-weight:400}.target-cell{max-width:140px;overflow:hidden;text-overflow:ellipsis}.path-cell{max-width:270px;overflow:hidden;text-overflow:ellipsis}.user-agent{max-width:220px;overflow:hidden;text-overflow:ellipsis;color:var(--text-dim)}
</style>
