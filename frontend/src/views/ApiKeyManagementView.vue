<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { apiKeyApplicationsApi } from '../api/credentials'
import { fmtTime } from '../composables/format'
import { notify } from '../composables/toast'
import Panel from '../components/Panel.vue'
import BaseSelect from '../components/BaseSelect.vue'
import Pager from '../components/Pager.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const filter = ref('ACTIVE')
const filterKeyword = ref('')
const page = ref(1), pageSize = 10
let queryTimer
const statusLabel = status => ({ ACTIVE: '已生效', REVOKED: '已禁用' }[status] || status)
const filterOptions = [
  { value: 'ACTIVE', label: '已生效' },
  { value: 'REVOKED', label: '已禁用' },
  { value: 'ALL', label: '全部密钥' }
]
const visibleRows = computed(() => rows.value)

async function load() {
  return loadPage(page.value)
}
async function loadPage(targetPage = 1) {
  loading.value = true
  try {
    const result = await apiKeyApplicationsApi.list({ page: targetPage, pageSize, keyword: filterKeyword.value.trim(), status: filter.value === 'ALL' ? 'ISSUED' : filter.value })
    rows.value = result?.items || []
    total.value = result?.total || 0
    page.value = result?.page || targetPage
  }
  catch (e) { notify.err(e.message) }
  finally { loading.value = false }
}

async function revoke(row) {
  if (!window.confirm(`确认禁用 API Key“${row.name}”吗？`)) return
  try {
    await apiKeyApplicationsApi.revoke(row.id)
    notify.ok('API Key 已禁用')
    await load()
  } catch (e) { notify.err(e.message) }
}

onMounted(load)
function query() { loadPage(1) }
watch(() => [filterKeyword.value, filter.value], () => { clearTimeout(queryTimer); queryTimer = setTimeout(() => loadPage(1), 300) })
</script>

<template>
  <Panel title="API Key 管理" subtitle="查看已生成的 API Key 使用情况，并控制密钥状态">
    <div class="toolbar">
      <input v-model="filterKeyword" class="input search-input" placeholder="搜索申请人 / Key 名称 / 目标服务 / Key 前缀" aria-label="搜索关键字" @keyup.enter="query" />
      <BaseSelect v-model="filter" class="filter-select" :options="filterOptions" aria-label="密钥状态" />
      <button class="btn" @click="query">查询</button>
    </div>
    <div class="table-wrap">
      <table class="tbl">
        <thead><tr><th>申请人</th><th>账号</th><th>邮箱</th><th>Key 名称</th><th>目标服务</th><th>Key 前缀</th><th>校验成功次数</th><th>最近使用时间</th><th>状态</th><th class="ops-h">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in visibleRows" :key="row.id">
            <td class="name">{{ row.fullName || row.username }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.email || '-' }}</td>
            <td>{{ row.name }}</td>
            <td class="mono">{{ row.audience }}</td>
            <td class="mono">{{ row.prefix ? `${row.prefix}…` : '-' }}</td>
            <td class="mono">{{ row.validationCount || 0 }}</td>
            <td class="mono muted">{{ row.lastUsedAt ? fmtTime(row.lastUsedAt) : '从未使用' }}</td>
            <td><span class="tag" :class="row.status === 'ACTIVE' ? 'ok' : 'off'"><span class="dot" />{{ statusLabel(row.status) }}</span></td>
            <td class="ops-cell"><div class="ops"><button v-if="row.status === 'ACTIVE'" class="btn sm danger" @click="revoke(row)">禁用</button><span v-else class="muted">-</span></div></td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pager v-if="total" :page="page" :size="pageSize" :total="total" @change="loadPage" />
    <div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : '暂无符合条件的 API Key' }}</div>
  </Panel>
</template>

<style scoped>
.toolbar{flex-wrap:nowrap;align-items:center;overflow-x:auto}.toolbar .search-input{flex:0 1 420px;width:420px;min-width:260px;max-width:420px}.toolbar .filter-select{flex:0 0 170px;width:170px}.toolbar .btn{flex:0 0 auto}.table-wrap{overflow:auto}.tbl .ops-h{text-align:left}.ops-cell{text-align:left}.name{font-weight:600}.sub{display:block;margin-top:4px;color:var(--text-dim);font-size:12px;font-weight:400}.tag.off{color:#64748b;background:#f1f5f9}.ops{white-space:nowrap}@media(max-width:800px){.tbl{min-width:1100px}}
</style>
