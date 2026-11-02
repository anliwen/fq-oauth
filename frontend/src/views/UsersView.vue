<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { userApi } from '../api/users'
import { can } from '../composables/auth'
import { lookups, loadDepts, loadRoles } from '../composables/lookups'
import { PASSWORD_HINT, STATUS, fmtTime, passwordError } from '../composables/format'
import { notify } from '../composables/toast'
import Panel from '../components/Panel.vue'
import Modal from '../components/Modal.vue'
import BaseSelect from '../components/BaseSelect.vue'
import Pager from '../components/Pager.vue'
import StatCard from '../components/StatCard.vue'

const canCreate = computed(() => can('user:create'))
const canUpdate = computed(() => can('user:update'))
const canDelete = computed(() => can('user:delete'))

// ---------- list ----------
const size = 10
const state = reactive({ page: 1, total: 0, items: [], loading: false })
const filter = reactive({ keyword: '', deptId: '', status: '' })

async function load(page = state.page) {
  state.loading = true
  try {
    const r = await userApi.page({
      page, pageSize: size, keyword: filter.keyword.trim(),
      deptId: filter.deptId, status: filter.status
    })
    Object.assign(state, { items: r.items, total: r.total, page: r.page })
  } catch (e) {
    notify.err(e.message)
  } finally {
    state.loading = false
  }
}

let timer
watch(() => filter.keyword, () => { clearTimeout(timer); timer = setTimeout(() => load(1), 300) })
watch(() => [filter.deptId, filter.status], () => load(1))

onMounted(async () => {
  await Promise.allSettled([loadRoles(), loadDepts()])
  await load(1)
})

// a role code is only unique within its platform, so the platform is shown with it
const rolesOf = u => lookups.roles.filter(r => u.roleIds.includes(r.id))

// department pickers: the tree flattened, children indented
const deptOptions = computed(() => lookups.depts.map(d => ({ value: d.id, label: d.name, depth: d.depth })))
const deptFilterOptions = computed(() => [{ value: '', label: '全部部门' }, ...deptOptions.value])
const deptFormOptions = computed(() => [{ value: '', label: '未分配部门' }, ...deptOptions.value])

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'active', label: '启用' },
  { value: 'disabled', label: '禁用' },
  { value: 'locked', label: '锁定' }
]

// ---------- stat cards ----------
const systemRoles = computed(() => lookups.roles.filter(r => r.isSystem).length)

// ---------- create / edit ----------
const dlg = reactive({ open: false, editing: null, saving: false, error: '' })
const blank = () => ({
  fullName: '', username: '', password: '', employeeNo: '', deptId: '', title: '', email: '', roleIds: []
})
const form = reactive(blank())

function openCreate() {
  Object.assign(form, blank())
  const analyst = lookups.roles.find(r => r.code === 'analyst')
  if (analyst) form.roleIds = [analyst.id]
  Object.assign(dlg, { open: true, editing: null, error: '' })
}

function openEdit(u) {
  Object.assign(form, {
    fullName: u.fullName, username: u.username, password: '', employeeNo: u.employeeNo || '',
    deptId: u.deptId ?? '', title: u.title || '', email: u.email, roleIds: [...u.roleIds]
  })
  Object.assign(dlg, { open: true, editing: u, error: '' })
}

function toggleRole(id) {
  const i = form.roleIds.indexOf(id)
  if (i >= 0) form.roleIds.splice(i, 1)
  else form.roleIds.push(id)
}

async function save() {
  dlg.error = ''
  if (!form.fullName.trim()) return (dlg.error = '请填写姓名')
  if (!form.email.trim()) return (dlg.error = '请填写邮箱')
  if (!dlg.editing) {
    if (!form.username.trim()) return (dlg.error = '请填写登录名')
    const pe = passwordError(form.password)
    if (pe) return (dlg.error = pe)
  }
  dlg.saving = true
  try {
    const base = {
      fullName: form.fullName.trim(), email: form.email.trim(), title: form.title.trim() || null,
      employeeNo: form.employeeNo.trim() || null, deptId: form.deptId === '' ? null : form.deptId,
      roleIds: form.roleIds
    }
    if (dlg.editing) {
      await userApi.update(dlg.editing.id, base)
      notify.ok('用户已更新')
    } else {
      await userApi.create({ ...base, username: form.username.trim(), password: form.password })
      notify.ok('用户已创建')
    }
    const wasEdit = !!dlg.editing
    dlg.open = false
    await load(wasEdit ? state.page : 1)
  } catch (e) {
    dlg.error = e.message
  } finally {
    dlg.saving = false
  }
}

