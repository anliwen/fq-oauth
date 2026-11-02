<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { agentApi } from '../api/agents'
import { loadPlatforms, lookups } from '../composables/lookups'
import { fmtTime } from '../composables/format'
import { can } from '../composables/auth'
import { notify } from '../composables/toast'
import Modal from '../components/Modal.vue'
import Panel from '../components/Panel.vue'
import StatCard from '../components/StatCard.vue'

const rows = ref([])
const loading = ref(false)
const dlg = reactive({ open: false, saving: false, error: '' })
const form = reactive({ platformId: '', tenantId: '', code: '', name: '' })
const pending = ref(null)
const changing = ref(false)
const platformOptions = computed(() => lookups.platforms.map(p => ({ value: p.id, label: `${p.name}（${p.code}）` })))
const canCreate = computed(() => can('agent:create'))
const canUpdate = computed(() => can('agent:update'))
const canDelete = computed(() => can('agent:delete'))

async function load() {
  loading.value = true
  try { await Promise.all([loadPlatforms(), agentApi.list().then(v => { rows.value = v || [] })]) }
  catch (e) { notify.err(e.message) } finally { loading.value = false }
}
onMounted(load)

function openCreate() {
  Object.assign(form, { platformId: platformOptions.value[0]?.value || '', tenantId: '', code: '', name: '' })
  Object.assign(dlg, { open: true, saving: false, error: '' })
}
async function save() {
  dlg.error = ''
  if (!form.platformId || !form.tenantId.trim() || !form.code.trim() || !form.name.trim()) return (dlg.error = '请填写所有必填项')
  if (!/^[a-zA-Z0-9][a-zA-Z0-9_-]*$/.test(form.code.trim())) return (dlg.error = 'Agent 编码只能包含字母、数字、下划线和连字符')
  dlg.saving = true
  try { await agentApi.create({ platformId: form.platformId, tenantId: form.tenantId.trim(), code: form.code.trim(), name: form.name.trim() }); notify.ok('Agent 已登记'); dlg.open = false; await load() }
  catch (e) { dlg.error = e.message } finally { dlg.saving = false }
}
async function changeStatus() {
  changing.value = true
  try { await (pending.value.action === 'revoke' ? agentApi.revoke(pending.value.id) : agentApi.suspend(pending.value.id)); notify.ok(pending.value.action === 'revoke' ? 'Agent 已撤销' : 'Agent 已暂停'); pending.value = null; await load() }
  catch (e) { notify.err(e.message); pending.value = null } finally { changing.value = false }
}
</script>

<template>
  <div class="stats">
    <StatCard label="我的 Agent" :value="rows.length" hint="已登记的外部 Agent" />
    <StatCard label="可用 Agent" :value="rows.filter(a => a.status === 'ACTIVE').length" hint="当前可以接受委托" tone="violet" />
  </div>
  <Panel title="Agent 管理" subtitle="登记接入平台中的 Agent，并控制其生命周期">
    <template #actions><button class="btn" :disabled="loading" @click="load">↻ 刷新</button><button v-if="canCreate" class="btn primary" @click="openCreate">＋ 登记 Agent</button></template>
    <table class="tbl"><thead><tr><th>名称</th><th>编码</th><th>平台</th><th>租户</th><th>状态</th><th>创建时间</th><th v-if="canUpdate || canDelete" class="ops-h">操作</th></tr></thead>
      <tbody><tr v-for="a in rows" :key="a.id"><td class="pname">{{ a.name }}</td><td><span class="tag violet mono">{{ a.code }}</span></td><td>{{ lookups.platforms.find(p => p.id === a.platformId)?.name || a.platformId }}</td><td class="mono">{{ a.tenantId }}</td><td><span class="tag" :class="a.status === 'ACTIVE' ? 'ok' : a.status === 'REVOKED' ? 'bad' : 'off'"><span class="dot"></span>{{ { ACTIVE: '有效', SUSPENDED: '已暂停', REVOKED: '已撤销' }[a.status] || '未知状态' }}</span></td><td class="mono muted">{{ fmtTime(a.createTime) }}</td><td v-if="canUpdate || canDelete"><div class="ops"><button v-if="canUpdate && a.status === 'ACTIVE'" class="btn sm" @click="pending = { ...a, action: 'suspend' }">暂停</button><button v-if="canDelete && a.status !== 'REVOKED'" class="btn sm danger" @click="pending = { ...a, action: 'revoke' }">撤销</button></div></td></tr></tbody>
    </table><div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : '暂无数据' }}</div>
  </Panel>
  <Modal v-if="dlg.open" title="登记 Agent" width="480px" @close="dlg.open = false"><form id="agent-form" novalidate @submit.prevent="save">
    <div class="field"><label>所属平台<span class="req">*</span></label><select v-model="form.platformId" class="select"><option value="" disabled>请选择平台</option><option v-for="p in platformOptions" :key="p.value" :value="p.value">{{ p.label }}</option></select></div>
    <div class="field"><label>租户标识<span class="req">*</span></label><input v-model="form.tenantId" class="input mono" maxlength="64" placeholder="tenant-001" /></div>
    <div class="field"><label>Agent 编码<span class="req">*</span></label><input v-model="form.code" class="input mono" maxlength="64" placeholder="finance-assistant" /></div>
    <div class="field"><label>名称<span class="req">*</span></label><input v-model="form.name" class="input" maxlength="128" placeholder="财务助手" /></div>
    <div v-if="dlg.error" class="err-line" role="alert">⚠ {{ dlg.error }}</div>
  </form><template #footer><button class="btn" @click="dlg.open = false">取消</button><button class="btn primary" type="submit" form="agent-form" :disabled="dlg.saving">{{ dlg.saving ? '登记中…' : '登记' }}</button></template></Modal>
  <Modal v-if="pending" :title="pending.action === 'revoke' ? '确认撤销' : '确认暂停'" width="400px" @close="pending = null"><p class="lead">确定{{ pending.action === 'revoke' ? '撤销' : '暂停' }} Agent <b>{{ pending.name }}</b> 吗？</p><p class="muted">已有委托不会被隐藏，撤销或暂停后新的授权检查将无法通过。</p><template #footer><button class="btn" @click="pending = null">取消</button><button class="btn danger" :disabled="changing" @click="changeStatus">确认</button></template></Modal>
</template>

<style scoped>.stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 20px; }.pname { font-weight: 600; }.lead { line-height: 1.8; margin-bottom: 8px; }@media (max-width: 900px) { .stats { grid-template-columns: 1fr; } }</style>
