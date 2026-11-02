<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { platformApi } from '../api/platforms'
import { can } from '../composables/auth'
import { lookups, loadPlatforms } from '../composables/lookups'
import { fmtTime } from '../composables/format'
import { notify } from '../composables/toast'
import Modal from '../components/Modal.vue'
import Panel from '../components/Panel.vue'
import StatCard from '../components/StatCard.vue'

const canCreate = computed(() => can('platform:create'))
const canUpdate = computed(() => can('platform:update'))
const canDelete = computed(() => can('platform:delete'))
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    await loadPlatforms()
  } catch (e) {
    notify.err(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)

const roleTotal = computed(() => lookups.platforms.reduce((n, p) => n + p.roleCount, 0))

// ---------- create / edit ----------
const dlg = reactive({ open: false, editing: null, saving: false, error: '' })
const form = reactive({ code: '', name: '', desc: '' })

function openCreate() {
  Object.assign(form, { code: '', name: '', desc: '' })
  Object.assign(dlg, { open: true, editing: null, error: '' })
}
function openEdit(p) {
  Object.assign(form, { code: p.code, name: p.name, desc: p.desc || '' })
  Object.assign(dlg, { open: true, editing: p, error: '' })
}

async function save() {
  dlg.error = ''
  if (!form.name.trim()) return (dlg.error = '请填写平台名称')
  if (!dlg.editing) {
    if (!/^[a-z0-9][a-z0-9_-]*$/.test(form.code.trim())) {
      return (dlg.error = '平台编码只能包含小写字母、数字、下划线和连字符')
    }
  }
  dlg.saving = true
  try {
    if (dlg.editing) {
      await platformApi.update(dlg.editing.id, { name: form.name.trim(), desc: form.desc.trim() })
      notify.ok('平台已更新')
    } else {
      await platformApi.create({ code: form.code.trim(), name: form.name.trim(), desc: form.desc.trim() })
      notify.ok('平台已创建')
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
    await platformApi.remove(del.value.id)
    notify.ok('平台已删除')
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
    <StatCard label="平台总数" :value="lookups.platforms.length" hint="已接入的系统" />
    <StatCard label="角色总数" :value="roleTotal" hint="各平台角色之和" tone="violet" />
  </div>

  <Panel title="平台管理" subtitle="接入本中心做统一登录和权限管理的系统">
    <template #actions>
      <button class="btn" :disabled="loading" @click="load">↻ 刷新</button>
      <button v-if="canCreate" class="btn primary" @click="openCreate">＋ 新建平台</button>
    </template>

    <table class="tbl">
      <thead>
        <tr>
          <th>编码</th><th>名称</th><th>描述</th><th>角色数</th><th>创建时间</th>
          <th v-if="canUpdate || canDelete" class="ops-h">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in lookups.platforms" :key="p.id">
          <td><span class="tag violet">{{ p.code }}</span></td>
          <td class="pname">{{ p.name }} <span v-if="p.isSystem" class="tag">本中心</span></td>
          <td class="desc">{{ p.desc || '—' }}</td>
          <td class="mono">{{ p.roleCount }}</td>
          <td class="mono muted">{{ fmtTime(p.createTime) }}</td>
          <td v-if="canUpdate || canDelete">
            <div class="ops">
              <button v-if="canUpdate" class="btn sm" @click="openEdit(p)">编辑</button>
              <button v-if="canDelete" class="btn sm danger" :disabled="p.isSystem" :title="p.isSystem ? '内置平台不可删除' : ''"
                      @click="del = p">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!lookups.platforms.length" class="empty">{{ loading ? '正在加载…' : '暂无数据' }}</div>
  </Panel>

  <Modal v-if="dlg.open" :title="dlg.editing ? '编辑平台' : '新建平台'" width="460px" @close="dlg.open = false">
    <form id="platform-form" novalidate @submit.prevent="save">
      <div class="field">
        <label for="p-code">平台编码<span class="req">*</span></label>
        <input id="p-code" v-model="form.code" class="input mono" maxlength="32" placeholder="如 crm"
               :disabled="!!dlg.editing" />
        <small class="muted">{{ dlg.editing ? '编码创建后不可修改' : '小写字母、数字、下划线、连字符；创建后不可修改，删除后也不可复用' }}</small>
      </div>
      <div class="field">
        <label for="p-name">平台名称<span class="req">*</span></label>
        <input id="p-name" v-model="form.name" class="input" maxlength="64" placeholder="如 CRM 系统" />
      </div>
      <div class="field">
        <label for="p-desc">描述</label>
        <textarea id="p-desc" v-model="form.desc" class="input" rows="3" maxlength="255"></textarea>
      </div>
      <div v-if="dlg.error" class="err-line" role="alert">⚠ {{ dlg.error }}</div>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="dlg.open = false">取消</button>
      <button class="btn primary" type="submit" form="platform-form" :disabled="dlg.saving">
        {{ dlg.saving ? '保存中…' : '保存' }}
      </button>
    </template>
  </Modal>

  <Modal v-if="del" title="确认删除" width="400px" @close="del = null">
    <p class="lead">确定删除平台 <b>{{ del.name }}</b> 吗？</p>
    <p class="muted">平台下还有角色时无法删除，需要先删除这些角色。</p>
    <template #footer>
      <button class="btn" @click="del = null">取消</button>
      <button class="btn danger" :disabled="deleting" @click="confirmDelete">确认删除</button>
    </template>
  </Modal>
</template>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 20px; }
.pname { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.desc { color: var(--text-dim); max-width: 380px; }
.lead { line-height: 1.8; margin-bottom: 8px; }
@media (max-width: 900px) { .stats { grid-template-columns: 1fr; } }
</style>
