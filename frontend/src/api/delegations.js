import { http } from './http'

export const delegationApi = {
  list: () => http.get('/delegations'),
  create: payload => http.post('/delegations', payload),
  revoke: id => http.post(`/delegations/${id}/revoke`),
  restore: id => http.post(`/delegations/${id}/restore`),
  check: (agentId, tenantId, payload) => http.post(`/delegations/check?agentId=${encodeURIComponent(agentId)}&tenantId=${encodeURIComponent(tenantId)}`, payload)
}
