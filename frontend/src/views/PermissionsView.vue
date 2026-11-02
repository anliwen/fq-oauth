<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { permissionApi } from '../api/permissions'
import { can } from '../composables/auth'
import { lookups, loadPlatforms } from '../composables/lookups'
import { MAX_DEPTH, TYPE_LABEL, childTypes, flattenTree } from '../composables/permTree'
import { notify } from '../composables/toast'
import BaseSelect from '../components/BaseSelect.vue'
import Modal from '../components/Modal.vue'
import Panel from '../components/Panel.vue'
import StatCard from '../components/StatCard.vue'

const canCreate = computed(() => can('permission:create'))
const canUpdate = computed(() => can('permission:update'))
const canDelete = computed(() => can('permission:delete'))

const platformId = ref('')
const tree = ref([])
const loading = ref(false)
const platformOptions = computed(() =>
  lookups.platforms.map(p => ({ value: p.id, label: `${p.name}（${p.code}）` })))
const rows = computed(() => flattenTree(tree.value))
// a platform has one root: once it exists, new nodes go under it
const hasRoot = computed(() => tree.value.length > 0)
const count = type => rows.value.filter(r => r.type === type).length

async function loadTree() {
  if (platformId.value === '') { tree.value = []; return }
  loading.value = true
  try {
    tree.value = await permissionApi.tree(platformId.value)
  } catch (e) {
    notify.err(e.message)
  } finally {
    loading.value = false
  }
}
watch(platformId, loadTree)

onMounted(async () => {
  try { await loadPlatforms() } catch (e) { notify.err(e.message) }
  if (lookups.platforms.length) {
    // the centre itself first: it is the one whose codes the buttons in this very app check
    platformId.value = (lookups.platforms.find(p => p.isSystem) || lookups.platforms[0]).id
  }
})

// ---------- create / edit ----------
const dlg = reactive({ open: false, editing: null, parent: null, saving: false, error: '' })
const form = reactive({ code: '', name: '', type: 'menu', sort: 0 })
const typeOptions = computed(() =>
  childTypes(dlg.parent).map(t => ({ value: t, label: TYPE_LABEL[t] })))

function openCreate(parent) {
  if (!parent && hasRoot.value) return
  const types = childTypes(parent)
  Object.assign(form, { code: '', name: '', type: types[0], sort: 0 })
  Object.assign(dlg, { open: true, editing: null, parent, error: '' })
}
function openEdit(node) {
  Object.assign(form, { code: node.code, name: node.name, type: node.type, sort: node.sort })
  Object.assign(dlg, { open: true, editing: node, parent: null, error: '' })
}

