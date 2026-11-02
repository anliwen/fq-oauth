<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { credentialsApi } from '../api/credentials'
import { fmtTime } from '../composables/format'
import { notify } from '../composables/toast'
import Modal from '../components/Modal.vue'
import Panel from '../components/Panel.vue'

const keys = ref([]), tokens = ref([]), loading = ref(false), saving = ref(false)
const createDialog = reactive({ open: false, error: '' })
const revokeDialog = ref(null)
const form = reactive({ name: '', reason: '', audience: 'fintech-mcp-server' })
const revealedKey = ref('')
const activeKeys = computed(() => keys.value.filter(k => k.status === 'ACTIVE').length)
const keyStatusLabel = status => ({ PENDING: '待审核', APPROVED: '审核通过，待生成', ACTIVE: '使用中', REJECTED: '已拒绝', REVOKED: '已撤销' }[status] || '未知状态')
const tokenTypeLabel = type => ({ ACCESS: '访问令牌', REFRESH: '刷新令牌' }[type] || '其他令牌')

async function load() {
  loading.value = true
  try { [keys.value, tokens.value] = await Promise.all([credentialsApi.keys(), credentialsApi.tokens()]) }
  catch (e) { notify.err(e.message) } finally { loading.value = false }
}
onMounted(load)

function openCreate() { form.name = ''; form.reason = ''; form.audience = 'fintech-mcp-server'; createDialog.error = ''; createDialog.open = true }
async function createKey() {
  createDialog.error = ''
  if (!form.name.trim() || !form.reason.trim() || !form.audience.trim()) { createDialog.error = '请填写名称、用途和目标服务'; return }
  saving.value = true
  try { await credentialsApi.createKey({ name: form.name.trim(), reason: form.reason.trim(), audience: form.audience.trim() }); createDialog.open = false; notify.ok('申请已提交，等待管理员审核'); await load() }
  catch (e) { createDialog.error = e.message } finally { saving.value = false }
}
async function issueKey(key) {
  saving.value = true
  try { const issued = await credentialsApi.issueKey(key.id); revealedKey.value = issued.apiKey; await load() }
  catch (e) { notify.err(e.message) } finally { saving.value = false }
}
async function rotateKey(key) {
  saving.value = true
  try { const issued = await credentialsApi.rotateKey(key.id); revealedKey.value = issued.apiKey; await load() }
  catch (e) { notify.err(e.message) } finally { saving.value = false }
}
async function confirmRevoke() {
  const item = revokeDialog.value
  if (!item) return
  saving.value = true
  try {
    if (item.kind === 'key') await credentialsApi.revokeKey(item.id)
    else await credentialsApi.revokeToken(item.id)
    notify.ok(item.kind === 'key' ? 'API Key 已撤销' : '授权令牌已撤销')
    revokeDialog.value = null
    await load()
  } catch (e) { notify.err(e.message) } finally { saving.value = false }
}
async function copyKey() {
  try { await navigator.clipboard.writeText(revealedKey.value); notify.ok('API Key 已复制') }
  catch { notify.err('复制失败，请手动选择并复制') }
}
function tokenStatus(token) {
  if (token.revoked) return '已撤销'
  return new Date(token.expiresAt) <= new Date() ? '已过期' : '有效'
}
</script>

