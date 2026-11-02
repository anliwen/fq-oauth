<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { oauthApi } from '../api/oauth'
import { setSession } from '../composables/auth'
import { notify } from '../composables/toast'
import TokenCard from '../components/TokenCard.vue'

const router = useRouter()
const route = useRoute()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')
const showPwd = ref(false)
const githubClientId = import.meta.env.VITE_GITHUB_CLIENT_ID || ''
const githubRedirectUri = `${window.location.origin}/github/callback`

function githubLogin() {
  if (!githubClientId) { error.value = 'GitHub 登录尚未配置，请联系管理员'; return }
  const state = crypto.randomUUID()
  localStorage.setItem('fq_github_state', state)
  localStorage.setItem('fq_github_redirect', typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard')
  const params = new URLSearchParams({ client_id: githubClientId, redirect_uri: githubRedirectUri, scope: 'read:user user:email', state })
  window.location.assign(`https://github.com/login/oauth/authorize?${params}`)
}

async function submit() {
  error.value = ''
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    const r = await authApi.login(form.username.trim(), form.password)
    setSession(r.access_token, r.user, r.permissions)
    const target = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect : '/dashboard'
    if (target.startsWith('/oauth/authorize')) {
      const query = Object.fromEntries(new URL(target, window.location.origin).searchParams.entries())
      const context = await oauthApi.authorize(query)
      const scopes = context.requestedScopes || query.scope?.split(/\s+/).filter(Boolean) || []
      if (context.autoApprove) {
        const callback = await oauthApi.confirm({
          clientId: query.client_id,
          redirectUri: query.redirect_uri,
          state: query.state,
          codeChallenge: query.code_challenge,
          codeChallengeMethod: query.code_challenge_method || 'S256',
          requestedScope: scopes.join(' '),
          scopes,
          approved: true
        })
        window.location.assign(callback)
      } else {
        router.replace({ name: 'consent', query })
      }
    } else {
      router.replace(target)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

const forgot = () => notify.info('请联系管理员重置密码')
</script>

<template>
  <div class="login">
    <div class="brand"><span class="logo">FQ</span><b>FQ OAuth</b></div>

    <div class="stage">
      <h2>让每个 Agent 都有身份<br />让每次授权都可追溯</h2>
      <p class="lead">为人与 AI Agent 提供统一的身份认证、委托授权与审计</p>

      <svg viewBox="0 0 620 340" class="art" aria-hidden="true">
        <defs>
          <radialGradient id="hubg" cx="50%" cy="40%" r="70%">
            <stop offset="0" stop-color="#3fcf8e" /><stop offset="1" stop-color="#0b9b5c" />
          </radialGradient>
          <linearGradient id="ag" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#ffd24d" /><stop offset="1" stop-color="#f5a800" />
          </linearGradient>
        </defs>

        <!-- orbit rings -->
        <ellipse cx="310" cy="170" rx="150" ry="62" class="ring" />
        <ellipse cx="310" cy="170" rx="235" ry="110" class="ring r2" />

        <!-- links: hub <-> nodes -->
        <path id="l1" d="M92,86 Q190,96 310,170" class="flow" />
        <path id="l2" d="M310,170 Q420,90 528,76" class="flow" />
        <path id="l3" d="M310,170 Q440,230 540,262" class="flow" />
        <path id="l4" d="M82,262 Q190,260 310,170" class="flow" />
        <path id="l5" d="M310,170 L310,296" class="flow" />
        <circle r="4" fill="#0b9b5c"><animateMotion dur="3.2s" repeatCount="indefinite"><mpath href="#l1" /></animateMotion></circle>
        <circle r="4" fill="#f5b301"><animateMotion dur="3.6s" repeatCount="indefinite" begin="0.8s"><mpath href="#l2" /></animateMotion></circle>
        <circle r="4" fill="#f5b301"><animateMotion dur="3.4s" repeatCount="indefinite" begin="1.4s"><mpath href="#l3" /></animateMotion></circle>
        <circle r="4" fill="#0b9b5c"><animateMotion dur="3.8s" repeatCount="indefinite" begin="0.3s" keyPoints="1;0" keyTimes="0;1" calcMode="linear"><mpath href="#l4" /></animateMotion></circle>
        <circle r="4" fill="#0b9b5c"><animateMotion dur="2.6s" repeatCount="indefinite" begin="1.1s"><mpath href="#l5" /></animateMotion></circle>

        <g class="edge-label"><rect x="140" y="82" width="56" height="18" rx="9" /><text x="168" y="95">委托</text></g>
        <g class="edge-label"><rect x="410" y="86" width="56" height="18" rx="9" /><text x="438" y="99">授权范围</text></g>
        <g class="edge-label"><rect x="428" y="212" width="72" height="18" rx="9" /><text x="464" y="225">用户同意</text></g>
        <g class="edge-label"><rect x="146" y="226" width="56" height="18" rx="9" /><text x="174" y="239">MCP</text></g>

        <!-- hub -->
        <g class="hub">
          <circle cx="310" cy="170" r="58" fill="#0b9b5c" opacity=".12" class="halo" />
          <circle cx="310" cy="170" r="44" fill="url(#hubg)" />
          <path d="M310,146 l20,8 v14 q0,16 -20,24 q-20,-8 -20,-24 v-14z" fill="#fff" opacity=".95" />
          <path d="M301,170 l7,7 l13,-15" fill="none" stroke="#0b9b5c" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
          <text x="310" y="238" class="hub-t">权限罗盘</text>
        </g>

        <!-- user -->
        <g class="node" style="animation-delay:-1s">
          <circle cx="80" cy="80" r="28" fill="#fff" class="nbg g" />
          <circle cx="80" cy="73" r="7" fill="#0b9b5c" /><path d="M66,96 a14,14 0 0 1 28,0" fill="#0b9b5c" />
          <text x="80" y="124" class="nt">用户 User</text>
        </g>
        <!-- agent 1 -->
        <g class="node" style="animation-delay:-2.2s">
          <circle cx="540" cy="70" r="28" fill="#fff" class="nbg y" />
          <rect x="525" y="61" width="30" height="22" rx="6" fill="url(#ag)" />
          <circle cx="533" cy="72" r="3" fill="#fff" /><circle cx="547" cy="72" r="3" fill="#fff" />
          <path d="M540,61 v-7" stroke="#f5a800" stroke-width="2.4" /><circle cx="540" cy="52" r="3" fill="#f5a800" />
          <text x="540" y="114" class="nt">Agent</text>
        </g>
        <!-- agent 2 -->
        <g class="node" style="animation-delay:-3.4s">
          <circle cx="552" cy="262" r="28" fill="#fff" class="nbg y" />
          <rect x="537" y="253" width="30" height="22" rx="6" fill="url(#ag)" />
          <circle cx="545" cy="264" r="3" fill="#fff" /><circle cx="559" cy="264" r="3" fill="#fff" />
          <path d="M552,253 v-7" stroke="#f5a800" stroke-width="2.4" /><circle cx="552" cy="244" r="3" fill="#f5a800" />
          <text x="552" y="306" class="nt">子 Agent</text>
        </g>
        <!-- mcp tool -->
        <g class="node" style="animation-delay:-0.4s">
          <circle cx="70" cy="262" r="28" fill="#fff" class="nbg g" />
          <rect x="56" y="248" width="12" height="12" rx="3" fill="#0b9b5c" /><rect x="72" y="248" width="12" height="12" rx="3" fill="#3fcf8e" />
          <rect x="56" y="264" width="12" height="12" rx="3" fill="#3fcf8e" /><rect x="72" y="264" width="12" height="12" rx="3" fill="#f5b301" />
          <text x="70" y="306" class="nt">MCP 工具</text>
        </g>
        <!-- policy -->
        <g class="node" style="animation-delay:-2.8s">
          <circle cx="310" cy="304" r="22" fill="#fff" class="nbg g" />
          <rect x="301" y="294" width="18" height="22" rx="3" fill="none" stroke="#0b9b5c" stroke-width="2.4" />
          <path d="M305,301 h10 M305,306 h10 M305,311 h6" stroke="#f5b301" stroke-width="2" stroke-linecap="round" />
          <text x="310" y="338" class="nt">策略 Policy</text>
        </g>
      </svg>

      <div class="below">
        <TokenCard />
        <ul class="chips">
          <li>身份认证</li><li>委托授权</li><li>MCP 工具</li><li>用户同意</li><li>审计追溯</li>
        </ul>
      </div>
    </div>

    <form class="card" novalidate @submit.prevent="submit">
      <div class="head">
        <h1>欢迎回来</h1>
        <p>验证身份 · 校准权限 · 为人与智能体划定可信边界</p>
      </div>

      <div class="grp">
        <label for="lg-user">用户名</label>
        <div class="row">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round"><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></svg>
          <input id="lg-user" v-model="form.username" autocomplete="username" placeholder="请输入用户名" autofocus />
        </div>
      </div>

      <div class="grp">
        <div class="lab">
          <label for="lg-pwd">密码</label>
          <button type="button" class="link" @click="forgot">忘记密码?</button>
        </div>
        <div class="row">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round"><rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></svg>
          <input id="lg-pwd" v-model="form.password" :type="showPwd ? 'text' : 'password'"
                 autocomplete="current-password" placeholder="请输入密码" />
          <button type="button" class="eye" :aria-label="showPwd ? '隐藏密码' : '显示密码'" :aria-pressed="showPwd"
                  @click="showPwd = !showPwd">
            <svg v-if="!showPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
                 stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
                 stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.6 5.1A10.7 10.7 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-3.2 4.2M6.6 6.6C3.5 8.4 2 12 2 12s3.6 7 10 7c1.7 0 3.2-.4 4.5-1" />
              <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M3 3l18 18" />
            </svg>
          </button>
        </div>
      </div>

      <div v-if="error" class="err" role="alert">⚠ {{ error }}</div>

      <button class="go" type="submit" :disabled="loading">
        <i v-if="loading" class="spin"></i>
        <span>{{ loading ? '登录中' : '登 录' }}</span>
        <svg v-if="!loading" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </button>

      <div class="or"><span>或</span></div>
      <button class="github" type="button" :disabled="loading" @click="githubLogin">
        <span class="github-mark">GH</span><span>使用 GitHub 登录</span>
      </button>

    </form>
  </div>
</template>

<style scoped>
.login {
  position: fixed; inset: 0; overflow-x: hidden; overflow-y: auto;
  display: flex; align-items: center; justify-content: space-around; gap: 24px; padding: 0 6vw;
  background:
    radial-gradient(640px 520px at 26% 48%, rgba(255, 255, 255, 0.9) 0%, transparent 72%),
    radial-gradient(520px 420px at 96% 4%, rgba(255, 224, 130, 0.26), transparent 70%),
    radial-gradient(560px 460px at 6% 100%, rgba(11, 155, 92, 0.07), transparent 70%),
    linear-gradient(135deg, #fafdfb 0%, #eef8f2 50%, #e0f3e8 100%);
}

.brand { position: absolute; left: 32px; top: 26px; display: flex; align-items: center; gap: 10px; z-index: 2; }
.brand b { font-size: 17px; letter-spacing: 0.04em; }
.logo {
  width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px;
  font-family: var(--mono); font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #0b9b5c, #f5b301); box-shadow: 0 8px 18px rgba(15, 169, 104, 0.3);
}

.stage { position: relative; z-index: 1; flex: 1; max-width: 640px; padding-top: 28px; }
h2 { font-size: clamp(22px, 3.4vh, 28px); line-height: 1.35; letter-spacing: 0.02em; }
.stage h2 { background: linear-gradient(90deg, #0b9b5c, #19b36f 60%, #c98a00); -webkit-background-clip: text; background-clip: text; color: transparent; }
.lead { margin: 10px 0 6px; color: var(--text-dim); font-size: 14px; }
.art { display: block; width: 100%; height: auto; max-height: 40vh; margin: 0 auto; }
.ring { fill: none; stroke: rgba(15, 169, 104, 0.28); stroke-width: 1.2; stroke-dasharray: 4 6; }
.ring.r2 { stroke: rgba(245, 179, 1, 0.35); }
.flow { fill: none; stroke: rgba(15, 169, 104, 0.4); stroke-width: 1.6; stroke-dasharray: 5 5; animation: dash 1.4s linear infinite; }
@keyframes dash { to { stroke-dashoffset: -20; } }
.edge-label rect { fill: #fff; stroke: rgba(15, 169, 104, 0.3); }
.edge-label text { font-size: 10px; text-anchor: middle; fill: #0b7a49; font-weight: 600; dominant-baseline: middle; transform: translateY(0); }
.nbg { filter: drop-shadow(0 8px 14px rgba(20, 70, 45, 0.16)); stroke-width: 2; }
.nbg.g { stroke: #bfe9d4; } .nbg.y { stroke: #ffe39a; }
.nt { font-size: 12px; text-anchor: middle; fill: var(--text); font-weight: 600; }
.hub-t { font-size: 13px; text-anchor: middle; fill: #0b7a49; font-weight: 700; }
.halo { animation: halo 3s ease-in-out infinite; transform-origin: 310px 170px; }
@keyframes halo { 0%, 100% { transform: scale(1); opacity: .14; } 50% { transform: scale(1.18); opacity: .04; } }
.node { animation: bob 5s ease-in-out infinite; }
@keyframes bob { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-6px); } }
.below { display: flex; align-items: flex-end; gap: 22px; margin-top: 6px; flex-wrap: wrap; }
.chips { list-style: none; display: flex; flex-wrap: wrap; gap: 8px; flex: 1; min-width: 160px; }
.chips li { font-size: 12px; padding: 5px 12px; border-radius: 999px; color: #0b7a49; background: rgba(255, 255, 255, 0.8); border: 1px solid rgba(15, 169, 104, 0.25); }
.chips li:nth-child(even) { color: #8a6200; border-color: rgba(245, 179, 1, 0.45); }

.card {
  position: relative; z-index: 2; width: 420px; max-width: 100%; flex-shrink: 0; overflow: hidden;
  padding: 42px 38px 28px; border-radius: 20px;
  background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 30px 80px rgba(20, 70, 45, 0.16), 0 2px 0 rgba(255, 255, 255, 0.8) inset;
  animation: fadeUp 0.4s ease;
}
.card::before {
  content: ''; position: absolute; left: 0; right: 0; top: 0; height: 4px;
  background: linear-gradient(90deg, #0b9b5c, #3fcf8e 55%, #f5b301);
}
.head { margin-bottom: 30px; }
h1 { font-size: 26px; font-weight: 700; letter-spacing: 0.02em; }
.head p { margin-top: 8px; font-size: 13px; color: var(--text-dim); line-height: 1.6; }

.grp { margin-bottom: 20px; }
.grp label { display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: var(--text); }
.lab { display: flex; align-items: center; justify-content: space-between; }
.lab label { margin-bottom: 8px; }
.lab .link { margin-bottom: 8px; }

.row {
  display: flex; align-items: center; gap: 10px; padding: 0 14px; height: 48px;
  border-radius: 12px; background: #fff; border: 1.5px solid #d9e3de; color: #8aa095;
  transition: border-color 0.18s, background 0.18s, box-shadow 0.18s, color 0.18s;
}
.row:hover { border-color: #b7cbc1; }
.row:focus-within { border-color: var(--cyan); box-shadow: 0 0 0 4px rgba(15, 169, 104, 0.13); color: var(--cyan); }
.row input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; font: inherit; font-size: 15px; color: var(--text); }
.row input::placeholder { color: #a9b8b0; }
.row input:-webkit-autofill,
.row input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 40px #fff inset;
  -webkit-text-fill-color: var(--text);
  caret-color: var(--text);
}
.eye { display: grid; place-items: center; background: none; border: 0; color: #8aa095; cursor: pointer; padding: 4px; border-radius: 6px; }
.eye:hover, .eye:focus-visible { color: var(--cyan); outline: 0; }

.link { background: none; border: 0; font-size: 12px; color: var(--cyan); cursor: pointer; }
.link:hover { text-decoration: underline; }
.err { margin-bottom: 16px; padding: 9px 12px; border-radius: 8px; font-size: 13px; color: var(--red); background: rgba(240, 62, 95, 0.07); }

.go {
  width: 100%; height: 50px; border: 0; border-radius: 12px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  font: inherit; font-size: 16px; font-weight: 600; letter-spacing: 0.3em; color: #fff;
  background: linear-gradient(135deg, #0b9b5c, #1fbf78);
  box-shadow: 0 10px 24px rgba(15, 169, 104, 0.3);
  transition: transform 0.18s, filter 0.18s, box-shadow 0.18s;
}
.go svg { transition: transform 0.18s; }
.go:hover:not(:disabled) { transform: translateY(-1px); filter: brightness(1.06); box-shadow: 0 14px 30px rgba(15, 169, 104, 0.4); }
.go:hover:not(:disabled) svg { transform: translateX(4px); }
.go:active:not(:disabled) { transform: translateY(0); }
.go:disabled { opacity: 0.75; cursor: not-allowed; }
.or { display:flex; align-items:center; gap:12px; margin:18px 0; color:#8aa095; font-size:12px; }
.or::before,.or::after { content:''; height:1px; flex:1; background:#d9e3de; }
.github { width:100%; height:46px; border:1px solid #cfdad4; border-radius:11px; background:#fff; color:#183c28; display:flex; align-items:center; justify-content:center; gap:10px; cursor:pointer; font:inherit; font-weight:600; }
.github:hover:not(:disabled) { background:#f5faf7; border-color:#9eb8aa; }
.github:disabled { opacity:.65; cursor:not-allowed; }
.github-mark { display:grid; place-items:center; width:22px; height:22px; border-radius:50%; background:#18212b; color:#fff; font-size:10px; font-weight:800; }
.spin { width: 16px; height: 16px; border-radius: 50%; border: 2px solid rgba(255, 255, 255, 0.4); border-top-color: #fff; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .stage { display: none; }
  .login { justify-content: center; }
}
</style>