async function save() {
  dlg.error = ''
  if (!form.name.trim()) return (dlg.error = '请填写名称')
  if (!dlg.editing && !/^[a-z0-9][a-z0-9:_-]*$/.test(form.code.trim())) {
    return (dlg.error = '权限编码只能包含小写字母、数字、冒号、下划线和连字符')
  }
  dlg.saving = true
  try {
    const sort = Number.isFinite(Number(form.sort)) ? Number(form.sort) : 0
    if (dlg.editing) {
      await permissionApi.update(dlg.editing.id, { name: form.name.trim(), sort })
      notify.ok('权限已更新')
    } else {
      await permissionApi.create({
        platformId: platformId.value, parentId: dlg.parent ? dlg.parent.id : null,
        code: form.code.trim(), name: form.name.trim(), type: form.type, sort
      })
      notify.ok('权限已创建')
    }
    dlg.open = false
    await loadTree()
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
    await permissionApi.remove(del.value.id)
    notify.ok('权限已删除')
    del.value = null
    await loadTree()
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
    <StatCard label="目录" :value="count('directory')" hint="分组" />
    <StatCard label="菜单（页面）" :value="count('menu')" hint="前端页面入口" tone="green" />
    <StatCard label="按钮（功能）" :value="count('button')" hint="页面里的操作" tone="amber" />
  </div>

  <Panel title="权限配置" subtitle="目录 › 菜单 › 按钮；权限编码在前端控制显示，在后端接口上放行">
    <template #actions>
      <BaseSelect v-model="platformId" class="plat" :options="platformOptions" placeholder="选择平台" aria-label="平台" />
      <button class="btn" :disabled="loading || platformId === ''" @click="loadTree">↻ 刷新</button>
      <span v-if="canCreate" :title="hasRoot ? '已存在根节点，请在它下面添加子节点' : ''">
        <button class="btn primary" :disabled="platformId === '' || loading || hasRoot" @click="openCreate(null)">＋ 新建根节点</button>
      </span>
    </template>

    <table class="tbl">
      <thead>
        <tr><th>名称</th><th>权限编码</th><th>类型</th><th>排序</th><th v-if="canCreate || canUpdate || canDelete" class="ops-h">操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="n in rows" :key="n.id">
          <td>
            <span class="indent" :style="{ width: n.depth * 22 + 'px' }"></span>
            <span class="glyph" :class="n.type" aria-hidden="true"></span>
            <span class="nname">{{ n.name }}</span>
            <span v-if="n.isSystem" class="tag">内置</span>
          </td>
          <td><span class="tag violet mono">{{ n.code }}</span></td>
          <td>{{ TYPE_LABEL[n.type] }}</td>
          <td class="mono muted">{{ n.sort }}</td>
          <td v-if="canCreate || canUpdate || canDelete">
            <div class="ops">
              <button v-if="canCreate && childTypes(n).length" class="btn sm" @click="openCreate(n)">＋ 子节点</button>
              <button v-if="canUpdate" class="btn sm" @click="openEdit(n)">编辑</button>
              <button v-if="canDelete" class="btn sm danger" :disabled="n.isSystem" :title="n.isSystem ? '内置权限已被代码引用，不可删除' : ''"
                      @click="del = n">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!rows.length" class="empty">{{ loading ? '正在加载…' : platformId === '' ? '请先选择平台' : '暂无数据' }}</div>
  </Panel>

  <Modal v-if="dlg.open" :title="dlg.editing ? '编辑权限' : dlg.parent ? `在「${dlg.parent.name}」下新建` : '新建根节点'"
         width="480px" @close="dlg.open = false">
    <form id="perm-form" novalidate @submit.prevent="save">
      <div class="field">
        <label for="pm-type">类型</label>
        <BaseSelect v-if="!dlg.editing" id="pm-type" v-model="form.type" :options="typeOptions" />
        <input v-else id="pm-type" class="input" :value="TYPE_LABEL[form.type]" disabled />
        <small class="muted">类型不限上下级关系；树最多 {{ MAX_DEPTH }} 层</small>
      </div>
      <div class="field">
        <label for="pm-code">权限编码<span class="req">*</span></label>
        <input id="pm-code" v-model="form.code" class="input mono" maxlength="64" placeholder="如 user:export"
               :disabled="!!dlg.editing" />
        <small class="muted">{{ dlg.editing ? '编码创建后不可修改，代码里引用的就是它' : '小写字母、数字、冒号、下划线、连字符；同一平台内唯一' }}</small>
      </div>
      <div class="field">
        <label for="pm-name">名称<span class="req">*</span></label>
        <input id="pm-name" v-model="form.name" class="input" maxlength="64" placeholder="如 导出用户" />
      </div>
      <div class="field">
        <label for="pm-sort">排序</label>
        <input id="pm-sort" v-model="form.sort" class="input mono" type="number" />
      </div>
      <div v-if="dlg.error" class="err-line" role="alert">⚠ {{ dlg.error }}</div>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="dlg.open = false">取消</button>
      <button class="btn primary" type="submit" form="perm-form" :disabled="dlg.saving">
        {{ dlg.saving ? '保存中…' : '保存' }}
      </button>
    </template>
  </Modal>

  <Modal v-if="del" title="确认删除" width="420px" @close="del = null">
    <p class="lead">确定删除 <b>{{ del.name }}</b>（<span class="mono">{{ del.code }}</span>）吗？</p>
    <p class="muted">已经分配给角色的授权会一并消失；有子节点时需先删除子节点。</p>
    <template #footer>
      <button class="btn" @click="del = null">取消</button>
      <button class="btn danger" :disabled="deleting" @click="confirmDelete">确认删除</button>
    </template>
  </Modal>
</template>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.plat { width: 240px; }
.indent { display: inline-block; vertical-align: middle; }
.glyph { display: inline-block; width: 10px; height: 10px; margin-right: 10px; vertical-align: middle; border-radius: 3px; }
.glyph.directory { background: #0b9b5c; }
.glyph.menu { background: #3fcf8e; }
.glyph.button { background: #f5b301; border-radius: 50%; }
.nname { margin-right: 8px; font-weight: 500; }
.lead { line-height: 1.8; margin-bottom: 8px; }
@media (max-width: 900px) { .stats { grid-template-columns: 1fr; } }
</style>
