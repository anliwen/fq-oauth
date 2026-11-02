import { http } from './http'

export const platformApi = {
  list: () => http.get('/platforms'),
  create: payload => http.post('/platforms', payload),
  update: (id, payload) => http.put(`/platforms/${id}`, payload),
  remove: id => http.del(`/platforms/${id}`)
}
