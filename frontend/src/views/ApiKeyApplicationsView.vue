<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { apiKeyApplicationsApi } from '../api/credentials'
import { fmtTime } from '../composables/format'
import { notify } from '../composables/toast'
import Modal from '../components/Modal.vue'
import Panel from '../components/Panel.vue'
import BaseSelect from '../components/BaseSelect.vue'
import Pager from '../components/Pager.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const filter = ref('PENDING')
const reviewItem = ref(null)
const approved = ref(false)
const comment = ref('')
const error = ref('')
const filterKeyword = ref('')
const page = ref(1), pageSize = 10
let queryTimer
const pendingCount = computed(() => rows.value.filter(row => row.status === 'PENDING').length)
const visibleRows = computed(() => rows.value)
const statusLabel = status => ({ PENDING: '待审核', APPROVED: '审核通过，待生成', ACTIVE: '已生效', REJECTED: '已拒绝', REVOKED: '已撤销' }[status] || '未知状态')
const filterOptions = computed(() => [
  { value: 'PENDING', label: `待审核（${pendingCount.value}）` },
  { value: 'APPROVED', label: '审核通过，待生成' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'ALL', label: '全部' }
])

async function load() {
  return loadPage(page.value)
}
async function loadPage(targetPage = 1) {
  loading.value = true
  try {
    const result = await apiKeyApplicationsApi.list({ page: targetPage, pageSize, keyword: filterKeyword.value.trim(), status: filter.value === 'ALL' ? '' : filter.value })
    rows.value = result?.items || []
    total.value = result?.total || 0
    page.value = result?.page || targetPage
  }
  catch (e) { notify.err(e.message) }
  finally { loading.value = false }
}

function openReview(row, decision) {
  reviewItem.value = row
  approved.value = decision
  comment.value = ''
  error.value = ''
}
function query() { loadPage(1) }
watch(() => [filterKeyword.value, filter.value], () => { clearTimeout(queryTimer); queryTimer = setTimeout(() => loadPage(1), 300) })

async function submitReview() {
  if (!approved.value && !comment.value.trim()) {
    error.value = '拒绝申请时请填写原因'
    return
  }
  saving.value = true
  error.value = ''
  try {
    await apiKeyApplicationsApi.review(reviewItem.value.id, { approved: approved.value, comment: comment.value.trim() || null })
    notify.ok(approved.value ? '申请已通过' : '申请已拒绝')
    reviewItem.value = null
    await load()
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <Panel title="API Key 审核" subtitle="处理客户端的 API Key 申请，审核通过后才能生成密钥">
    <div class="toolbar"><input v-model="filterKeyword" class="input search-input" placeholder="搜索申请人 / 账号 / 邮箱 / Key 名称 / 目标服务" aria-label="搜索关键字" @keyup.enter="query" /><BaseSelect v-model="filter" class="filter-select" :options="filterOptions" aria-label="申请状态" /><button class="btn" @click="query">查询</button></div>
    <div class="table-wrap">
      <table class="tbl">
        <thead><tr><th>申请人</th><th>账号</th><th>邮箱</th><th>Key 名称</th><th>目标服务</th><th>申请用途</th><th>申请时间</th><th>状态</th><th>审核意见</th><th class="ops-h">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in visibleRows" :key="row.id">
            <td class="name">{{ row.fullName || row.username }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.email || '-' }}</td>
            <td>{{ row.name }}</td>
            <td class="mono">{{ row.audience }}</td>
            <td class="reason-cell">{{ row.reason || '未填写' }}</td>
            <td class="mono muted">{{ fmtTime(row.createTime) }}</td>
            <td><span class="tag" :class="row.status === 'ACTIVE' ? 'ok' : row.status === 'PENDING' || row.status === 'APPROVED' ? 'pending' : 'off'"><span class="dot" />{{ statusLabel(row.status) }}</span></td>
            <td class="review-cell"><span>{{ row.reviewComment || '-' }}</span><small v-if="row.reviewerName" class="sub">审核人：{{ row.reviewerName }}</small></td>
            <td class="ops-cell"><div class="ops"><template v-if="row.status === 'PENDING'"><button class="btn sm primary" @click="openReview(row, true)">通过</button><button class="btn sm danger" @click="openReview(row, false)">拒绝</button></template><span v-else class="muted">-</span></div></td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pager v-if="total" :page="page" :size="pageSize" :total="total" @change="loadPage" />
    <div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : '暂无符合条件的申请' }}</div>
  </Panel>

  <Modal v-if="reviewItem" :title="approved ? '通过 API Key 申请' : '拒绝 API Key 申请'" width="460px" @close="reviewItem = null">
    <p class="summary"><b>{{ reviewItem.fullName || reviewItem.username }}</b> 申请了 <b>{{ reviewItem.name }}</b></p>
    <div class="field"><label>{{ approved ? '审核意见（选填）' : '拒绝原因' }}<span v-if="!approved" class="req">*</span></label><textarea v-model="comment" class="input comment" maxlength="500" rows="4" :placeholder="approved ? '可填写补充说明' : '请说明拒绝原因'" /></div>
    <div v-if="error" class="err-line" role="alert">{{ error }}</div>
    <template #footer><button class="btn" @click="reviewItem = null">取消</button><button class="btn" :class="approved ? 'primary' : 'danger'" :disabled="saving" @click="submitReview">{{ saving ? '提交中…' : approved ? '确认通过' : '确认拒绝' }}</button></template>
  </Modal>
</template>

<style scoped>
.toolbar{flex-wrap:nowrap;align-items:center;overflow-x:auto}.toolbar .search-input{flex:0 1 420px;width:420px;min-width:260px;max-width:420px}.toolbar .filter-select{flex:0 0 180px}.toolbar .btn{flex:0 0 auto}.filter{width:auto;min-width:150px}.table-wrap{overflow:auto}.tbl .ops-h{text-align:left}.ops-cell{text-align:left}.name{font-weight:600}.sub{display:block;margin-top:4px;color:var(--text-dim);font-size:12px;font-weight:400}.reason-cell{min-width:160px;max-width:260px;white-space:normal;line-height:1.5}.review-cell{min-width:160px;max-width:240px;white-space:normal;line-height:1.5}.tag.pending{color:#8a5b00;background:#fff7db}.summary{line-height:1.8}.comment{resize:vertical}@media(max-width:800px){.tbl{min-width:1100px}}
</style>
