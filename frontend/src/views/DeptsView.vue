<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deptApi } from '../api/depts'
import { can } from '../composables/auth'
import { lookups, loadDepts } from '../composables/lookups'
import { descendantIds } from '../composables/permTree'
import { notify } from '../composables/toast'
import BaseSelect from '../components/BaseSelect.vue'
import Modal from '../components/Modal.vue'
import Panel from '../components/Panel.vue'
import StatCard from '../components/StatCard.vue'

const canCreate = computed(() => can('dept:create'))
const canUpdate = computed(() => can('dept:update'))
const canDelete = computed(() => can('dept:delete'))
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    await loadDepts()
  } catch (e) {
    notify.err(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)

const rows = computed(() => lookups.depts)
const members = computed(() => rows.value.reduce((n, d) => n + d.userCount, 0))
const rootCount = computed(() => rows.value.filter(d => d.parentId === null).length)

// ---------- create / edit ----------
const dlg = reactive({ open: false, editing: null, saving: false, error: '' })
const form = reactive({ name: '', parentId: '', leader: '', sort: 0 })

/** Where a department may sit: anywhere except under itself or one of its own descendants. */
const parentOptions = computed(() => {
  const banned = new Set(dlg.editing ? [dlg.editing.id, ...descendantIds(dlg.editing)] : [])
  return [
    { value: '', label: '无（作为根部门）' },
    ...rows.value.filter(d => !banned.has(d.id)).map(d => ({ value: d.id, label: d.name, depth: d.depth }))
  ]
})

function openCreate(parent) {
  Object.assign(form, { name: '', parentId: parent ? parent.id : '', leader: '', sort: 0 })
  Object.assign(dlg, { open: true, editing: null, error: '' })
}
function openEdit(d) {
  Object.assign(form, { name: d.name, parentId: d.parentId ?? '', leader: d.leader || '', sort: d.sort })
  Object.assign(dlg, { open: true, editing: d, error: '' })
}

async function save() {
  dlg.error = ''
  if (!form.name.trim()) return (dlg.error = '请填写部门名称')
  dlg.saving = true
  try {
    const payload = {
      name: form.name.trim(), parentId: form.parentId === '' ? null : form.parentId,
      leader: form.leader.trim(), sort: Number.isFinite(Number(form.sort)) ? Number(form.sort) : 0
    }
    if (dlg.editing) {
      await deptApi.update(dlg.editing.id, payload)
      notify.ok('部门已更新')
    } else {
      await deptApi.create(payload)
      notify.ok('部门已创建')
    }
    dlg.open = false
    await load()
  } catch (e) {
    dlg.error = e.message
  } finally {
    dlg.saving = false
  }
}

// ---------- delete ----------
const del = ref(null)
const deleting = ref(false)
async function confirmDelete() {
  deleting.value = true
  try {
    await deptApi.remove(del.value.id)
    notify.ok('部门已删除')
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
    <StatCard label="部门总数" :value="rows.length" hint="未删除的部门" />
    <StatCard label="根部门" :value="rootCount" hint="没有上级的部门" tone="green" />
    <StatCard label="已归属成员" :value="members" hint="属于某个部门的用户" tone="violet" />
  </div>

  <Panel title="部门管理" subtitle="组织机构树；用户在用户管理里归属到部门">
    <template #actions>
      <button class="btn" :disabled="loading" @click="load">↻ 刷新</button>
      <button v-if="canCreate" class="btn primary" @click="openCreate(null)">＋ 新建部门</button>
    </template>

    <table class="tbl">
      <thead>
        <tr><th>部门</th><th>负责人</th><th>成员数</th><th>排序</th><th v-if="canCreate || canUpdate || canDelete" class="ops-h">操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in rows" :key="d.id">
          <td>
            <span class="indent" :style="{ width: d.depth * 22 + 'px' }"></span>
            <span class="glyph" aria-hidden="true"></span>
            <span class="dname">{{ d.name }}</span>
          </td>
          <td>{{ d.leader || '—' }}</td>
          <td class="mono">{{ d.userCount }}</td>
          <td class="mono muted">{{ d.sort }}</td>
          <td v-if="canCreate || canUpdate || canDelete">
            <div class="ops">
              <button v-if="canCreate" class="btn sm" @click="openCreate(d)">＋ 子部门</button>
              <button v-if="canUpdate" class="btn sm" @click="openEdit(d)">编辑</button>
              <button v-if="canDelete" class="btn sm danger" @click="del = d">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : '暂无数据' }}</div>
  </Panel>

  <Modal v-if="dlg.open" :title="dlg.editing ? '编辑部门' : '新建部门'" width="480px" @close="dlg.open = false">
    <form id="dept-form" novalidate @submit.prevent="save">
      <div class="field">
        <label for="d-name">部门名称<span class="req">*</span></label>
        <input id="d-name" v-model="form.name" class="input" maxlength="64" placeholder="如 研究组" />
        <small class="muted">名称全局唯一，删除过的部门名称也不能再用</small>
      </div>
      <div class="field">
        <label for="d-parent">上级部门</label>
        <BaseSelect id="d-parent" v-model="form.parentId" :options="parentOptions" />
      </div>
      <div class="field">
        <label for="d-leader">负责人</label>
        <input id="d-leader" v-model="form.leader" class="input" maxlength="64" />
      </div>
      <div class="field">
        <label for="d-sort">排序</label>
        <input id="d-sort" v-model="form.sort" class="input mono" type="number" />
      </div>
      <div v-if="dlg.error" class="err-line" role="alert">⚠ {{ dlg.error }}</div>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="dlg.open = false">取消</button>
      <button class="btn primary" type="submit" form="dept-form" :disabled="dlg.saving">
        {{ dlg.saving ? '保存中…' : '保存' }}
      </button>
    </template>
  </Modal>

  <Modal v-if="del" title="确认删除" width="420px" @close="del = null">
    <p class="lead">确定删除部门 <b>{{ del.name }}</b> 吗？</p>
    <p class="muted">有子部门或仍有成员时无法删除，需要先删除子部门、调整成员。</p>
    <template #footer>
      <button class="btn" @click="del = null">取消</button>
      <button class="btn danger" :disabled="deleting" @click="confirmDelete">确认删除</button>
    </template>
  </Modal>
</template>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.indent { display: inline-block; vertical-align: middle; }
.glyph { display: inline-block; width: 10px; height: 10px; margin-right: 10px; vertical-align: middle; border-radius: 3px; background: #0b9b5c; }
.dname { font-weight: 600; }
.lead { line-height: 1.8; margin-bottom: 8px; }
@media (max-width: 900px) { .stats { grid-template-columns: 1fr; } }
</style>
