<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { permissionApi } from '../api/permissions'
import { roleApi } from '../api/roles'
import { can } from '../composables/auth'
import { lookups, loadPlatforms, loadRoles } from '../composables/lookups'
import { fmtTime } from '../composables/format'
import { TYPE_LABEL, descendantIds, flattenTree } from '../composables/permTree'
import { notify } from '../composables/toast'
import Modal from '../components/Modal.vue'
import BaseSelect from '../components/BaseSelect.vue'
import Panel from '../components/Panel.vue'
import StatCard from '../components/StatCard.vue'

const canCreate = computed(() => can('role:create'))
const canUpdate = computed(() => can('role:update'))
const canDelete = computed(() => can('role:delete'))
const canGrant = computed(() => can('role:grant'))
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    await loadRoles()
  } catch (e) {
    notify.err(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  await loadPlatforms().catch(e => notify.err(e.message))
  await load()
})

const platformFilter = ref('')
const filterOptions = computed(() => [
  { value: '', label: '全部平台' },
  ...lookups.platforms.map(p => ({ value: p.id, label: p.name }))
])
const platformOptions = computed(() =>
  lookups.platforms.map(p => ({ value: p.id, label: `${p.name}（${p.code}）` })))
const shown = computed(() =>
  platformFilter.value === '' ? lookups.roles : lookups.roles.filter(r => r.platformId === platformFilter.value))

const systemCount = computed(() => lookups.roles.filter(r => r.isSystem).length)
const assigned = computed(() => lookups.roles.reduce((n, r) => n + r.userCount, 0))

// ---------- create / edit ----------
const dlg = reactive({ open: false, editing: null, saving: false, error: '' })
const form = reactive({ platformId: '', code: '', name: '', desc: '' })

function openCreate() {
  Object.assign(form, { platformId: platformFilter.value, code: '', name: '', desc: '' })
  Object.assign(dlg, { open: true, editing: null, error: '' })
}
function openEdit(r) {
  Object.assign(form, { platformId: r.platformId, code: r.code, name: r.name, desc: r.desc || '' })
  Object.assign(dlg, { open: true, editing: r, error: '' })
}

async function save() {
  dlg.error = ''
  if (!form.name.trim()) return (dlg.error = '请填写角色名称')
  if (!dlg.editing && form.platformId === '') return (dlg.error = '请选择所属平台')
  if (!dlg.editing && !form.code.trim()) return (dlg.error = '请填写角色编码')
  dlg.saving = true
  try {
    if (dlg.editing) {
      await roleApi.update(dlg.editing.id, { name: form.name.trim(), desc: form.desc.trim() })
      notify.ok('角色已更新')
    } else {
      await roleApi.create({ platformId: form.platformId, code: form.code.trim(), name: form.name.trim(), desc: form.desc.trim() })
      notify.ok('角色已创建')
    }
    dlg.open = false
    await load()
  } catch (e) {
    dlg.error = e.message
  } finally {
    dlg.saving = false
  }
}

// ---------- grant permissions ----------
const gr = reactive({ open: false, role: null, tree: [], checked: new Set(), loading: false, saving: false, error: '' })
const grRows = computed(() => flattenTree(gr.tree))
// the centre's own superadmin role always holds every permission of the centre: there is nothing to assign
const alwaysAll = r => r.platformCode === 'fq-oauth' && r.code === 'superadmin'

async function openGrant(r) {
  Object.assign(gr, { open: true, role: r, tree: [], checked: new Set(), loading: true, error: '' })
  try {
    const [tree, ids] = await Promise.all([permissionApi.tree(r.platformId), permissionApi.granted(r.id)])
    gr.tree = tree
    gr.checked = new Set(ids)
  } catch (e) {
    gr.error = e.message
  } finally {
    gr.loading = false
  }
}

const isHalf = n => {
  if (gr.checked.has(n.id)) return false
  return descendantIds(n).some(id => gr.checked.has(id))
}
/** Ticking selects the node and everything below it; unticking clears them. Ancestors are added by the server. */
function toggleNode(n) {
  if (alwaysAll(gr.role) || !canGrant.value) return
  const next = new Set(gr.checked)
  const ids = [n.id, ...descendantIds(n)]
  if (next.has(n.id)) ids.forEach(id => next.delete(id))
  else ids.forEach(id => next.add(id))
  gr.checked = next
}
const grCount = computed(() => grRows.value.filter(n => gr.checked.has(n.id)).length)

