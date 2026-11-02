import { http } from './http'

export const oauthApi = {
  clients: params => http.get('/oauth-clients?' + new URLSearchParams({ page: params?.page || 1, page_size: params?.pageSize || 10, keyword: params?.keyword || '', client_kind: params?.clientKind || '', status: params?.status || '' })),
  createClient: body => http.post('/oauth-clients', body),
  updateClient: (id, body) => http.put(`/oauth-clients/${encodeURIComponent(id)}`, body),
  updateClientStatus: (id, status) => http.put(`/oauth-clients/${encodeURIComponent(id)}/status`, { status }),
  deleteClient: id => http.del(`/oauth-clients/${encodeURIComponent(id)}`),
  authorize: params => fetch(`/api/v1/oauth/authorize?${new URLSearchParams(params)}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('fq_token') || ''}` }
  }).then(async r => { const body = await r.json(); if (!r.ok || body.code !== 'OK') throw new Error(body.message || '授权请求无效'); return body.data }),
  confirm: body => fetch('/oauth/authorize/confirm', { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('fq_token') || ''}` }, body: JSON.stringify(body) }).then(async r => { const data = await r.json(); if (!r.ok || data.code !== 'OK') throw new Error(data.message || '授权失败'); return data.data })
}
