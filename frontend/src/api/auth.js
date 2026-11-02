import { http } from './http'

export const authApi = {
  // Slider captcha is not used here; the field is kept so the request matches alphadesk's contract.
  login: (username, password) =>
    http.post('/auth/token', { username, password, captcha_verify_param: '' }),
  githubCode: (code, redirectUri) =>
    http.post('/auth/federated/github/code', { code, redirectUri }),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me'),
  changePassword: (oldPassword, newPassword) =>
    http.post('/auth/password/change', { old_password: oldPassword, new_password: newPassword })
}
