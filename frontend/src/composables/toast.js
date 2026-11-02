import { reactive } from 'vue'

export const toasts = reactive([])
let seq = 0

export function toast(message, type = 'info', ms = 3200) {
  const id = ++seq
  toasts.push({ id, message, type })
  setTimeout(() => {
    const i = toasts.findIndex(t => t.id === id)
    if (i >= 0) toasts.splice(i, 1)
  }, ms)
}

export const notify = {
  ok: m => toast(m, 'ok'),
  err: m => toast(m, 'err', 4800),
  info: m => toast(m, 'info')
}
