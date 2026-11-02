<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { setSession } from '../composables/auth'
import { oauthApi } from '../api/oauth'

const route = useRoute(), router = useRouter(), error = ref(''), loading = ref(true)
const redirectUri = `${window.location.origin}/github/callback`

async function continueLogin(token) {
  setSession(token.access_token, token.user, token.permissions)
  const target = localStorage.getItem('fq_github_redirect') || '/dashboard'
  localStorage.removeItem('fq_github_redirect')
  if (!target.startsWith('/oauth/authorize')) return router.replace(target)
  const query = Object.fromEntries(new URL(target, window.location.origin).searchParams.entries())
  const context = await oauthApi.authorize(query)
  const scopes = context.requestedScopes || query.scope?.split(/\s+/).filter(Boolean) || []
  if (!context.autoApprove) return router.replace({ name: 'consent', query })
  const callback = await oauthApi.confirm({ clientId: query.client_id, redirectUri: query.redirect_uri, state: query.state, codeChallenge: query.code_challenge, codeChallengeMethod: query.code_challenge_method || 'S256', requestedScope: scopes.join(' '), scopes, approved: true })
  window.location.assign(callback)
}

onMounted(async () => {
  try {
    const expected = localStorage.getItem('fq_github_state')
    if (!route.query.code || !route.query.state || route.query.state !== expected) throw new Error('GitHub 登录状态无效，请重新发起登录')
    localStorage.removeItem('fq_github_state')
    await continueLogin(await authApi.githubCode(String(route.query.code), redirectUri))
  } catch (e) { error.value = e.message } finally { loading.value = false }
})
</script>
<template><main class="callback"><div><p v-if="loading">正在验证 GitHub 登录...</p><p v-else-if="error" class="error">{{ error }}</p></div></main></template>
<style scoped>.callback{min-height:100vh;display:grid;place-items:center;background:#f4f7f5;color:#183c28}.error{color:#b42318}</style>