<template>
  <div class="stats">
    <div class="metric"><span>API Key</span><strong>{{ keys.length }}</strong><small>{{ activeKeys }} 个有效</small></div>
    <div class="metric"><span>服务授权令牌</span><strong>{{ tokens.filter(t => !t.revoked && new Date(t.expiresAt) > new Date()).length }}</strong><small>可随时撤销访问授权</small></div>
  </div>

  <Panel title="我的 API Key" subtitle="申请经管理员审核后，点击“生成密钥”查看完整 API Key；完整值仅显示一次">
    <template #actions><button class="btn" :disabled="loading" @click="load">↻ 刷新</button><button class="btn primary" @click="openCreate">＋ 申请 API Key</button></template>
    <div class="table-wrap"><table class="tbl"><thead><tr><th>名称</th><th>Key 前缀</th><th>目标服务</th><th>状态</th><th>创建时间</th><th>最近使用</th><th class="ops-h">操作</th></tr></thead>
      <tbody><tr v-for="key in keys" :key="key.id"><td class="name">{{ key.name }}<small v-if="key.status === 'PENDING'" class="review-note">申请已提交，管理员审核通过后可生成密钥</small><small v-if="key.status === 'APPROVED'" class="review-note">审核已通过，请点击“生成密钥”查看完整值</small><small v-if="key.reviewComment" class="review-note">审核意见：{{ key.reviewComment }}</small></td><td class="mono">{{ key.prefix ? `${key.prefix}…` : key.status === 'PENDING' ? '审核中' : '待生成' }}</td><td class="mono">{{ key.audience }}</td><td><span class="tag" :class="key.status === 'ACTIVE' ? 'ok' : key.status === 'REJECTED' || key.status === 'REVOKED' ? 'off' : 'pending'"><span class="dot" />{{ keyStatusLabel(key.status) }}</span></td><td class="mono muted">{{ fmtTime(key.createTime) }}</td><td class="mono muted">{{ fmtTime(key.lastUsedAt) }}</td><td><button v-if="key.status === 'APPROVED'" class="btn sm primary" :disabled="saving" @click="issueKey(key)">生成密钥</button><template v-else-if="key.status === 'ACTIVE'"><button class="btn sm" :disabled="saving" @click="rotateKey(key)">轮换</button><button class="btn sm danger" @click="revokeDialog = { id: key.id, name: key.name, kind: 'key' }">撤销</button></template></td></tr></tbody>
    </table></div>
    <div v-if="!keys.length" class="empty">{{ loading ? '正在加载…' : '尚未申请 API Key' }}</div>
  </Panel>

  <Panel class="token-panel" title="已授权服务" subtitle="查看并撤销服务代表你访问 API 的授权令牌">
    <div class="table-wrap"><table class="tbl"><thead><tr><th>服务</th><th>令牌类型</th><th>授权范围</th><th>过期时间</th><th>状态</th><th class="ops-h">操作</th></tr></thead>
      <tbody><tr v-for="token in tokens" :key="token.id"><td class="name">{{ token.clientName || token.clientId }}</td><td>{{ tokenTypeLabel(token.tokenType) }}</td><td class="mono">{{ token.scope }}</td><td class="mono muted">{{ fmtTime(token.expiresAt) }}</td><td><span class="tag" :class="tokenStatus(token) === '有效' ? 'ok' : 'off'"><span class="dot" />{{ tokenStatus(token) }}</span></td><td><button v-if="!token.revoked" class="btn sm danger" @click="revokeDialog = { id: token.id, name: token.clientName || token.clientId, kind: 'token' }">撤销</button></td></tr></tbody>
    </table></div>
    <div v-if="!tokens.length" class="empty">尚无服务授权令牌</div>
  </Panel>

  <Modal v-if="createDialog.open" title="申请 API Key" width="480px" @close="createDialog.open = false"><form id="api-key-form" @submit.prevent="createKey"><div class="field"><label>名称<span class="req">*</span></label><input v-model="form.name" class="input" maxlength="100" autocomplete="off" placeholder="例如：我的开发环境" /></div><div class="field"><label>目标服务<span class="req">*</span></label><input v-model="form.audience" class="input" maxlength="128" autocomplete="off" placeholder="例如：fintech-mcp-server" /></div><div class="field"><label>申请用途<span class="req">*</span></label><textarea v-model="form.reason" class="input reason" maxlength="500" rows="3" placeholder="说明接入场景" /></div><p class="notice">该密钥只能用于指定目标服务。审核通过后可签发或轮换；完整值仅展示一次。</p><div v-if="createDialog.error" class="err-line" role="alert">{{ createDialog.error }}</div></form><template #footer><button class="btn" @click="createDialog.open = false">取消</button><button class="btn primary" type="submit" form="api-key-form" :disabled="saving">{{ saving ? '提交中…' : '提交申请' }}</button></template></Modal>
  <Modal v-if="revealedKey" title="API Key 已生成" width="500px" @close="revealedKey = ''"><p class="notice">请现在复制并妥善保管。出于安全考虑，关闭后将无法再次查看完整密钥。</p><div class="secret mono">{{ revealedKey }}</div><template #footer><button class="btn" @click="copyKey">复制</button><button class="btn primary" @click="revealedKey = ''">我已保存</button></template></Modal>
  <Modal v-if="revokeDialog" title="确认撤销" width="400px" @close="revokeDialog = null"><p class="confirm-copy">确定撤销 <b>{{ revokeDialog.name }}</b> 的{{ revokeDialog.kind === 'key' ? ' API Key' : '服务授权令牌' }}吗？撤销后不能恢复。</p><template #footer><button class="btn" @click="revokeDialog = null">取消</button><button class="btn danger" :disabled="saving" @click="confirmRevoke">确认撤销</button></template></Modal>
</template>

<style scoped>
.stats{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-bottom:18px}.metric{display:grid;grid-template-columns:1fr auto;gap:4px 12px;padding:16px 18px;background:#fff;border:1px solid var(--line);border-radius:8px}.metric span{color:var(--text-dim);font-size:13px}.metric strong{grid-column:2;grid-row:1/3;font-size:26px;align-self:center}.metric small{color:var(--text-dim)}.token-panel{margin-top:18px}.table-wrap{overflow:auto}.name{font-weight:600}.review-note{display:block;margin-top:4px;color:var(--text-dim);font-weight:400}.notice{line-height:1.7;color:var(--text-dim)}.secret{padding:12px;background:#f3f7f4;border:1px solid var(--line);border-radius:6px;overflow-wrap:anywhere;user-select:all}.confirm-copy{line-height:1.8}.confirm-copy b{overflow-wrap:anywhere}.reason{resize:vertical}.tag.pending{color:#8a5b00;background:#fff7db}@media(max-width:700px){.stats{grid-template-columns:1fr}.tbl{min-width:760px}}
</style>
