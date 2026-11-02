import { http } from './http'

export const permissionApi = {
  tree: platformId => http.get(`/permissions?platform_id=${platformId}`),
  create: payload => http.post('/permissions', payload),
  update: (id, payload) => http.put(`/permissions/${id}`, payload),
  remove: id => http.del(`/permissions/${id}`),
  granted: roleId => http.get(`/roles/${roleId}/permissions`),
  grant: (roleId, permissionIds) => http.put(`/roles/${roleId}/permissions`, { permissionIds })
}
