<script setup>
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { computed, ref } from 'vue'
import ToastHost from './components/ToastHost.vue'
import { auth, can, clearSession } from './composables/auth'
import { authApi } from './api/auth'

const route = useRoute()
const router = useRouter()
const title = computed(() => route.meta.title || '')
const isLogin = computed(() => !!route.meta.public)

// Until the first navigation is decided the route is empty; drawing the shell then would flash an empty layout.
const ready = ref(false)
router.isReady().finally(() => { ready.value = true })

const displayName = computed(() => auth.user?.fullName || auth.user?.username || '')
async function logout() {
  try { await authApi.logout() } catch { /* token may already be invalid */ }
  clearSession()
  router.replace('/login')
}

const allNav = [
  { to: '/dashboard', perm: 'dashboard:view', label: '数据总览', icon: 'M3 3v18h18M7 14l4-4 4 3 5-7' },
  { to: '/client/home', perm: 'client:home', label: '首页', icon: 'M3 10.5 12 3l9 7.5M5 9v12h14V9M9 21v-7h6v7' },
  { to: '/client/product', perm: 'client:product', label: '产品介绍', icon: 'M4 4h16v16H4zM8 8h8M8 12h8M8 16h5' },
  { to: '/client/guide', perm: 'client:guide', label: '接入指南', icon: 'M5 4h14v16H5zM8 8h8M8 12h8M8 16h5' },
  { to: '/client/cases', perm: 'client:cases', label: '场景案例', icon: 'M4 19V5m0 14h16M8 15l3-4 3 2 5-7' },
  { to: '/platforms', perm: 'platform:view', label: '平台管理', icon: 'M3 7l9-4 9 4-9 4-9-4Zm0 5l9 4 9-4M3 17l9 4 9-4' },
  { to: '/depts', perm: 'dept:view', label: '部门管理', icon: 'M3 21h18M5 21V8l7-4 7 4v13M9 21v-6h6v6M9 11h.01M15 11h.01' },
  { to: '/users', perm: 'user:view', label: '用户管理', icon: 'M16 11a4 4 0 1 0-8 0 4 4 0 0 0 8 0Zm-12 9a8 8 0 0 1 16 0' },
  { to: '/roles', perm: 'role:view', label: '角色管理', icon: 'M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6l8-3Z' },
  { to: '/permissions', perm: 'permission:view', label: '权限配置', icon: 'M12 2v6m0 0a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm-7 14h14M5 22h14' },
  { to: '/agents', perm: 'agent:view', label: 'Agent 管理', icon: 'M12 3a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm-6 18v-2a6 6 0 0 1 12 0v2M5 12h14M8 9v6m8-6v6' },
  { to: '/delegations', perm: 'delegation:view', label: '委托授权', icon: 'M7 7h10m-7-4 4 4-4 4M17 17H7m7 4-4-4 4-4' },
  { to: '/oauth-clients', perm: 'oauth-client:view', label: '服务客户端', icon: 'M4 6h16v12H4zM8 10h8M8 14h5' },
  { to: '/api-key-applications', perm: 'api-key-review:view', label: 'API Key 审核', icon: 'M12 2v6m0 0a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm-7 14h14M5 22h14' },
  { to: '/api-key-management', perm: 'api-key-management:view', label: 'API Key 管理', icon: 'M4 6h16v12H4zM8 10h8M8 14h5' },
  { to: '/audit-logs', perm: 'audit:view', label: '审计日志', icon: 'M4 5h16v14H4zM8 9h8M8 13h5M8 17h3' },
  { to: '/my-credentials', perm: 'credential:view', label: '我的 API Key', icon: 'M7 14a5 5 0 1 1 3.5-8.6L21 16v4h-4v-3h-3v-3h-3.4A5 5 0 0 1 7 14Z' }
]
// an entry shows only when the user holds the permission code of that page
const nav = computed(() => {
  return allNav.filter(n => !n.perm || can(n.perm))
})
</script>