// ---------- status / reset password / delete ----------
async function toggleStatus(u) {
  const next = u.status === 'active' ? 'disabled' : 'active'
  try {
    await userApi.setStatus(u.id, next)
    notify.ok(next === 'active' ? '已启用' : '已禁用')
    await load()
  } catch (e) {
    notify.err(e.message)
  }
}

const pwd = reactive({ user: null, value: '', saving: false, error: '' })
function openPwd(u) { Object.assign(pwd, { user: u, value: '', saving: false, error: '' }) }
async function savePwd() {
  const pe = passwordError(pwd.value)
  if (pe) return (pwd.error = pe)
  pwd.saving = true
  try {
    await userApi.resetPassword(pwd.user.id, pwd.value)
    notify.ok('密码已重置，该用户需用新密码重新登录')
    pwd.user = null
  } catch (e) {
    pwd.error = e.message
  } finally {
    pwd.saving = false
  }
}

const del = ref(null)
const deleting = ref(false)
async function confirmDelete() {
  deleting.value = true
  try {
    await userApi.remove(del.value.id)
    notify.ok('用户已删除')
    const lastOnPage = state.items.length === 1 && state.page > 1
    del.value = null
    await load(lastOnPage ? state.page - 1 : state.page)
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
    <StatCard label="用户总数" :value="state.total" hint="当前筛选范围内" />
    <StatCard label="角色数" :value="lookups.roles.length" hint="系统内全部角色" tone="violet" />
    <StatCard label="内置角色" :value="systemRoles" hint="内置角色，不可删除" tone="amber" />
  </div>

  <Panel title="用户管理" subtitle="管理账号、部门、角色与状态">
    <template #actions>
      <button class="btn" :disabled="state.loading" @click="load()">↻ 刷新</button>
      <button v-if="canCreate" class="btn primary" @click="openCreate">＋ 新建用户</button>
    </template>

    <div class="toolbar">
      <input v-model="filter.keyword" class="input" placeholder="搜索姓名 / 登录名 / 邮箱 / 工号" aria-label="搜索" />
      <BaseSelect v-model="filter.deptId" class="filter-select" :options="deptFilterOptions" aria-label="部门" />
      <BaseSelect v-model="filter.status" class="filter-select" :options="statusOptions" aria-label="状态" />
    </div>

    <table class="tbl">
      <thead>
        <tr>
          <th>用户</th><th>部门</th><th>职级</th><th>角色</th><th>状态</th><th>最后登录</th>
          <th v-if="canUpdate || canDelete" class="ops-h">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in state.items" :key="u.id">
          <td>
            <div class="name">{{ u.fullName || u.username }}</div>
            <div class="sub mono">{{ u.username }}<template v-if="u.employeeNo"> · {{ u.employeeNo }}</template></div>
          </td>
          <td>{{ u.deptName || '—' }}</td>
          <td>{{ u.title || '—' }}</td>
          <td>
            <span v-for="r in rolesOf(u)" :key="r.id" class="tag violet role-tag" :title="r.platformName">{{ r.platformCode }}:{{ r.code }}</span>
            <span v-if="!u.roleIds.length" class="muted">—</span>
          </td>
          <td>
            <span class="tag" :class="STATUS[u.status]?.cls"><i class="dot"></i>{{ STATUS[u.status]?.label || u.status }}</span>
          </td>
          <td class="mono muted">{{ fmtTime(u.lastLoginAt) }}</td>
          <td v-if="canUpdate || canDelete">
            <div class="ops">
              <button v-if="canUpdate" class="btn sm" @click="openEdit(u)">编辑</button>
              <button v-if="canUpdate" class="btn sm" @click="toggleStatus(u)">{{ u.status === 'active' ? '禁用' : '启用' }}</button>
              <button v-if="canUpdate" class="btn sm" @click="openPwd(u)">重置密码</button>
              <button v-if="canDelete" class="btn sm danger" @click="del = u">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!state.items.length" class="empty">{{ state.loading ? '正在加载…' : '暂无数据' }}</div>

    <Pager :page="state.page" :size="size" :total="state.total" @change="load" />
  </Panel>

  <Modal v-if="dlg.open" :title="dlg.editing ? '编辑用户' : '新建用户'" width="620px" @close="dlg.open = false">
    <form id="user-form" novalidate @submit.prevent="save">
      <div class="grid2">
        <div class="field">
          <label for="u-name">姓名<span class="req">*</span></label>
          <input id="u-name" v-model="form.fullName" class="input" maxlength="64" placeholder="显示姓名" />
        </div>
        <div class="field">
          <label for="u-user">登录名<span class="req">*</span></label>
          <input id="u-user" v-model="form.username" class="input mono" maxlength="64"
                 :disabled="!!dlg.editing" placeholder="英文登录名" />
        </div>
        <div v-if="!dlg.editing" class="field" style="grid-column: 1 / -1">
          <label for="u-pwd">初始密码<span class="req">*</span></label>
          <input id="u-pwd" v-model="form.password" class="input" type="password" autocomplete="new-password"
                 :placeholder="PASSWORD_HINT" />
        </div>
        <div class="field">
          <label for="u-no">工号</label>
          <input id="u-no" v-model="form.employeeNo" class="input mono" maxlength="32" placeholder="如 HC0001" />
        </div>
        <div class="field">
          <label for="u-dept">部门</label>
          <BaseSelect id="u-dept" v-model="form.deptId" :options="deptFormOptions" placeholder="未分配部门" />
        </div>
        <div class="field">
          <label for="u-title">职级</label>
          <input id="u-title" v-model="form.title" class="input" maxlength="64" placeholder="如 投资经理 / 研究员" />
        </div>
        <div class="field">
          <label for="u-mail">邮箱<span class="req">*</span></label>
          <input id="u-mail" v-model="form.email" class="input" maxlength="128" placeholder="name@example.com" />
        </div>
      </div>
      <div class="field">
        <label>角色</label>
        <div class="checks">
          <label v-for="r in lookups.roles" :key="r.id" class="check" :class="{ on: form.roleIds.includes(r.id) }">
            <input type="checkbox" :checked="form.roleIds.includes(r.id)" @change="toggleRole(r.id)" />{{ r.platformName }} · {{ r.name }}
          </label>
          <span v-if="!lookups.roles.length" class="muted">暂无可分配角色</span>
        </div>
      </div>
      <div v-if="dlg.error" class="err-line" role="alert">⚠ {{ dlg.error }}</div>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="dlg.open = false">取消</button>
      <button class="btn primary" type="submit" form="user-form" :disabled="dlg.saving">
        {{ dlg.saving ? '保存中…' : '保存' }}
      </button>
    </template>
  </Modal>

  <Modal v-if="pwd.user" title="重置密码" width="420px" @close="pwd.user = null">
    <p class="lead">为 <b>{{ pwd.user.fullName || pwd.user.username }}</b> 设置新密码。重置后该用户的所有登录会话立即失效。</p>
    <div class="field">
      <label for="p-new">新密码</label>
      <input id="p-new" v-model="pwd.value" class="input" type="password" autocomplete="new-password"
             :placeholder="PASSWORD_HINT" @keyup.enter="savePwd" />
    </div>
    <div v-if="pwd.error" class="err-line" role="alert">⚠ {{ pwd.error }}</div>
    <template #footer>
      <button class="btn" @click="pwd.user = null">取消</button>
      <button class="btn primary" :disabled="pwd.saving" @click="savePwd">{{ pwd.saving ? '提交中…' : '重置' }}</button>
    </template>
  </Modal>

  <Modal v-if="del" title="确认删除" width="400px" @close="del = null">
    <p class="lead">确定删除用户 <b>{{ del.fullName || del.username }}</b>（{{ del.username }}）吗？账号会被停用并立即退出登录。</p>
    <template #footer>
      <button class="btn" @click="del = null">取消</button>
      <button class="btn danger" :disabled="deleting" @click="confirmDelete">确认删除</button>
    </template>
  </Modal>
</template>

<style scoped>
.filter-select { width: 180px; }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.name { font-weight: 600; }
.sub { font-size: 12px; color: var(--text-dim); margin-top: 2px; }
.role-tag { margin: 0 4px 4px 0; }
.lead { line-height: 1.8; margin-bottom: 14px; }
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(2, 1fr); } }
</style>
