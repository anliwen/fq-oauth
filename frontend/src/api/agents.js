import { http } from './http'

export const agentApi = {
  list: () => http.get('/agents'),
  create: payload => http.post('/agents', payload),
  suspend: id => http.post(`/agents/${id}/suspend`),
  revoke: id => http.post(`/agents/${id}/revoke`)
}