async function saveGrant() {
  gr.saving = true
  gr.error = ''
  try {
    const saved = await permissionApi.grant(gr.role.id, [...gr.checked])
    gr.checked = new Set(saved)
    notify.ok('权限已保存，对该角色用户的下一次请求即生效')
    gr.open = false
  } catch (e) {
    gr.error = e.message
  } finally {
    gr.saving = false
  }
}

// ---------- delete ----------
const del = ref(null)
const deleting = ref(false)
async function confirmDelete() {
  deleting.value = true
  try {
    await roleApi.remove(del.value.id)
    notify.ok('角色已删除')
    del.value = null
    await load()
  } catch (e) {
    notify.err(e.message)
    del.value = null
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="stats">
    <StatCard label="角色总数" :value="lookups.roles.length" hint="系统内全部角色" />
    <StatCard label="内置角色" :value="systemCount" hint="不可删除" tone="amber" />
    <StatCard label="已分配" :value="assigned" hint="用户-角色关联数" tone="green" />
  </div>

  <Panel title="角色管理" subtitle="创建角色，并在用户管理里分配给用户">
    <template #actions>
      <button class="btn" :disabled="loading" @click="load">↻ 刷新</button>
      <button v-if="canCreate" class="btn primary" @click="openCreate">＋ 新建角色</button>
    </template>

    <div class="toolbar">
      <BaseSelect v-model="platformFilter" class="filter-select" :options="filterOptions" aria-label="平台" />
    </div>

    <table class="tbl">
      <thead>
        <tr>
          <th>所属平台</th><th>编码</th><th>名称</th><th>描述</th><th>用户数</th><th>创建时间</th>
          <th v-if="canUpdate || canDelete || canGrant" class="ops-h">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in shown" :key="r.id">
          <td>{{ r.platformName }}</td>
          <td><span class="tag violet">{{ r.code }}</span></td>
          <td class="rname">{{ r.name }} <span v-if="r.isSystem" class="tag">内置</span></td>
          <td class="desc">{{ r.desc || '—' }}</td>
          <td class="mono">{{ r.userCount }}</td>
          <td class="mono muted">{{ fmtTime(r.createTime) }}</td>
          <td v-if="canUpdate || canDelete || canGrant">
            <div class="ops">
              <button v-if="canGrant" class="btn sm" @click="openGrant(r)">分配权限</button>
              <button v-if="canUpdate" class="btn sm" @click="openEdit(r)">编辑</button>
              <button v-if="canDelete" class="btn sm danger" :disabled="r.isSystem" :title="r.isSystem ? '内置角色不可删除' : ''"
                      @click="del = r">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!shown.length" class="empty">{{ loading ? '正在加载…' : '暂无数据' }}</div>
  </Panel>

  <Modal v-if="dlg.open" :title="dlg.editing ? '编辑角色' : '新建角色'" width="460px" @close="dlg.open = false">
    <form id="role-form" novalidate @submit.prevent="save">
      <div class="field">
        <label for="r-platform">所属平台<span class="req">*</span></label>
        <BaseSelect id="r-platform" v-model="form.platformId" :options="platformOptions"
                    placeholder="请选择平台" :disabled="!!dlg.editing" />
        <small class="muted">{{ dlg.editing ? '角色所属平台创建后不可更换' : '一个角色只属于一个平台，创建后不可更换' }}</small>
      </div>
      <div class="field">
        <label for="r-code">角色编码<span class="req">*</span></label>
        <input id="r-code" v-model="form.code" class="input mono" maxlength="32" placeholder="如 compliance"
               :disabled="!!dlg.editing" />
        <small class="muted">{{ dlg.editing ? '编码创建后不可修改' : '同一平台内不能重复' }}</small>
      </div>
      <div class="field">
        <label for="r-name">角色名称<span class="req">*</span></label>
        <input id="r-name" v-model="form.name" class="input" maxlength="64" placeholder="如 合规专员" />
      </div>
      <div class="field">
        <label for="r-desc">描述</label>
        <textarea id="r-desc" v-model="form.desc" class="input" rows="3" maxlength="255"></textarea>
      </div>
      <div v-if="dlg.error" class="err-line" role="alert">⚠ {{ dlg.error }}</div>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="dlg.open = false">取消</button>
      <button class="btn primary" type="submit" form="role-form" :disabled="dlg.saving">
        {{ dlg.saving ? '保存中…' : '保存' }}
      </button>
    </template>
  </Modal>

  <Modal v-if="gr.open" :title="`分配权限 · ${gr.role.platformName} / ${gr.role.name}`" width="560px" @close="gr.open = false">
    <p v-if="alwaysAll(gr.role)" class="note">本中心的管理员角色始终拥有本平台的全部权限（包括以后新增的），无需也不能分配。</p>
    <p v-else class="note">一个角色只能获得它所属平台的权限。勾选按钮时，它所在的菜单和目录会自动一并授予。</p>
    <div v-if="gr.loading" class="empty">正在加载…</div>
    <ul v-else class="ptree">
      <li v-for="n in grRows" :key="n.id" class="prow" :style="{ paddingLeft: n.depth * 22 + 'px' }">
        <button type="button" class="pick" role="checkbox" :aria-checked="alwaysAll(gr.role) || gr.checked.has(n.id)"
                :disabled="alwaysAll(gr.role) || !canGrant" @click="toggleNode(n)">
          <span class="box" :class="{ on: alwaysAll(gr.role) || gr.checked.has(n.id), half: !alwaysAll(gr.role) && isHalf(n) }"></span>
          <span class="pname">{{ n.name }}</span>
          <span class="ptype">{{ TYPE_LABEL[n.type] }}</span>
          <span class="tag violet mono">{{ n.code }}</span>
        </button>
      </li>
      <li v-if="!grRows.length" class="empty">该平台还没有配置权限，请先到「权限配置」里创建</li>
    </ul>
    <div v-if="gr.error" class="err-line" role="alert">⚠ {{ gr.error }}</div>
    <template #footer>
      <span class="muted gcount">已选 {{ alwaysAll(gr.role) ? '全部' : grCount }} 项</span>
      <button class="btn" type="button" @click="gr.open = false">关闭</button>
      <button v-if="canGrant && !alwaysAll(gr.role)" class="btn primary" :disabled="gr.saving || gr.loading" @click="saveGrant">
        {{ gr.saving ? '保存中…' : '保存权限' }}
      </button>
    </template>
  </Modal>

  <Modal v-if="del" title="确认删除" width="400px" @close="del = null">
    <p class="lead">确定删除角色 <b>{{ del.name }}</b> 吗？</p>
    <p class="muted">若仍有用户使用该角色，将无法删除。</p>
    <template #footer>
      <button class="btn" @click="del = null">取消</button>
      <button class="btn danger" :disabled="deleting" @click="confirmDelete">确认删除</button>
    </template>
  </Modal>
</template>

<style scoped>
.filter-select { width: 180px; }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.rname { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.desc { color: var(--text-dim); max-width: 380px; }
.lead { line-height: 1.8; margin-bottom: 8px; }
.note { margin-bottom: 12px; padding: 8px 12px; border-radius: 8px; font-size: 13px; color: #8a6200; background: rgba(245, 179, 1, 0.12); }
.ptree { list-style: none; padding: 0; max-height: 52vh; overflow: auto; }
.prow { padding-top: 2px; padding-bottom: 2px; }
.pick { width: 100%; display: flex; align-items: center; gap: 10px; text-align: left; font: inherit; color: inherit;
  background: none; border: 0; padding: 7px 8px; border-radius: 8px; cursor: pointer; }
.pick:hover:not(:disabled) { background: rgba(15, 169, 104, 0.07); }
.pick:disabled { cursor: default; }
.pname { font-weight: 500; }
.ptype { font-size: 12px; color: var(--text-dim); }
.pick .tag { margin-left: auto; }
.box { width: 16px; height: 16px; border-radius: 4px; border: 1.5px solid #b7cbc1; position: relative; flex-shrink: 0; background: #fff; }
.box.on { background: var(--cyan); border-color: var(--cyan); }
.box.on::after { content: ''; position: absolute; left: 4px; top: 1px; width: 4px; height: 8px; border: solid #fff; border-width: 0 2px 2px 0; transform: rotate(45deg); }
.box.half { border-color: var(--cyan); }
.box.half::after { content: ''; position: absolute; left: 3px; top: 6px; width: 8px; height: 2px; background: var(--cyan); }
.gcount { margin-right: auto; align-self: center; }
@media (max-width: 900px) { .stats { grid-template-columns: 1fr; } }
</style>
