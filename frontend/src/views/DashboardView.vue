<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { agentApi } from '../api/agents'
import { delegationApi } from '../api/delegations'
import { platformApi } from '../api/platforms'
import { roleApi } from '../api/roles'
import { userApi } from '../api/users'
import { oauthApi } from '../api/oauth'
import { apiKeyApplicationsApi } from '../api/credentials'
import { fmtTime } from '../composables/format'

const loading = ref(false)
const updatedAt = ref(null)
const data = ref({})
const issues = ref([])

const sources = [
  ['users', '用户', () => userApi.page({ page: 1, pageSize: 1 })],
  ['activeUsers', '启用用户', () => userApi.page({ page: 1, pageSize: 1, status: 'active' })],
  ['platforms', '平台', () => platformApi.list()],
  ['roles', '角色', () => roleApi.list()],
  ['agents', 'Agent', () => agentApi.list()],
  ['delegations', '委托', () => delegationApi.list()],
  ['clients', '服务客户端', () => oauthApi.clients()],
  ['applications', 'API Key 申请', () => apiKeyApplicationsApi.list()]
]

async function load() {
  loading.value = true
  const results = await Promise.allSettled(sources.map(([, , request]) => request()))
  const next = {}, failed = []
  results.forEach((result, index) => {
    const [key, label] = sources[index]
    if (result.status === 'fulfilled') next[key] = result.value
    else failed.push(label)
  })
  data.value = next
  issues.value = failed
  updatedAt.value = new Date()
  loading.value = false
}
onMounted(load)

const items = key => Array.isArray(data.value[key]) ? data.value[key] : []
const count = key => data.value[key]?.total ?? (Array.isArray(data.value[key]) ? data.value[key].length : null)
const activeAgents = computed(() => items('agents').filter(item => item.status === 'ACTIVE').length)
const activeDelegations = computed(() => items('delegations').filter(item => item.status === 'ACTIVE' && new Date(item.expiresAt) > new Date()).length)
const pendingApplications = computed(() => items('applications').filter(item => item.status === 'PENDING').length)

const statusGroups = computed(() => {
  const rows = items('applications')
  const groups = [
    { key: 'PENDING', label: '待审核', color: '#d28a13' },
    { key: 'APPROVED', label: '审核通过，待生成', color: '#3577b8' },
    { key: 'ACTIVE', label: '使用中', color: '#13845d' },
    { key: 'REJECTED', label: '已拒绝', color: '#d45353' },
    { key: 'REVOKED', label: '已撤销', color: '#87948d' }
  ]
  return groups.map(group => ({ ...group, value: rows.filter(row => row.status === group.key).length }))
})
const statusTotal = computed(() => statusGroups.value.reduce((sum, item) => sum + item.value, 0))
const statusDonut = computed(() => {
  if (!statusTotal.value) return { background: '#e8eeea' }
  let offset = 0
  const stops = statusGroups.value.map(item => {
    const start = offset
    offset += item.value / statusTotal.value * 100
    return `${item.color} ${start}% ${offset}%`
  })
  return { background: `conic-gradient(${stops.join(', ')})` }
})

const trend = computed(() => {
  const now = new Date()
  return Array.from({ length: 7 }, (_, index) => {
    const day = new Date(now)
    day.setHours(0, 0, 0, 0)
    day.setDate(now.getDate() - (6 - index))
    const next = new Date(day)
    next.setDate(day.getDate() + 1)
    const value = items('applications').filter(row => {
      const created = new Date(row.createTime)
      return created >= day && created < next
    }).length
    return { label: `${day.getMonth() + 1}/${day.getDate()}`, value }
  })
})
const maxTrend = computed(() => Math.max(1, ...trend.value.map(item => item.value)))

const activity = computed(() => {
  const records = [
    ...items('applications').map(row => ({ kind: 'API Key 申请', title: row.name, detail: row.fullName || row.username, status: row.status, time: row.createTime })),
    ...items('clients').map(row => ({ kind: '服务客户端', title: row.name, detail: row.clientId, status: row.status, time: row.createTime })),
    ...items('agents').map(row => ({ kind: 'Agent', title: row.name, detail: row.code, status: row.status, time: row.createTime })),
    ...items('delegations').map(row => ({ kind: '委托授权', title: row.resource, detail: `租户 ${row.tenantId}`, status: row.status, time: row.createTime }))
  ]
  return records.filter(row => row.time).sort((a, b) => new Date(b.time) - new Date(a.time)).slice(0, 6)
})

