import { auth, clearSession } from '../composables/auth'

const BASE = '/api/v1'

// Backend envelope: { code: "OK", message, data }. Errors carry the same shape with a non-OK code.
export async function request(method, url, body) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth.token) headers['Authorization'] = `Bearer ${auth.token}`

  const res = await fetch(BASE + url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  })

  // Session expired / invalid: drop it and go to login. The login call itself also returns 401 for bad
  // credentials; that error is surfaced to the form instead.
  if (res.status === 401 && !url.startsWith('/auth/token')) {
    clearSession()
    if (location.pathname !== '/login') location.assign('/login')
    throw new Error('登录已过期，请重新登录')
  }
  if (res.status === 204) return null

  const text = await res.text()
  const data = text ? safeJson(text) : null
  if (!res.ok) throw new Error(errorMessage(data, res))
  if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
    if (data.code !== 'OK') throw new Error(data.message || '请求失败')
    return data.data
  }
  return data
}

function safeJson(text) {
  try { return JSON.parse(text) } catch { return null }
}

function errorMessage(data, res) {
  if (data) {
    if (Array.isArray(data.details) && data.details.length) {
      return `${data.message || '请求参数校验失败'}：` + data.details.map(d => `${d.loc} ${d.msg}`).join('；')
    }
    if (data.message) return data.message
  }
  return `请求失败 (${res.status})`
}

export const http = {
  get: url => request('GET', url),
  post: (url, body) => request('POST', url, body ?? {}),
  put: (url, body) => request('PUT', url, body ?? {}),
  del: url => request('DELETE', url)
}

export function query(params) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.set(k, v)
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}
