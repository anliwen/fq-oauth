<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { oauthApi } from '../api/oauth'
import { platformApi } from '../api/platforms'
import { notify } from '../composables/toast'
import { can } from '../composables/auth'
import Panel from '../components/Panel.vue'
import Modal from '../components/Modal.vue'
import Pager from '../components/Pager.vue'
import BaseSelect from '../components/BaseSelect.vue'

const rows = ref([]), total = ref(0), loading = ref(false), createdSecret = ref(''), platforms = ref([])
const dlg = reactive({ open: false, saving: false, error: '', editingId: '' })
const form = reactive({ platformId: '', name: '', clientType: 'PUBLIC', clientKind: 'APPLICATION', resource: '', redirectUris: 'http://localhost:8090/oauth/callback', scopes: 'mcp', canIntrospectApiKeys: false, skipConsent: false })
const filter = reactive({ keyword: '', clientKind: '', status: '' }), page = ref(1), pageSize = 10
let queryTimer
const canCreate = () => can('oauth-client:create')
const canUpdate = () => can('oauth-client:update')
const canDelete = () => can('oauth-client:delete')
async function load(targetPage = page.value) { loading.value = true; try { const r = await oauthApi.clients({ page: targetPage, pageSize, keyword: filter.keyword.trim(), clientKind: filter.clientKind, status: filter.status }); rows.value = r?.items || []; total.value = r?.total || 0; page.value = r?.page || targetPage } catch (e) { notify.err(e.message) } finally { loading.value = false } }
function query() { load(1) }
watch(() => [filter.keyword, filter.clientKind, filter.status], () => { clearTimeout(queryTimer); queryTimer = setTimeout(() => load(1), 300) })
onMounted(async () => { await Promise.all([load(), platformApi.list().then(v => { platforms.value = v || [] }).catch(e => notify.err(e.message))]) })
function open(c = null) { Object.assign(form, c ? { platformId: c.platformId || '', name: c.name, clientType: c.clientType, clientKind: c.clientKind || 'APPLICATION', resource: c.resource || '', redirectUris: (c.redirectUris || []).join('\n'), scopes: (c.scopes || []).join(' '), canIntrospectApiKeys: c.canIntrospectApiKeys, skipConsent: c.skipConsent } : { platformId: platforms.value[0]?.id || '', name: '', clientType: 'PUBLIC', clientKind: 'APPLICATION', resource: '', redirectUris: 'http://localhost:8090/oauth/callback', scopes: 'mcp', canIntrospectApiKeys: false, skipConsent: false }); dlg.editingId = c?.clientId || ''; dlg.error = ''; dlg.open = true }
async function save() { dlg.error = ''; const redirectUris = form.redirectUris.split('\n').map(x => x.trim()).filter(Boolean), scopes = form.scopes.split(/[ ,]+/).map(x => x.trim()).filter(Boolean), callbackRequired = form.clientKind !== 'APPLICATION' || form.clientType !== 'CONFIDENTIAL'; if (!form.platformId || !form.name.trim() || (callbackRequired && !redirectUris.length) || !scopes.length) return (dlg.error = '请选择平台并填写名称、权限范围及必需的回调地址'); if (form.clientKind === 'SERVICE' && !form.resource.trim()) return (dlg.error = '服务客户端必须填写 resource'); dlg.saving = true; try { const body = { platformId: Number(form.platformId), name: form.name.trim(), redirectUris, scopes, resource: form.resource.trim() || null, canIntrospectApiKeys: form.canIntrospectApiKeys, skipConsent: form.clientKind === 'APPLICATION' && form.skipConsent }; if (dlg.editingId) await oauthApi.updateClient(dlg.editingId, body); else { const c = await oauthApi.createClient({ ...body, clientType: form.clientType, clientKind: form.clientKind }); createdSecret.value = c.clientSecret || '' } dlg.open = false; await load() } catch (e) { dlg.error = e.message } finally { dlg.saving = false } }
async function toggle(c) { try { await oauthApi.updateClientStatus(c.clientId, c.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'); await load() } catch (e) { notify.err(e.message) } }
async function remove(c) { if (!window.confirm(`确认删除客户端“${c.name}”吗？`)) return; try { await oauthApi.deleteClient(c.clientId); await load() } catch (e) { notify.err(e.message) } }
function formatDate(value) { return value ? String(value).split('T')[0] : '-' }
async function copy(text) { try { await navigator.clipboard.writeText(text); notify.ok('已复制') } catch { notify.err('复制失败，请手动选择') } }
</script>
<template>
  <Panel title="服务客户端" subtitle="登记外部服务并绑定已登记的平台"><template #actions><button v-if="canCreate()" class="btn primary" @click="open">＋ 新建客户端</button></template>
    <div class="toolbar"><input v-model="filter.keyword" class="input search-input" placeholder="搜索服务名称 / resource / 客户端标识" @keyup.enter="query" /><BaseSelect v-model="filter.clientKind" class="filter-select" :options="[{value:'',label:'全部主体类型'},{value:'SERVICE',label:'服务自身身份'},{value:'APPLICATION',label:'终端应用'}]" /><BaseSelect v-model="filter.status" class="filter-select" :options="[{value:'',label:'全部状态'},{value:'ACTIVE',label:'有效'},{value:'DISABLED',label:'已停用'}]" /><button class="btn" @click="query">查询</button></div>
    <div class="table-wrap"><table class="tbl clients-table"><thead><tr><th>服务名称</th><th>所属平台</th><th>主体类型</th><th>resource</th><th>客户端类型</th><th>客户端标识</th><th>回调地址</th><th>权限范围</th><th>状态</th><th v-if="canUpdate() || canDelete()">操作</th></tr></thead><tbody><tr v-for="c in rows" :key="c.clientId"><td class="nowrap">{{ c.name }}</td><td class="nowrap">{{ c.platformName || '-' }}</td><td class="nowrap">{{ c.clientKind === 'SERVICE' ? '服务自身身份' : '终端应用' }}</td><td class="mono nowrap">{{ c.resource || '-' }}</td><td class="nowrap">{{ c.clientType === 'PUBLIC' ? '公共 · PKCE' : '机密 · 密钥' }}</td><td class="mono client-id-cell" :title="c.clientId">{{ c.clientId }}</td><td class="mono redirect-cell" :title="(c.redirectUris || []).join(', ')">{{ (c.redirectUris || []).join(', ') }}</td><td class="nowrap">{{ (c.scopes || []).join(' ') }}</td><td class="nowrap">{{ c.status === 'ACTIVE' ? '有效' : '已停用' }}</td><td v-if="canUpdate() || canDelete()"><div class="ops"><button v-if="canUpdate()" class="btn sm" @click="open(c)">编辑</button><button v-if="canUpdate()" class="btn sm" :class="c.status === 'ACTIVE' ? 'danger' : 'primary'" @click="toggle(c)">{{ c.status === 'ACTIVE' ? '禁用' : '启用' }}</button><button v-if="canDelete()" class="btn sm danger" @click="remove(c)">删除</button></div></td></tr></tbody></table></div>
    <Pager v-if="total" :page="page" :size="pageSize" :total="total" @change="load" /><div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : '暂无符合条件的数据' }}</div>
  </Panel>
  <Modal v-if="dlg.open" title="登记服务客户端" width="520px" @close="dlg.open=false">
    <form id="oauth-client-form" @submit.prevent="save">
      <div class="field"><label>所属平台</label><BaseSelect v-model="form.platformId" :options="platforms.map(p => ({ value: p.id, label: `${p.name} · ${p.code}` }))" placeholder="请选择已登记的平台" /></div>
      <div class="field"><label>服务名称</label><input v-model="form.name" class="input" maxlength="128" /></div>
      <div class="field"><label>主体类型</label><BaseSelect v-model="form.clientKind" :options="[{ value: 'SERVICE', label: '服务自身身份（MCP Server）' }, { value: 'APPLICATION', label: '终端应用（用户授权登录）' }]" /></div>
      <div v-if="form.clientKind === 'SERVICE'" class="field"><label>resource</label><input v-model="form.resource" class="input mono" maxlength="128" /></div>
      <div class="field"><label>客户端类型</label><BaseSelect v-model="form.clientType" :options="[{ value: 'PUBLIC', label: '公共客户端（PKCE）' }, { value: 'CONFIDENTIAL', label: '机密客户端（密钥）' }]" /></div>
      <div class="field">
        <label>回调地址<span v-if="form.clientKind !== 'APPLICATION' || form.clientType !== 'CONFIDENTIAL'" class="req">*</span></label>
        <textarea v-model="form.redirectUris" class="input area" rows="3" :placeholder="form.clientKind === 'APPLICATION' && form.clientType === 'CONFIDENTIAL' ? '直接接口登录无需填写' : '每行填写一个精确回调地址'" />
        <small v-if="form.clientKind === 'APPLICATION' && form.clientType === 'CONFIDENTIAL'" class="muted">直接接口登录可留空；如使用浏览器授权码流程，仍需登记回调地址</small>
      </div>
      <div class="field"><label>允许的权限范围</label><input v-model="form.scopes" class="input mono" placeholder="mcp" /></div>
      <label v-if="form.clientType === 'CONFIDENTIAL'" class="permission-option"><input v-model="form.canIntrospectApiKeys" type="checkbox" /><span>允许校验用户 API Key</span></label>
      <label v-if="form.clientKind === 'APPLICATION'" class="permission-option"><input v-model="form.skipConsent" type="checkbox" /><span>可信内部应用：登录后跳过同意页</span></label>
      <div v-if="dlg.error" class="err-line">⚠ {{ dlg.error }}</div>
    </form>
    <template #footer><button class="btn" @click="dlg.open=false">取消</button><button class="btn primary" type="submit" form="oauth-client-form" :disabled="dlg.saving">保存</button></template>
  </Modal>
  <Modal v-if="createdSecret" title="客户端密钥只显示一次" width="520px" @close="createdSecret=''" ><p class="muted">立即安全保存此密钥。</p><div class="secret mono">{{ createdSecret }}</div><template #footer><button class="btn" @click="copy(createdSecret)">复制密钥</button><button class="btn primary" @click="createdSecret=''">已保存</button></template></Modal>
</template>
<style scoped>.toolbar{display:flex;gap:10px;align-items:center;overflow-x:auto}.toolbar .search-input{flex:0 1 360px;width:360px;min-width:240px}.filter-select{flex:0 0 180px}.table-wrap{overflow-x:auto}.clients-table{min-width:1380px}.clients-table th,.nowrap{white-space:nowrap}.client-id-cell,.redirect-cell{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.client-id-cell{max-width:145px}.redirect-cell{max-width:230px}.ops{display:flex;gap:6px;white-space:nowrap}.area{resize:vertical}.permission-option{display:flex;gap:10px;margin:16px 0}.secret{padding:12px;background:#f3f7f4;overflow-wrap:anywhere;user-select:all}</style>