const metrics = computed(() => [
  { label: '用户总数', value: count('users'), hint: data.value.activeUsers ? `启用 ${count('activeUsers')} 人` : '身份目录', tone: 'green' },
  { label: '接入平台', value: count('platforms'), hint: '已登记的平台', tone: 'blue' },
  { label: '服务客户端', value: count('clients'), hint: '由当前管理员创建', tone: 'amber' },
  { label: '待审核申请', value: data.value.applications ? pendingApplications.value : null, hint: 'API Key 审核队列', tone: 'red', attention: pendingApplications.value > 0 }
])

function display(value) { return value === null || value === undefined ? '—' : value }
function statusText(value) {
  return ({ PENDING: '待审核', APPROVED: '待生成', ACTIVE: '有效', REJECTED: '已拒绝', REVOKED: '已撤销', SUSPENDED: '已暂停' })[value] || value || '—'
}
function statusClass(value) { return value === 'ACTIVE' ? 'good' : value === 'PENDING' || value === 'APPROVED' ? 'pending' : 'muted' }
</script>

<template>
  <div class="dashboard">
    <section class="dashboard-hero">
      <div class="hero-copy"><span class="hero-eyebrow"><i></i> 身份与接入治理</span><h2>用户授权中心</h2><p>集中查看身份目录、服务接入与授权审批状态。</p><div class="hero-meta"><span><i class="meta-dot" :class="issues.length ? 'degraded' : 'online'"></i>{{ issues.length ? `${issues.length} 项统计暂不可用` : updatedAt ? '统计数据已更新' : '正在同步数据' }}</span><span v-if="updatedAt">最近更新 {{ fmtTime(updatedAt) }}</span></div></div>
      <div class="hero-actions"><button class="hero-refresh" :disabled="loading" @click="load"><span class="refresh-icon" :class="{ spinning: loading }">↻</span> 刷新数据</button><RouterLink class="hero-review" to="/api-key-applications">处理待审核 <b>{{ display(data.applications ? pendingApplications : '—') }}</b><span>→</span></RouterLink></div>
      <div class="hero-index" aria-hidden="true"><span>FQ</span><i></i><i></i><i></i></div>
    </section>

    <div v-if="issues.length" class="partial-warning" role="status">部分统计暂不可用：{{ issues.join('、') }}。请检查当前账号的数据查看权限。</div>

    <section class="metric-grid" aria-label="核心指标">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="[metric.tone, { attention: metric.attention }]"><span>{{ metric.label }}</span><strong>{{ display(metric.value) }}</strong><small>{{ metric.hint }}</small><i aria-hidden="true"></i></article>
    </section>

    <section class="secondary-metrics" aria-label="治理指标">
      <div><span>角色</span><b>{{ display(count('roles')) }}</b><small>已配置角色</small></div>
      <div><span>已登记 Agent</span><b>{{ display(count('agents')) }}</b><small>其中 {{ activeAgents }} 个有效</small></div>
      <div><span>有效委托</span><b>{{ data.delegations ? activeDelegations : '—' }}</b><small>状态有效且未过期</small></div>
      <div><span>API Key 申请</span><b>{{ display(count('applications')) }}</b><small>全状态申请记录</small></div>
    </section>

    <section class="chart-grid">
      <article class="chart-panel">
        <div class="panel-head"><div><h2>API Key 申请状态</h2><p>审核队列全量状态分布</p></div><RouterLink class="panel-link" to="/api-key-applications">查看申请 <span>→</span></RouterLink></div>
        <div v-if="data.applications" class="status-chart">
          <div class="donut-wrap"><div class="status-donut" :style="statusDonut"><div><strong>{{ statusTotal }}</strong><span>申请总数</span></div></div><span class="donut-marker">状态分布</span></div>
          <div class="status-legend"><div v-for="group in statusGroups" :key="group.key" class="legend-row"><span class="legend-color" :style="{ background: group.color }"></span><span class="legend-name">{{ group.label }}</span><b>{{ group.value }}</b><small>{{ statusTotal ? Math.round(group.value / statusTotal * 100) : 0 }}%</small></div></div>
        </div>
        <div v-else class="chart-empty">当前账号无权查看申请统计</div>
      </article>

      <article class="chart-panel trend-panel">
        <div class="panel-head"><div><h2>近 7 天申请趋势</h2><p>按申请创建日期统计</p></div><span class="trend-total">{{ trend.reduce((sum, item) => sum + item.value, 0) }} <small>项</small></span></div>
        <div v-if="data.applications" class="trend-chart">
          <div v-for="(item, index) in trend" :key="item.label" class="trend-column" :class="{ today: index === trend.length - 1 }"><span class="trend-value">{{ item.value }}</span><div class="trend-bar-area"><i :style="{ height: `${item.value ? Math.max(8, item.value / maxTrend * 100) : 2}%` }"></i></div><span class="trend-label">{{ item.label }}</span></div>
        </div>
        <div v-else class="chart-empty">当前账号无权查看申请趋势</div>
      </article>
    </section>

    <section class="activity-panel">
      <div class="panel-head"><div><h2>近期接入与授权</h2><p>按创建时间排列，不代表登录或工具调用审计</p></div></div>
      <div v-if="activity.length" class="activity-table-wrap"><table class="activity-table"><thead><tr><th>类型</th><th>名称</th><th>关联标识</th><th>状态</th><th>创建时间</th></tr></thead><tbody><tr v-for="(row, index) in activity" :key="`${row.kind}-${row.title}-${index}`"><td><span class="kind-label">{{ row.kind }}</span></td><td class="activity-title">{{ row.title || '未命名' }}</td><td class="activity-detail">{{ row.detail || '—' }}</td><td><span class="status-label" :class="statusClass(row.status)">{{ statusText(row.status) }}</span></td><td class="activity-time">{{ fmtTime(row.time) }}</td></tr></tbody></table></div>
      <div v-else class="activity-empty">{{ loading ? '正在加载数据…' : '暂无可展示的近期记录' }}</div>
    </section>
    <p class="scope-note">统计口径：用户总数取分页接口总量；平台、角色和服务客户端按当前账号可见范围统计。当前系统未提供登录审计或工具调用日志，因此本页不展示调用量指标。</p>
  </div>
