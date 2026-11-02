<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  /** [{ value, label, depth? }]; `depth` indents the entry in the open list only, the closed box shows the bare label */
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择' },
  disabled: { type: Boolean, default: false },
  id: { type: String, default: undefined },
  ariaLabel: { type: String, default: undefined }
})
const emit = defineEmits(['update:modelValue'])

let seq = 0
const uid = `bs-${++seq}-${Math.random().toString(36).slice(2, 7)}`
const listId = `${uid}-list`

const open = ref(false)
const active = ref(-1)
const trigger = ref(null)
const popup = ref(null)
const pos = ref({ left: 0, top: 0, width: 0, maxHeight: 280, above: false })

const selected = computed(() => props.options.find(o => o.value === props.modelValue))
const label = computed(() => selected.value?.label ?? '')

function place() {
  const el = trigger.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const gap = 6
  const below = window.innerHeight - r.bottom - gap - 12
  const above = r.top - gap - 12
  const want = Math.min(props.options.length * 38 + 12, 280)
  const flip = below < want && above > below
  const maxHeight = Math.max(120, Math.min(280, flip ? above : below))
  pos.value = {
    left: r.left,
    width: r.width,
    maxHeight,
    above: flip,
    top: flip ? r.top - gap : r.bottom + gap
  }
}

async function openList() {
  if (props.disabled || open.value) return
  place()
  open.value = true
  active.value = Math.max(0, props.options.findIndex(o => o.value === props.modelValue))
  await nextTick()
  popup.value?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' })
}
function closeList() {
  open.value = false
  active.value = -1
}
function choose(i) {
  const o = props.options[i]
  if (!o) return
  emit('update:modelValue', o.value)
  closeList()
  trigger.value?.focus()
}

function move(delta) {
  const n = props.options.length
  if (!n) return
  active.value = (active.value + delta + n) % n
  nextTick(() => popup.value?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' }))
}

function onKey(e) {
  if (props.disabled) return
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      open.value ? move(1) : openList()
      break
    case 'ArrowUp':
      e.preventDefault()
      open.value ? move(-1) : openList()
      break
    case 'Home':
      if (open.value) { e.preventDefault(); active.value = 0 }
      break
    case 'End':
      if (open.value) { e.preventDefault(); active.value = props.options.length - 1 }
      break
    case 'Enter':
    case ' ':
      e.preventDefault()
      open.value ? choose(active.value) : openList()
      break
    case 'Escape':
      // closes only the list; stopping it here keeps an enclosing dialog open
      if (open.value) { e.preventDefault(); e.stopPropagation(); closeList() }
      break
    case 'Tab':
      closeList()
      break
  }
}

function onDocDown(e) {
  if (trigger.value?.contains(e.target) || popup.value?.contains(e.target)) return
  closeList()
}
function onViewport(e) {
  if (popup.value?.contains(e.target)) return
  place()
}

watch(open, v => {
  if (v) {
    document.addEventListener('mousedown', onDocDown)
    window.addEventListener('resize', onViewport)
    window.addEventListener('scroll', onViewport, true)
  } else {
    document.removeEventListener('mousedown', onDocDown)
    window.removeEventListener('resize', onViewport)
    window.removeEventListener('scroll', onViewport, true)
  }
})
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocDown)
  window.removeEventListener('resize', onViewport)
  window.removeEventListener('scroll', onViewport, true)
})

const popupStyle = computed(() => {
  const p = pos.value
  return {
    left: `${p.left}px`,
    width: `${p.width}px`,
    maxHeight: `${p.maxHeight}px`,
    top: `${p.top}px`,
    transform: p.above ? 'translateY(-100%)' : 'none'
  }
})
</script>

