import { http } from './http'

export const credentialsApi = {
  keys: () => http.get('/me/api-keys'),
  createKey: body => http.post('/me/api-keys', body),
  issueKey: id => http.post(`/me/api-keys/${id}/issue`),
  rotateKey: id => http.post(`/me/api-keys/${id}/rotate`),
  revokeKey: id => http.post(`/me/api-keys/${id}/revoke`),
  tokens: () => http.get('/me/tokens'),
  revokeToken: id => http.post(`/me/tokens/${id}/revoke`)
}

export const apiKeyApplicationsApi = {
  list: params => http.get('/api-key-applications?' + new URLSearchParams({ page: params?.page || 1, page_size: params?.pageSize || 10, keyword: params?.keyword || '', status: params?.status || '' })),
  review: (id, body) => http.post(`/api-key-applications/${id}/review`, body),
  revoke: id => http.post(`/api-key-applications/${id}/revoke`)
}