</template>

<style scoped>
.dashboard{max-width:1440px;margin:0 auto;padding-bottom:30px;color:#182a42;animation:dash-enter .45s ease both}
.dashboard-hero{position:relative;isolation:isolate;display:flex;align-items:center;justify-content:space-between;gap:32px;min-height:198px;margin-bottom:18px;padding:30px 34px;overflow:hidden;border:1px solid #087f79;border-radius:8px;background:radial-gradient(ellipse at 79% 2%,#ffd16670 0,transparent 28%),radial-gradient(ellipse at 18% 120%,#31d2ae 0,transparent 38%),linear-gradient(112deg,#087c83 0%,#0c9b80 54%,#2ba777 100%);color:#fff;box-shadow:0 16px 34px #087f7930}
.dashboard-hero::before{content:'';position:absolute;inset:0;z-index:-1;opacity:.2;background-image:linear-gradient(#eafff81c 1px,transparent 1px),linear-gradient(90deg,#eafff81c 1px,transparent 1px);background-size:34px 34px;mask-image:linear-gradient(90deg,transparent 8%,#000 100%)}
.dashboard-hero::after{content:'';position:absolute;z-index:-1;width:310px;height:190px;right:18%;top:-95px;border:1px solid #ffecb54d;transform:rotate(-24deg);box-shadow:0 0 0 20px #ffecb50b,0 0 0 42px #ffecb508}
.hero-copy,.hero-actions{position:relative;z-index:1}.hero-eyebrow{display:flex;align-items:center;gap:9px;color:#d4fff1;font-size:10px;font-weight:700}.hero-eyebrow i,.meta-dot{width:7px;height:7px;border-radius:50%;background:#ffdc78}.hero-eyebrow i{box-shadow:0 0 0 4px #ffdc7828,0 0 16px #fff0a8aa}.hero-copy h2{margin:13px 0 7px;font-size:27px;font-weight:700;color:#fff}.hero-copy>p{color:#ddfff3;font-size:12px}.hero-meta{display:flex;flex-wrap:wrap;gap:16px;margin-top:19px;color:#d0f5e8;font-size:10px}.hero-meta span{display:flex;align-items:center;gap:7px}.meta-dot.online{background:#8ff3cb;box-shadow:0 0 0 3px #8ff3cb35,0 0 10px #8ff3cbaa}.meta-dot.degraded{background:#ffcf74;box-shadow:0 0 0 3px #ffcf7435}.hero-actions{display:flex;flex-direction:column;align-items:stretch;gap:9px;min-width:164px}.hero-refresh,.hero-review{display:flex;align-items:center;justify-content:center;gap:8px;min-height:40px;padding:0 14px;border:1px solid #b0ecda;border-radius:5px;color:#f7fffc;background:#087c76aa;font:600 11px var(--sans);text-decoration:none;cursor:pointer;transition:transform .18s,box-shadow .18s,background .18s}.hero-refresh:hover:not(:disabled){background:#08716d;transform:translateY(-2px);box-shadow:0 8px 18px #075b5050}.hero-review{justify-content:space-between;border-color:#ffe19a;background:linear-gradient(110deg,#ffe38b,#ffb979);color:#59421b;box-shadow:0 8px 22px #7c501a26}.hero-review:hover{transform:translateY(-2px);box-shadow:0 12px 24px #7c501a45}.hero-review b{font:700 12px var(--mono)}.hero-review span{font-size:15px}.hero-refresh:disabled{opacity:.6;cursor:wait}.spinning{display:inline-block;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
.hero-index{position:absolute;right:235px;top:-33px;display:flex;align-items:flex-end;gap:9px;opacity:.27;filter:drop-shadow(0 0 14px #ffecb530)}.hero-index span{color:#d3fff0;font:700 116px/1 var(--mono)}.hero-index i{width:9px;height:56px;margin-bottom:8px;border-radius:4px;background:linear-gradient(#ffe28a,#8cf1ce)}.hero-index i:nth-of-type(2){height:88px}.hero-index i:nth-of-type(3){height:69px}
.partial-warning{margin-bottom:15px;padding:11px 14px;background:#fff6e8;border:1px solid #f2d49c;border-radius:5px;color:#80520e;font-size:12px;line-height:1.6}
.metric-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}
.metric-card{--tone:#19aa91;--tone-soft:#e8faf5;position:relative;isolation:isolate;display:flex;flex-direction:column;gap:9px;min-height:139px;padding:19px 21px;background:linear-gradient(145deg,#fff 45%,var(--tone-soft));border:1px solid #e0e7f0;border-radius:7px;overflow:hidden;box-shadow:0 5px 15px #20385809;transition:transform .2s,box-shadow .2s,border-color .2s;animation:dash-enter .45s ease both}
.metric-card:nth-child(2){animation-delay:.05s}.metric-card:nth-child(3){animation-delay:.1s}.metric-card:nth-child(4){animation-delay:.15s}.metric-card:hover{transform:translateY(-3px);border-color:color-mix(in srgb,var(--tone),white 52%);box-shadow:0 12px 24px #20385814}
.metric-card::after{content:'';position:absolute;z-index:-1;right:-27px;bottom:-46px;width:118px;height:118px;border:1px solid color-mix(in srgb,var(--tone),white 65%);border-radius:50%;box-shadow:0 0 0 13px color-mix(in srgb,var(--tone),transparent 94%),0 0 0 28px color-mix(in srgb,var(--tone),transparent 97%)}
.metric-card.green{--tone:#0e9d84;--tone-soft:#e7faf4}.metric-card.blue{--tone:#4286d7;--tone-soft:#edf5ff}.metric-card.amber{--tone:#e39732;--tone-soft:#fff6e8}.metric-card.red{--tone:#e26b78;--tone-soft:#fff0f1}.metric-card>span{color:#718099;font-size:12px;font-weight:600}.metric-card strong{color:#192d49;font:700 32px/1 var(--mono)}.metric-card small{color:#8290a4;font-size:11px}.metric-card i{position:absolute;right:19px;top:21px;width:9px;height:9px;border:2px solid var(--tone);border-radius:50%;opacity:.9;box-shadow:0 0 0 4px color-mix(in srgb,var(--tone),transparent 90%)}.metric-card.attention{border-color:#f0c2c7;background:linear-gradient(145deg,#fff 45%,#fff0f1)}.metric-card.attention i{background:var(--tone);box-shadow:0 0 0 5px #e26b7824,0 0 14px #e26b7870}
.secondary-metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));margin-top:14px;background:linear-gradient(110deg,#f4f8ff,#fff 45%,#f2fbfa);border:1px solid #dfe8f1;border-radius:7px;box-shadow:0 5px 16px #20385808}.secondary-metrics>div{display:grid;grid-template-columns:1fr auto;gap:5px 12px;padding:15px 18px}.secondary-metrics>div+div{border-left:1px solid #e2eaf2}.secondary-metrics span{color:#75839a;font-size:11px}.secondary-metrics b{grid-column:2;grid-row:1/3;align-self:center;color:#253d5d;font:700 21px var(--mono)}.secondary-metrics small{color:#8d9aab;font-size:10px}
.chart-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:14px}.chart-panel,.activity-panel{min-width:0;padding:19px 21px;background:linear-gradient(155deg,#fff,#fbfdff);border:1px solid #dfe7f0;border-radius:7px;box-shadow:0 7px 20px #2038580a}.panel-head{display:flex;align-items:center;justify-content:space-between;gap:16px}.panel-head h2{font-size:14px;font-weight:700;color:#203650}.panel-head p{margin-top:5px;color:#8795a9;font-size:11px}.trend-total{color:#168f91;font:700 22px var(--mono)}.trend-total small{color:#8290a4;font:400 11px var(--sans)}
.status-chart{display:grid;grid-template-columns:180px minmax(0,1fr);align-items:center;gap:20px;min-height:198px;margin-top:6px}.donut-wrap{position:relative;display:grid;place-items:center}.status-donut{display:grid;place-items:center;width:154px;aspect-ratio:1;border-radius:50%;position:relative;filter:drop-shadow(0 7px 10px #183b5525);animation:donut-in .7s cubic-bezier(.2,.8,.2,1) both}.status-donut::before{content:'';position:absolute;inset:15px;background:linear-gradient(145deg,#fff,#f5f9fc);border:1px solid #e5ecf3;border-radius:50%}.status-donut>div{position:relative;display:grid;justify-items:center;gap:3px}.status-donut strong{color:#203854;font:700 25px var(--mono)}.status-donut span{color:#8492a5;font-size:10px}.donut-marker{position:absolute;left:0;right:0;bottom:-21px;text-align:center;color:#98a4b3;font-size:10px}.status-legend{display:grid;gap:11px}.legend-row{display:grid;grid-template-columns:8px minmax(0,1fr) 26px 34px;align-items:center;gap:8px}.legend-color{width:8px;height:8px;border-radius:2px;box-shadow:0 0 0 3px color-mix(in srgb,currentColor,transparent 91%)}.legend-name{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#66788f;font-size:10px}.legend-row b{text-align:right;color:#354b66;font:600 11px var(--mono)}.legend-row small{text-align:right;color:#8997a8;font:10px var(--mono)}.panel-link{color:#078c83;text-decoration:none;font-size:11px;font-weight:700;white-space:nowrap}.panel-link:hover{color:#0aaca0}.panel-link span{margin-left:5px}
.trend-panel{background:radial-gradient(ellipse at 50% 100%,#e9f9f6 0,transparent 53%),linear-gradient(155deg,#fff,#f9fcff)}.trend-chart{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:10px;height:160px;margin-top:12px;padding-top:8px;border-bottom:1px solid #dce6ee}.trend-column{display:grid;grid-template-rows:16px 1fr 20px;justify-items:center;min-width:0}.trend-value{color:#148e8d;font:600 10px var(--mono)}.trend-bar-area{display:flex;align-items:flex-end;width:100%;max-width:30px;height:100%;background:repeating-linear-gradient(to top,transparent 0,transparent 31px,#eaf0f4 32px)}.trend-bar-area i{display:block;width:100%;min-height:2px;background:linear-gradient(to top,#18a890,#75e1c5);border-radius:4px 4px 0 0;box-shadow:0 0 12px #37c5a54a;transition:height .5s cubic-bezier(.2,.8,.2,1);animation:bar-in .65s cubic-bezier(.2,.8,.2,1) both}.trend-column.today .trend-bar-area i{background:linear-gradient(to top,#e17b5d,#ffbe77);box-shadow:0 0 14px #ed9c6b66}.trend-column.today .trend-value,.trend-column.today .trend-label{color:#ce754d;font-weight:700}.trend-label{align-self:end;color:#7e8da1;font:10px var(--mono)}
.chart-empty,.activity-empty{display:grid;place-items:center;min-height:150px;color:#8996a7;font-size:12px}.activity-panel{margin-top:14px}.activity-table-wrap{overflow:auto;margin-top:14px}.activity-table{width:100%;border-collapse:collapse;white-space:nowrap}.activity-table th{padding:10px;background:#f1f6fa;border-block:1px solid #e2eaf1;text-align:left;color:#78899f;font-size:10px;font-weight:700}.activity-table td{padding:12px 10px;border-bottom:1px solid #edf1f5;font-size:11px}.activity-table tbody tr{transition:background .15s}.activity-table tbody tr:hover{background:#f2f9fa}.activity-table tr:last-child td{border-bottom:0}.kind-label{display:inline-flex;padding:4px 7px;border-radius:3px;background:#eaf6f5;color:#27887f;font-size:10px}.activity-title{max-width:230px;overflow:hidden;text-overflow:ellipsis;font-weight:600;color:#2a405d}.activity-detail,.activity-time{color:#7b899d;font-family:var(--mono);font-size:10px!important}.status-label{display:inline-block;padding:4px 8px;border-radius:3px;background:#eff2f6;color:#728096;font-size:10px}.status-label.good{background:#e4f7f0;color:#138266}.status-label.pending{background:#fff1df;color:#a56a17}.scope-note{margin-top:12px;color:#8794a6;font-size:10px;line-height:1.7}
@keyframes dash-enter{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}@keyframes donut-in{from{opacity:.5;transform:rotate(-35deg) scale(.88)}to{opacity:1;transform:rotate(0) scale(1)}}@keyframes bar-in{from{transform:scaleY(.15);transform-origin:bottom}to{transform:scaleY(1);transform-origin:bottom}}
@media(max-width:960px){.metric-grid,.secondary-metrics{grid-template-columns:repeat(2,minmax(0,1fr))}.secondary-metrics>div:nth-child(3){border-left:0;border-top:1px solid #e2eaf2}.secondary-metrics>div:nth-child(4){border-top:1px solid #e2eaf2}.chart-grid{grid-template-columns:1fr}.hero-index{right:180px}}
@media(max-width:600px){.dashboard-hero{align-items:flex-start;flex-direction:column;gap:18px;padding:23px 20px}.hero-copy h2{font-size:23px}.hero-meta{gap:8px 12px}.hero-actions{width:100%;flex-direction:row}.hero-actions>*{flex:1}.hero-index{right:20px;top:-18px}.hero-index span{font-size:78px}.hero-index i{height:38px}.hero-index i:nth-of-type(2){height:58px}.hero-index i:nth-of-type(3){height:48px}.metric-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.metric-card{min-height:117px;padding:14px}.metric-card strong{font-size:25px}.secondary-metrics>div{padding:12px}.chart-panel,.activity-panel{padding:15px 12px}.status-chart{grid-template-columns:130px minmax(0,1fr);gap:10px}.status-donut{width:118px}.status-donut::before{inset:12px}.status-donut strong{font-size:20px}.legend-row{grid-template-columns:7px minmax(0,1fr) 22px 28px;gap:5px}.legend-name{font-size:9px}.legend-row small{font-size:9px}.trend-chart{gap:4px}.trend-bar-area{max-width:24px}}
@media(prefers-reduced-motion:reduce){.dashboard,.metric-card,.status-donut,.trend-bar-area i,.spinning{animation:none!important;transition:none!important}}
</style>
