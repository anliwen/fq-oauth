<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { oauthApi } from '../api/oauth'
import { notify } from '../composables/toast'
import { auth } from '../composables/auth'

const route = useRoute(), router = useRouter()
const data = ref(null), loading = ref(true), submitting = ref(false), error = ref('')
const params = reactive({ clientId: '', redirectUri: '', state: '', codeChallenge: '', codeChallengeMethod: 'S256', requestedScope: '', scopes: [] })
const scopeLabels = { openid: '查看登录身份', profile: '查看基本资料', email: '查看邮箱' }
onMounted(async () => {
  const q = route.query
  params.clientId = typeof q.client_id === 'string' ? q.client_id : ''
  params.redirectUri = typeof q.redirect_uri === 'string' ? q.redirect_uri : ''
  params.state = typeof q.state === 'string' ? q.state : ''
  params.codeChallenge = typeof q.code_challenge === 'string' ? q.code_challenge : ''
  params.codeChallengeMethod = typeof q.code_challenge_method === 'string' ? q.code_challenge_method : ''
  params.requestedScope = typeof q.scope === 'string' ? q.scope : ''
  params.scopes = params.requestedScope.split(/\s+/).filter(Boolean)
  if (!auth.token) { router.replace({ name: 'login', query: { redirect: route.fullPath } }); return }
  try {
    data.value = await oauthApi.authorize({ client_id: params.clientId, redirect_uri: params.redirectUri, state: params.state, code_challenge: params.codeChallenge, code_challenge_method: params.codeChallengeMethod, scope: params.requestedScope })
    params.scopes = data.value.requestedScopes
    params.requestedScope = params.scopes.join(' ')
    if (data.value.autoApprove) await decide(true)
  }
  catch (e) { error.value = e.message; notify.err(e.message) } finally { loading.value = false }
})
async function decide(approved) {
  submitting.value = true
  try { const url = await oauthApi.confirm({ clientId: params.clientId, redirectUri: params.redirectUri, state: params.state, codeChallenge: params.codeChallenge, codeChallengeMethod: params.codeChallengeMethod, requestedScope: params.requestedScope, scopes: params.scopes, approved }); location.assign(url) }
  catch (e) { error.value = e.message } finally { submitting.value = false }
}
</script>

<template>
  <main class="consent-wrap">
    <section class="consent">
      <div class="mark">FQ</div>
      <p class="eyebrow">安全授权</p>
      <h1>{{ data ? data.clientName : '正在验证授权请求' }}</h1>
      <p v-if="data" class="copy">此服务请求访问你的账户信息：</p>
      <div v-if="loading" class="status">正在加载…</div>
      <div v-else-if="error" class="error" role="alert">{{ error }}</div>
      <ul v-else class="scopes"><li v-for="scope in params.scopes" :key="scope"><span class="check">✓</span><span><b>{{ scopeLabels[scope] || scope }}</b><small>{{ scope }}</small></span></li></ul>
      <p v-if="data" class="account">以 <b>{{ auth.user?.fullName || auth.user?.username || '当前账户' }}</b> 身份继续</p>
      <div v-if="data" class="actions"><button class="btn" :disabled="submitting" @click="decide(false)">拒绝</button><button class="btn primary" :disabled="submitting" @click="decide(true)">同意并继续</button></div>
      <p class="foot">授权后可随时在账户授权管理中撤销。</p>
    </section>
  </main>
</template>

<style scoped>
.consent-wrap{min-height:100vh;display:grid;place-items:center;padding:24px;background:#f4f7f5}.consent{width:min(100%,480px);padding:32px;background:white;border:1px solid #dce5df;border-radius:8px;box-shadow:0 12px 36px #183c2812}.mark{width:42px;height:42px;display:grid;place-items:center;background:#078451;color:white;font-weight:800;border-radius:8px}.eyebrow{margin:26px 0 6px;color:#078451;font-size:12px;font-weight:700}.consent h1{font-size:24px;margin:0;overflow-wrap:anywhere}.copy,.account,.foot{color:#64736b;line-height:1.65}.scopes{list-style:none;margin:22px 0;padding:0;border-block:1px solid #e8eeea}.scopes li{display:flex;align-items:center;gap:12px;padding:14px 0}.check{color:#078451;font-size:20px}.scopes small{display:block;color:#7c8982;margin-top:3px}.actions{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}.status,.error{padding:18px 0}.error{color:#b42318}.foot{font-size:12px;margin:20px 0 0}@media(max-width:520px){.consent{padding:24px}.actions{display:grid;grid-template-columns:1fr 1fr}}
</style>