<template>
  <div class="bsel">
    <button
      :id="id"
      ref="trigger"
      type="button"
      class="trigger"
      :class="{ open, placeholder: !selected, disabled }"
      :disabled="disabled"
      role="combobox"
      aria-haspopup="listbox"
      :aria-expanded="open"
      :aria-controls="listId"
      :aria-label="ariaLabel"
      :aria-activedescendant="open && active >= 0 ? `${uid}-o${active}` : undefined"
      @click="open ? closeList() : openList()"
      @keydown="onKey"
    >
      <span class="text">{{ selected ? label : placeholder }}</span>
      <svg class="chev" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
           stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>

    <Teleport to="body">
      <Transition name="bsel-pop">
        <ul
          v-if="open"
          :id="listId"
          ref="popup"
          class="bsel-pop"
          :class="{ above: pos.above }"
          role="listbox"
          :style="popupStyle"
        >
          <li
            v-for="(o, i) in options"
            :id="`${uid}-o${i}`"
            :key="String(o.value)"
            role="option"
            class="opt"
            :class="{ sel: o.value === modelValue, act: i === active }"
            :aria-selected="o.value === modelValue"
            :data-active="i === active"
            :style="o.depth ? { paddingLeft: `${10 + o.depth * 16}px` } : undefined"
            @mousedown.prevent
            @mouseenter="active = i"
            @click="choose(i)"
          >
            <span class="olabel">{{ o.label }}</span>
            <svg v-if="o.value === modelValue" class="tick" viewBox="0 0 24 24" width="16" height="16" fill="none"
                 stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"
                 aria-hidden="true"><path d="m5 12 5 5L20 7" /></svg>
          </li>
          <li v-if="!options.length" class="none">暂无选项</li>
        </ul>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.bsel { position: relative; width: 100%; }
.trigger {
  width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 8px;
  font: inherit; text-align: left; cursor: pointer; color: var(--text);
  background: #fff; border: 1px solid rgba(15, 169, 104, 0.22); border-radius: 8px; padding: 9px 12px;
  outline: none; transition: border-color 0.18s, box-shadow 0.18s, background 0.18s;
}
.trigger:hover:not(.disabled) { border-color: rgba(15, 169, 104, 0.5); }
.trigger:focus-visible, .trigger.open { border-color: var(--cyan); box-shadow: 0 0 0 3px rgba(15, 169, 104, 0.14); }
.trigger.placeholder .text { color: #aab4cf; }
.trigger.disabled { background: #f0f3fb; color: var(--text-dim); cursor: not-allowed; }
.text { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chev { flex-shrink: 0; color: var(--text-dim); transition: transform 0.18s ease, color 0.18s; }
.trigger.open .chev { transform: rotate(180deg); color: var(--cyan); }
</style>

<!-- the popup is teleported to <body>, so its styles cannot be scoped -->
<style>
.bsel-pop {
  position: fixed; z-index: 300; list-style: none; margin: 0; padding: 6px; overflow-y: auto;
  background: #fff; border: 1px solid rgba(15, 169, 104, 0.18); border-radius: 12px;
  box-shadow: 0 16px 40px rgba(20, 70, 45, 0.18), 0 2px 6px rgba(20, 70, 45, 0.06);
}
.bsel-pop .opt {
  display: flex; align-items: center; justify-content: space-between; gap: 10px; cursor: pointer;
  padding: 8px 10px; border-radius: 8px; font-size: 14px; color: #17301f; transition: background 0.12s;
}
.bsel-pop .opt.act { background: rgba(15, 169, 104, 0.09); }
.bsel-pop .opt.sel { color: #0b9b5c; font-weight: 600; }
.bsel-pop .opt .tick { flex-shrink: 0; color: #0b9b5c; }
.bsel-pop .olabel { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bsel-pop .none { padding: 10px; color: #6b8576; font-size: 13px; text-align: center; }
.bsel-pop-enter-active, .bsel-pop-leave-active { transition: opacity 0.14s ease, margin 0.14s ease; }
.bsel-pop-enter-from, .bsel-pop-leave-to { opacity: 0; margin-top: -4px; }
.bsel-pop.above.bsel-pop-enter-from, .bsel-pop.above.bsel-pop-leave-to { margin-top: 4px; }
</style>
