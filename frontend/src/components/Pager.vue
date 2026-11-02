<script setup>
import { computed } from 'vue'

const props = defineProps({ page: Number, size: Number, total: Number })
const emit = defineEmits(['change'])

const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
function go(p) {
  if (p < 1 || p > pages.value || p === props.page) return
  emit('change', p)
}
</script>

<template>
  <div class="pager">
    <span class="info">共 {{ total }} 条 · 第 {{ page }} / {{ pages }} 页</span>
    <div class="ctrl">
      <button class="btn sm" :disabled="page <= 1" @click="go(page - 1)">‹ 上一页</button>
      <button class="btn sm" :disabled="page >= pages" @click="go(page + 1)">下一页 ›</button>
    </div>
  </div>
</template>

<style scoped>
.pager { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--line); font-size: 13px; }
.info { color: var(--text-dim); }
.ctrl { display: flex; gap: 8px; }
</style>
