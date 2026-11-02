<script setup>
import { toasts } from '../composables/toast'
</script>

<template>
  <div class="toast-host">
    <TransitionGroup name="t">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">
        <span class="bar"></span>{{ t.message }}
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed; top: 70px; right: 24px; z-index: 100;
  display: flex; flex-direction: column; gap: 10px; pointer-events: none;
}
.toast {
  position: relative; min-width: 220px; max-width: 360px;
  padding: 11px 16px 11px 20px; font-size: 12px;
  background: #fff; border: 1px solid var(--line); border-radius: 10px;
  box-shadow: 0 10px 30px rgba(20, 70, 45, 0.15); overflow: hidden;
}
.bar { position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--blue); }
.toast.ok { color: var(--green); }
.toast.ok .bar { background: var(--green); }
.toast.err { color: var(--red); }
.toast.err .bar { background: var(--red); }
.t-enter-active, .t-leave-active { transition: all 0.25s ease; }
.t-enter-from, .t-leave-to { opacity: 0; transform: translateX(30px); }
</style>
