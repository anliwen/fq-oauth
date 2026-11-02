import { http, query } from './http'

export const userApi = {
  page: ({ page = 1, pageSize = 10, keyword, deptId, status } = {}) =>
    http.get('/users' + query({ page, page_size: pageSize, keyword, dept_id: deptId, status })),
  create: payload => http.post('/users', payload),
  update: (id, payload) => http.put(`/users/${id}`, payload),
  remove: id => http.del(`/users/${id}`),
  setStatus: (id, status) => http.put(`/users/${id}/status`, { status }),
  resetPassword: (id, newPassword) => http.put(`/users/${id}/password/reset`, { newPassword })
}
