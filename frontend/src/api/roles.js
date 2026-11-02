import { http } from './http'

export const roleApi = {
  list: () => http.get('/roles'),
  create: payload => http.post('/roles', payload),
  update: (id, payload) => http.put(`/roles/${id}`, payload),
  remove: id => http.del(`/roles/${id}`)
}
