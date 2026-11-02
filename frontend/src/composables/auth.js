import { reactive } from 'vue'

const KEY = 'fq_token'

export const auth = reactive({
  token: localStorage.getItem(KEY) || '',
  user: null,
  /** Permission codes of this centre the user holds; null until the first login/me response. */
  permissions: null
})

export function setSession(token, user, permissions) {
  auth.token = token
  auth.user = user
  auth.permissions = permissions || []
  localStorage.setItem(KEY, token)
}

export function applyMe(me) {
  auth.user = me.user
  auth.permissions = me.permissions || []
}

export function clearSession() {
  auth.token = ''
  auth.user = null
  auth.permissions = null
  localStorage.removeItem(KEY)
}

/**
 * Does the user hold this permission code? The backend checks the very same code on the endpoint, so hiding a
 * button here is only a convenience; it is never the security boundary.
 */
export const can = code => !!auth.permissions && auth.permissions.includes(code)
export const hasRole = role => !!auth.user?.roles?.includes(role)
