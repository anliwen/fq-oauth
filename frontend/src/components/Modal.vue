<script setup>
import { onMounted, onUnmounted } from 'vue'

defineProps({ title: String, width: { type: String, default: '480px' } })
const emit = defineEmits(['close'])

function onKey(e) { if (e.key === 'Escape') emit('close') }
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div class="mask" @mousedown.self="emit('close')">
      <div class="dialog" :style="{ width }" role="dialog" aria-modal="true" :aria-label="title">
        <header>
          <span class="title">{{ title }}</span>
          <button class="close" type="button" aria-label="关闭" @click="emit('close')">✕</button>
        </header>
        <div class="body"><slot /></div>
        <footer v-if="$slots.footer"><slot name="footer" /></footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.mask {
  position: fixed; inset: 0; z-index: 50;
  display: grid; place-items: center;
  background: rgba(20, 40, 30, 0.38); backdrop-filter: blur(4px);
}
.dialog {
  position: relative; max-width: 92vw; max-height: 88vh; display: flex; flex-direction: column;
  background: #fff; border-radius: 16px; overflow: hidden;
  box-shadow: 0 24px 70px rgba(20, 70, 45, 0.28);
  animation: fadeUp 0.2s ease;
}
header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid var(--line); }
.title { font-weight: 700; font-size: 15px; }
.close { background: none; border: 0; color: var(--text-dim); cursor: pointer; font-size: 14px; }
.close:hover { color: var(--red); }
.body { padding: 20px; overflow: auto; }
footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid var(--line); }
</style>
