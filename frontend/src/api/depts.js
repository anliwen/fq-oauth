import { http } from './http'

export const deptApi = {
  tree: () => http.get('/depts'),
  create: payload => http.post('/depts', payload),
  update: (id, payload) => http.put(`/depts/${id}`, payload),
  remove: id => http.del(`/depts/${id}`)
}