<template>
  <template v-if="!ready"></template>
  <RouterView v-else-if="isLogin" />
  <template v-else>
  <div class="bg-blob b1"></div>
  <div class="bg-blob b2"></div>
  <div class="bg-grid"></div>

  <div class="shell">
    <aside class="side">
      <div class="brand">
        <div class="logo">FQ</div>
        <div>
          <div class="brand-name">FQ OAuth</div>
          <div class="brand-sub">用户授权中心</div>
        </div>
      </div>

      <nav class="nav-scroll">
        <RouterLink v-for="n in nav" :key="n.to" :to="n.to" class="nav-item">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
               stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path :d="n.icon" /></svg>
          <span>{{ n.label }}</span>
        </RouterLink>
      </nav>

      <div class="side-foot">
        <div class="me" v-if="displayName">
          <span class="avatar">{{ displayName.slice(0, 1).toUpperCase() }}</span>
          <span class="who">{{ displayName }}</span>
          <button class="out" type="button" @click="logout">退出</button>
        </div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <h1>{{ title }}</h1>
      </header>
      <section class="content">
        <RouterView />
      </section>
    </main>
  </div>

  </template>

  <ToastHost />
</template>

<style scoped>
.bg-blob { position: fixed; z-index: 0; border-radius: 50%; filter: blur(80px); pointer-events: none; }
.b1 { width: 520px; height: 520px; top: -160px; right: -80px; background: rgba(245, 179, 1, 0.22); }
.b2 { width: 480px; height: 480px; bottom: -180px; left: 120px; background: rgba(15, 169, 104, 0.2); }
.bg-grid {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background-image:
    linear-gradient(rgba(15, 169, 104, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 169, 104, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at 60% 20%, #000 20%, transparent 75%);
}

.shell { position: relative; z-index: 2; display: flex; height: 100%; }

.side {
  width: 232px; flex-shrink: 0; display: flex; flex-direction: column;
  height: 100%; min-height: 0;
  padding: 24px 16px; background: rgba(255, 255, 255, 0.72); backdrop-filter: blur(16px);
  border-right: 1px solid var(--line);
}
.brand { display: flex; align-items: center; gap: 12px; padding: 0 8px 28px; }
.logo {
  width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px;
  font-family: var(--mono); font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #0b9b5c, #f5b301); box-shadow: 0 8px 20px rgba(15, 169, 104, 0.35);
}
.brand-name { font-weight: 700; font-size: 16px; }
.brand-sub { font-size: 12px; color: var(--text-dim); margin-top: 2px; }

.nav-scroll {
  display: flex; flex: 1 1 0; flex-direction: column; gap: 4px;
  min-height: 0; overflow-y: auto; padding-right: 4px;
}
.nav-item {
  display: flex; align-items: center; gap: 12px; padding: 11px 14px; border-radius: 10px;
  color: var(--text-dim); text-decoration: none; font-weight: 500; transition: all 0.18s;
}
.nav-item:hover { color: var(--cyan); background: rgba(15, 169, 104, 0.06); }
.nav-item.router-link-active {
  color: #fff; background: linear-gradient(135deg, #0b9b5c, #2fbf71);
  box-shadow: 0 8px 20px rgba(15, 169, 104, 0.3);
}

.side-foot { flex-shrink: 0; margin-top: auto; padding: 0 8px; font-size: 12px; color: var(--text-dim); }
.ver { margin-top: 4px; opacity: 0.7; }
.me { display: flex; align-items: center; gap: 10px; padding: 10px; margin-bottom: 14px; border-radius: 10px; background: rgba(15, 169, 104, 0.07); }
.avatar { width: 30px; height: 30px; flex-shrink: 0; display: grid; place-items: center; border-radius: 50%; color: #fff; font-weight: 700; background: linear-gradient(135deg, #0b9b5c, #f5b301); }
.who { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text); font-weight: 600; }
.out { background: none; border: 0; color: var(--red); cursor: pointer; font-size: 12px; }
.led {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 8px;
  background: var(--green); box-shadow: 0 0 0 4px rgba(15, 169, 104, 0.18); animation: pulse 2s infinite;
}

.main { flex: 1; min-width: 0; min-height: 0; display: flex; flex-direction: column; }
.topbar {
  height: 64px; flex-shrink: 0; display: flex; align-items: center; justify-content: space-between;
  padding: 0 32px;
}
h1 { font-size: 20px; font-weight: 700; }
.chip {
  font-size: 12px; color: var(--text-dim); padding: 5px 12px; border-radius: 999px;
  background: #fff; border: 1px solid var(--line);
}
.content { flex: 1; min-height: 0; overflow: auto; padding: 4px 32px 32px; }
</style>
