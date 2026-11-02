import { createRouter, createWebHistory } from 'vue-router'
import UsersView from './views/UsersView.vue'
import RolesView from './views/RolesView.vue'
import PlatformsView from './views/PlatformsView.vue'
import PermissionsView from './views/PermissionsView.vue'
import DeptsView from './views/DeptsView.vue'
import ForbiddenView from './views/ForbiddenView.vue'
import LoginView from './views/LoginView.vue'
import GitHubCallbackView from './views/GitHubCallbackView.vue'
import AgentsView from './views/AgentsView.vue'
import DelegationsView from './views/DelegationsView.vue'
import ConsentView from './views/ConsentView.vue'
import OAuthClientsView from './views/OAuthClientsView.vue'
import MyCredentialsView from './views/MyCredentialsView.vue'
import ApiKeyApplicationsView from './views/ApiKeyApplicationsView.vue'
import ApiKeyManagementView from './views/ApiKeyManagementView.vue'
import AuditLogsView from './views/AuditLogsView.vue'
import ClientPortalView from './views/ClientPortalView.vue'
import DashboardView from './views/DashboardView.vue'
import { applyMe, auth, can } from './composables/auth'
import { authApi } from './api/auth'

// `perm` is the permission code that must be held to open the page: the same code the backend checks.
export const pages = [
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { title: '数据总览', perm: 'dashboard:view' } },
  { path: '/client/home', name: 'client-home', component: ClientPortalView, meta: { title: '首页', perm: 'client:home', portalPage: 'home' } },
  { path: '/client/product', name: 'client-product', component: ClientPortalView, meta: { title: '产品介绍', perm: 'client:product', portalPage: 'product' } },
  { path: '/client/guide', name: 'client-guide', component: ClientPortalView, meta: { title: '接入指南', perm: 'client:guide', portalPage: 'guide' } },
  { path: '/client/cases', name: 'client-cases', component: ClientPortalView, meta: { title: '场景案例', perm: 'client:cases', portalPage: 'cases' } },
  { path: '/platforms', name: 'platforms', component: PlatformsView, meta: { title: '平台管理', perm: 'platform:view' } },
  { path: '/depts', name: 'depts', component: DeptsView, meta: { title: '部门管理', perm: 'dept:view' } },
  { path: '/users', name: 'users', component: UsersView, meta: { title: '用户管理', perm: 'user:view' } },
  { path: '/roles', name: 'roles', component: RolesView, meta: { title: '角色管理', perm: 'role:view' } },
  { path: '/permissions', name: 'permissions', component: PermissionsView, meta: { title: '权限配置', perm: 'permission:view' } },
  { path: '/agents', name: 'agents', component: AgentsView, meta: { title: 'Agent 管理', perm: 'agent:view' } },
  { path: '/delegations', name: 'delegations', component: DelegationsView, meta: { title: '委托授权', perm: 'delegation:view' } },
  { path: '/oauth-clients', name: 'oauth-clients', component: OAuthClientsView, meta: { title: '服务客户端', perm: 'oauth-client:view' } },
  { path: '/api-key-applications', name: 'api-key-applications', component: ApiKeyApplicationsView, meta: { title: 'API Key 审核', perm: 'api-key-review:view' } },
  { path: '/api-key-management', name: 'api-key-management', component: ApiKeyManagementView, meta: { title: 'API Key 管理', perm: 'api-key-management:view' } },
  { path: '/audit-logs', name: 'audit-logs', component: AuditLogsView, meta: { title: '审计日志', perm: 'audit:view' } },
  { path: '/my-credentials', name: 'my-credentials', component: MyCredentialsView, meta: { title: '我的 API Key', perm: 'credential:view' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { title: '登录', public: true } },
    { path: '/github/callback', name: 'github-callback', component: GitHubCallbackView, meta: { title: 'GitHub 登录', public: true } },
    { path: '/consent', name: 'consent', component: ConsentView, meta: { title: '服务授权', public: true } },
    { path: '/403', name: 'forbidden', component: ForbiddenView, meta: { title: '无权限' } },
    ...pages,
    // '/' and unknown paths have no page of their own: the guard below sends them to the first page the user may open
    { path: '/:pathMatch(.*)*', name: 'fallback', component: ForbiddenView, meta: { title: '' } }
  ]
})

const firstAllowed = () => pages.find(p => !p.meta.perm || can(p.meta.perm))

router.beforeEach(async to => {
  if (!auth.token) {
    if (to.meta.public) return true
    return { name: 'login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
  }
  // After a page refresh the token survives but the permission codes do not: fetch them once, before any decision
  // is made, so a logged-in user is never judged by an empty permission list.
  if (auth.permissions === null) {
    try {
      applyMe(await authApi.me())
    } catch {
      // an expired token (401) has already been cleared and sent to the login page; anything else (backend down)
      // also ends up at the login page, where trying again is possible
      return to.name === 'login' ? true : { name: 'login' }
    }
  }
  if (to.name === 'login') return firstAllowed()?.path ?? '/403'
  if (to.name === 'fallback') return firstAllowed()?.path ?? '/403'
  if (to.meta.perm && !can(to.meta.perm)) return firstAllowed()?.path ?? '/403'
  return true
})

export default router
