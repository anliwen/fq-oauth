<script setup>
defineProps({
  label: String,
  value: [Number, String],
  hint: String,
  tone: { type: String, default: 'blue' }
})
</script>

<template>
  <div class="stat" :class="tone">
    <div class="meta">
      <span class="label">{{ label }}</span>
      <span class="value mono">{{ value }}</span>
      <span v-if="hint" class="hint">{{ hint }}</span>
    </div>
    <div class="orb"><i></i></div>
  </div>
</template>

<style scoped>
.stat {
  --c1: #0b9b5c; --c2: #5bc236;
  position: relative; overflow: hidden; display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px; border-radius: 14px; background: var(--panel-solid);
  border: 1px solid var(--line); box-shadow: var(--shadow); animation: fadeUp 0.35s ease;
}
.stat.green { --c1: #0b9b5c; --c2: #2fbf71; }
.stat.amber { --c1: #f5b301; --c2: #ff8a1f; }
.stat.violet { --c1: #f5b301; --c2: #c98a00; }
.stat::after {
  content: ''; position: absolute; right: -30px; top: -30px; width: 110px; height: 110px; border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--c1) 18%, transparent), transparent 70%);
}
.meta { display: flex; flex-direction: column; gap: 4px; }
.label { font-size: 12px; color: var(--text-dim); font-weight: 600; letter-spacing: 0.06em; }
.value { font-size: 30px; font-weight: 700; line-height: 1.1; background: linear-gradient(135deg, var(--c1), var(--c2)); -webkit-background-clip: text; background-clip: text; color: transparent; }
.hint { font-size: 11px; color: var(--text-dim); }
.orb { width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center; background: linear-gradient(135deg, var(--c1), var(--c2)); box-shadow: 0 6px 16px color-mix(in srgb, var(--c1) 40%, transparent); z-index: 1; }
.orb i { width: 16px; height: 16px; border: 2.5px solid #fff; border-radius: 50%; border-top-color: transparent; }
</style>
