<script setup>
// Illustrative delegated-token claims: a user delegates a scoped, short-lived token to an agent.
const lines = [
  ['sub', '"user:zhangsan"'],
  ['act', '"agent:report-bot"'],
  ['scope', '["orders:read", "mcp:search"]'],
  ['ttl', '"3600s"']
]
</script>

<template>
  <div class="token">
    <header>
      <span class="name">委托令牌</span>
      <span class="live"><i></i>有效</span>
    </header>
    <div class="code mono">
      <div>{</div>
      <div v-for="(l, i) in lines" :key="l[0]" class="ln" :style="{ animationDelay: `${0.5 + i * 0.5}s` }">
        <span class="k">{{ l[0] }}</span><span class="p">: </span><span class="v">{{ l[1] }}</span><span v-if="i < lines.length - 1" class="p">,</span>
      </div>
      <div>}<i class="cur"></i></div>
    </div>
  </div>
</template>

<style scoped>
.token {
  width: 300px; padding: 12px 16px 14px; border-radius: 12px; background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--line); box-shadow: 0 18px 44px rgba(20, 70, 45, 0.16);
}
header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.name { font-size: 12px; font-weight: 700; }
.name em { font-style: normal; font-weight: 400; font-size: 10px; color: var(--text-dim); margin-left: 4px; }
.live { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: var(--green); font-weight: 600; }
.live i { width: 6px; height: 6px; border-radius: 50%; background: var(--green); box-shadow: 0 0 0 3px rgba(15, 169, 104, 0.18); animation: pulse 2s infinite; }
.code { font-size: 12px; line-height: 1.75; color: var(--text-dim); white-space: pre; }
.ln { padding-left: 16px; opacity: 0; animation: reveal 9s ease infinite; }
.k { color: #0b9b5c; font-weight: 600; }
.v { color: #a87400; }
.cur { display: inline-block; width: 7px; height: 13px; margin-left: 3px; vertical-align: -2px; background: var(--cyan); animation: blink 1s steps(2) infinite; }
@keyframes reveal {
  0% { opacity: 0; transform: translateX(-8px); }
  6%, 88% { opacity: 1; transform: none; }
  96%, 100% { opacity: 0; }
}
@keyframes blink { 50% { opacity: 0; } }
</style>
