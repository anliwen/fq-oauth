import { http } from './http'

export const auditApi = {
  list: params => http.get('/audit-logs?' + new URLSearchParams({
    page: params?.page || 1,
    page_size: params?.pageSize || 20,
    keyword: params?.keyword || '',
    action: params?.action || '',
    result: params?.result || ''
  }))
}
