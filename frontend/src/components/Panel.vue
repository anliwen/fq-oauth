<script setup>
defineProps({ title: String, subtitle: String })
</script>

<template>
  <section class="panel">
    <header>
      <div>
        <h2>{{ title }}</h2>
        <p v-if="subtitle" class="mono">{{ subtitle }}</p>
      </div>
      <div class="actions"><slot name="actions" /></div>
    </header>
    <slot />
  </section>
</template>

<style scoped>
.panel {
  position: relative; animation: fadeUp 0.35s ease;
  background: var(--panel-solid); border: 1px solid var(--line);
  border-radius: 14px; overflow: hidden; box-shadow: var(--shadow);
}
header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; }
h2 { font-size: 16px; font-weight: 700; }
p { margin-top: 4px; font-size: 12px; color: var(--text-dim); }
.actions { display: flex; gap: 10px; }
</style>
