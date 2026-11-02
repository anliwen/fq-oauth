export function fmtTime(s) {
  if (!s) return '—'
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return '—'
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

export const STATUS = {
  active: { label: '启用', cls: 'ok' },
  disabled: { label: '禁用', cls: 'off' },
  locked: { label: '锁定', cls: 'bad' }
}

/** Same rule as the backend: 8-128 chars, letters and digits. Returns an error message or null. */
export function passwordError(pw) {
  if (pw.length < 8 || pw.length > 128) return '密码长度须为 8-128 位'
  if (!(/[A-Za-z]/.test(pw) && /\d/.test(pw))) return '密码须同时包含字母与数字'
  return null
}
export const PASSWORD_HINT = '8-128 位，须同时包含字母和数字'
