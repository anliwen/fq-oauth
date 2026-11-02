<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const page = computed(() => route.meta.portalPage)

const links = [
  { to: '/client/product', label: '了解产品' },
  { to: '/client/guide', label: '开始接入' },
  { to: '/client/cases', label: '查看场景' }
]
</script>

<template>
  <div class="portal">
    <template v-if="page === 'home'">
      <section class="welcome">
        <div class="welcome-copy">
          <span class="eyebrow">MCP 服务接入与授权</span>
          <h2>让服务接入更简单，让访问授权更清晰</h2>
          <p>在统一入口申请和管理 API Key，为 MCP 服务建立清晰、可撤销的访问凭证。</p>
          <div class="actions">
            <RouterLink class="action primary" to="/client/guide">查看接入指南 <span>→</span></RouterLink>
            <RouterLink class="action secondary" to="/my-credentials">管理我的凭证</RouterLink>
          </div>
        </div>
        <div class="welcome-mark" aria-hidden="true">
          <div class="mark-node node-top">CLIENT</div><div class="mark-node node-mid">授权中心</div><div class="mark-node node-bottom">MCP 服务</div>
          <div class="mark-line line-a"></div><div class="mark-line line-b"></div>
        </div>
      </section>

      <section class="section-heading"><div><span class="eyebrow">快速开始</span><h3>从申请到调用，三步完成接入</h3></div></section>
      <div class="steps">
        <article class="step"><span class="step-no">01</span><h4>申请 API Key</h4><p>填写密钥名称与使用用途，提交后进入管理员审核流程。</p><RouterLink to="/my-credentials">前往申请 <span>→</span></RouterLink></article>
        <article class="step"><span class="step-no">02</span><h4>审核并生成</h4><p>审核通过后，在凭证管理页生成密钥。完整密钥只展示一次。</p><RouterLink to="/client/guide">查看详细流程 <span>→</span></RouterLink></article>
        <article class="step"><span class="step-no">03</span><h4>携带密钥调用</h4><p>按照 MCP 服务提供方的接入地址和协议，在请求中携带 Bearer 凭证。</p><RouterLink to="/client/guide">查看请求示例 <span>→</span></RouterLink></article>
      </div>
      <section class="home-bottom"><div><span class="eyebrow">了解更多</span><h3>为服务调用建立可信的身份边界</h3><p>集中管理客户端凭证，掌握申请状态、密钥生命周期与已授权服务。</p></div><div class="quick-links"><RouterLink v-for="item in links" :key="item.to" :to="item.to">{{ item.label }} <span>→</span></RouterLink></div></section>
      <section class="home-capabilities" aria-label="平台能力">
        <article><span class="cap-icon">✓</span><div><b>先审批，再签发</b><p>每个凭证申请都有明确用途与审核状态。</p></div></article>
        <article><span class="cap-icon">↻</span><div><b>密钥可撤销</b><p>不再使用或存在风险时，可及时停用凭证。</p></div></article>
        <article><span class="cap-icon">盾</span><div><b>权限由服务端执行</b><p>服务端按自身策略校验工具、资源和操作权限。</p></div></article>
      </section>
      <div class="security-foot"><span class="security-mark">i</span><p><b>密钥安全提示</b>　API Key 仅在生成时完整展示一次。请存放在服务端密钥管理设施中，不要提交到代码仓库或放入浏览器端代码。</p><RouterLink to="/client/guide">查看安全接入建议 <span>→</span></RouterLink></div>
    </template>

    <template v-else-if="page === 'product'">
      <section class="page-intro"><span class="eyebrow">产品介绍</span><h2>面向 MCP 服务的统一凭证与授权管理</h2><p>为接入服务提供统一的身份凭证入口，让申请、审核、使用和撤销都有迹可循。</p></section>
      <section class="product-flow" aria-label="接入授权流程"><div><span>01</span><b>客户端</b><small>发起凭证申请</small></div><i>→</i><div><span>02</span><b>FQ 授权中心</b><small>审核与生命周期管理</small></div><i>→</i><div><span>03</span><b>MCP 服务</b><small>验证请求凭证</small></div></section>
      <div class="feature-grid">
        <article><span class="feature-icon">01</span><h3>统一申请入口</h3><p>通过客户端账号提交 API Key 申请，填写用途并跟踪审核状态，减少线下凭证流转。</p></article>
        <article><span class="feature-icon">02</span><h3>审核后签发</h3><p>密钥先审批、后生成。完整值只在签发时显示一次，降低未经审核凭证被使用的风险。</p></article>
        <article><span class="feature-icon">03</span><h3>凭证全生命周期</h3><p>查看密钥状态与前缀，随时撤销不再使用的 API Key；授权令牌也可集中查看和撤销。</p></article>
        <article><span class="feature-icon">04</span><h3>职责边界明确</h3><p>授权中心负责身份凭证与审批；MCP 服务负责校验凭证并执行自身的资源级访问策略。</p></article>
      </div>
      <aside class="callout"><b>接入边界</b><p>API Key 用于识别获准接入的客户端。具体可访问哪些工具、数据和操作，仍需由 MCP 服务端按自身策略校验，不能仅凭客户端页面替代服务端鉴权。</p></aside>
    </template>

    <template v-else-if="page === 'guide'">
      <section class="page-intro"><span class="eyebrow">接入指南</span><h2>按步骤完成服务接入</h2><p>先在授权中心取得凭证，再根据目标 MCP 服务提供的地址、协议和工具说明发起请求。</p></section>
      <div class="guide-layout">
        <div class="guide-steps">
          <article><span>1</span><div><h3>提交 API Key 申请</h3><p>进入“我的 API Key”，填写便于识别的名称和真实用途，提交后状态显示为“待审核”。</p><RouterLink to="/my-credentials">打开我的 API Key <b>→</b></RouterLink></div></article>
          <article><span>2</span><div><h3>等待管理员审核</h3><p>管理员审核通过后，申请状态变为“审核通过，待生成”；如被拒绝，可查看审核意见后重新沟通。</p></div></article>
          <article><span>3</span><div><h3>生成并保存密钥</h3><p>点击“生成密钥”，立即复制并保存到客户端的密钥管理设施。关闭弹窗后，系统不会再次展示完整值。</p></div></article>
          <article><span>4</span><div><h3>向 MCP 服务发起请求</h3><p>将密钥放在 HTTPS 请求的 Authorization 标头中。服务地址、请求路径和 MCP 协议版本以服务提供方文档为准。</p></div></article>
        </div>
        <aside class="guide-note"><h3>接入前检查</h3><ul><li>已获得管理员审核通过的 API Key</li><li>已确认目标 MCP 服务的 HTTPS 地址</li><li>已阅读服务方公布的 MCP 协议与工具说明</li><li>密钥存放在服务端密钥管理设施中</li></ul><RouterLink to="/my-credentials" class="action primary">申请或管理密钥 <span>→</span></RouterLink></aside>
      </div>
      <section class="code-section"><div class="code-heading"><div><span class="eyebrow">请求示例</span><h3>通过 Authorization 标头携带凭证</h3></div><span class="code-tag">HTTP · Bearer</span></div><pre><code>curl -X POST "https://&lt;MCP 服务地址&gt;/&lt;服务路径&gt;" \\
  -H "Authorization: Bearer &lt;你的 API Key&gt;" \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'</code></pre><p>请将占位内容替换为服务提供方给出的实际地址与请求格式。不要将 API Key 写入网页前端、公开代码仓库或日志。</p></section>
    </template>

    <template v-else>
      <section class="page-intro"><span class="eyebrow">场景案例</span><h2>让授权管理融入真实服务流程</h2><p>以下为典型接入模式示例，具体工具、数据范围和鉴权策略由 MCP 服务提供方定义。</p></section>
      <div class="case-list">
        <article class="case-item"><div class="case-index">01</div><div class="case-main"><div class="case-meta"><span>企业知识服务</span><span class="case-type">内部 MCP</span></div><h3>让内部应用按审批结果访问知识工具</h3><p>应用团队提交 API Key 申请并说明知识库范围。审核通过后由服务端保存密钥，调用时向知识 MCP 服务出示凭证，服务端再按应用身份校验可访问的知识域。</p><div class="case-tags"><span>客户端凭证</span><span>管理员审批</span><span>服务端策略</span></div></div></article>
        <article class="case-item"><div class="case-index">02</div><div class="case-main"><div class="case-meta"><span>数据分析服务</span><span class="case-type">团队协作</span></div><h3>将报表工具的调用权限交给受控服务</h3><p>数据分析平台作为客户端登记用途、申请凭证。MCP 服务端根据已登记客户端和业务规则限制可调用的报表工具，并在服务侧记录访问审计。</p><div class="case-tags"><span>用途登记</span><span>工具范围</span><span>调用审计</span></div></div></article>
        <article class="case-item"><div class="case-index">03</div><div class="case-main"><div class="case-meta"><span>第三方服务接入</span><span class="case-type">外部集成</span></div><h3>以可撤销凭证降低长期接入风险</h3><p>合作服务在授权中心申请专用 API Key 并由管理员审核。合作终止或密钥疑似泄露时，立即撤销该凭证，并在服务端同步拒绝后续请求。</p><div class="case-tags"><span>独立凭证</span><span>快速撤销</span><span>最小权限</span></div></div></article>
      </div>
      <aside class="callout"><b>落地建议</b><p>按环境、应用或服务拆分凭证；避免多人共用同一密钥；设置服务端限流、审计和轮换机制；发生泄露时先撤销，再重新签发。</p></aside>
    </template>
  </div>
</template>

<style scoped>
.portal{max-width:1180px;margin:0 auto;padding-bottom:36px;color:#172c25}.eyebrow{display:block;color:#12865d;font-size:11px;font-weight:700;letter-spacing:1.3px;text-transform:uppercase}.welcome{min-height:280px;display:grid;grid-template-columns:minmax(0,1fr) 310px;align-items:center;gap:40px;padding:34px 44px;background:#eaf5ef;border:1px solid #d8e9df;border-radius:8px;overflow:hidden}.welcome h2,.page-intro h2{max-width:700px;margin:12px 0;font-size:32px;line-height:1.35;font-weight:700}.welcome p,.page-intro p{max-width:690px;color:#587066;line-height:1.8}.actions{display:flex;flex-wrap:wrap;gap:12px;margin-top:24px}.action{display:inline-flex;align-items:center;gap:18px;min-height:42px;padding:0 16px;border-radius:5px;text-decoration:none;font-weight:600}.action.primary{background:#13845d;color:#fff}.action.secondary{border:1px solid #b9d3c5;color:#176b50;background:#fff}.action span{font-size:18px}.welcome-mark{position:relative;height:196px}.mark-node{position:absolute;z-index:1;display:grid;place-items:center;width:118px;height:54px;background:#fff;border:1px solid #cfe2d6;border-radius:5px;color:#245c45;font-size:12px;font-weight:700}.node-top{top:6px;left:92px}.node-mid{top:72px;left:92px;background:#13845d;border-color:#13845d;color:#fff}.node-bottom{top:138px;left:92px}.mark-line{position:absolute;left:150px;width:1px;height:18px;background:#7db59a}.line-a{top:60px}.line-b{top:126px}.section-heading{display:flex;justify-content:space-between;align-items:end;margin:34px 0 16px}.section-heading h3,.home-bottom h3{margin-top:7px;font-size:20px}.steps{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));border:1px solid #e0e9e3;border-radius:7px;background:#fff}.step{padding:22px 24px;min-height:174px}.step+.step{border-left:1px solid #e7eee9}.step-no{font-family:var(--mono);color:#12865d;font-size:12px;font-weight:700}.step h4{margin:13px 0 7px;font-size:16px}.step p,.home-bottom p{color:#687c73;line-height:1.7}.step a,.guide-steps a{display:inline-block;margin-top:13px;color:#12865d;font-size:13px;font-weight:600;text-decoration:none}.step a span,.guide-steps a b{margin-left:7px}.home-bottom{display:flex;justify-content:space-between;align-items:center;gap:24px;margin-top:24px;padding:20px 0;border-top:1px solid #dfe9e3}.quick-links{display:flex;flex-wrap:wrap;gap:18px}.quick-links a{color:#176b50;text-decoration:none;font-weight:600}.quick-links span{padding-left:6px}.page-intro{padding:16px 0 26px}.page-intro h2{font-size:28px}.product-flow{display:grid;grid-template-columns:1fr 40px 1fr 40px 1fr;align-items:center;padding:22px 26px;background:#eff6f1;border:1px solid #dce9e0;border-radius:7px}.product-flow div{display:grid;grid-template-columns:36px 1fr;gap:3px 10px;align-items:center}.product-flow div>span{grid-row:1/3;color:#13845d;font:700 12px var(--mono)}.product-flow b{font-size:14px}.product-flow small{color:#6c8177}.product-flow i{text-align:center;color:#83a895;font-style:normal}.feature-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;margin-top:20px;background:#e1eae4;border:1px solid #e1eae4}.feature-grid article{min-height:145px;padding:22px;background:#fff}.feature-icon{color:#13845d;font:700 11px var(--mono)}.feature-grid h3{margin:10px 0 7px;font-size:16px}.feature-grid p,.callout p{color:#64796f;line-height:1.8}.callout{margin-top:20px;padding:16px 18px;border-left:3px solid #13845d;background:#f4f8f5}.callout b{display:block;margin-bottom:5px;font-size:13px}.guide-layout{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(250px,.8fr);gap:24px}.guide-steps{display:grid;gap:0}.guide-steps article{display:grid;grid-template-columns:34px 1fr;gap:14px;padding:15px 0;border-bottom:1px solid #e2ebe5}.guide-steps article>span{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;background:#e5f2e9;color:#13845d;font:700 12px var(--mono)}.guide-steps h3{margin:2px 0 6px;font-size:15px}.guide-steps p,.guide-note li{color:#65796f;line-height:1.7}.guide-note{align-self:start;padding:20px;background:#f2f7f3;border:1px solid #e1ebe4;border-radius:6px}.guide-note h3{font-size:15px}.guide-note ul{display:grid;gap:10px;padding:16px 0 18px 18px}.guide-note .action{font-size:13px}.code-section{margin-top:24px;padding:20px;background:#172d26;border-radius:7px;color:#e8f3ec}.code-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}.code-heading h3{margin-top:7px;font-size:16px}.code-tag{color:#a7c7b3;font-size:11px}.code-section pre{overflow:auto;margin:16px 0 12px;padding:16px;background:#10211b;border:1px solid #294137;border-radius:5px;color:#c3e5ce;font:12px/1.8 var(--mono)}.code-section p{color:#abc0b3;font-size:12px;line-height:1.7}.case-list{display:grid;border-top:1px solid #dfe8e2}.case-item{display:grid;grid-template-columns:58px minmax(0,1fr);gap:15px;padding:23px 8px;border-bottom:1px solid #dfe8e2}.case-index{color:#13845d;font:700 14px var(--mono)}.case-meta{display:flex;align-items:center;gap:12px;color:#12865d;font-size:12px;font-weight:700}.case-type{padding:3px 7px;background:#edf4ef;color:#587066;font-size:11px;font-weight:500}.case-main h3{margin:10px 0;font-size:17px}.case-main p{max-width:850px;color:#64796f;line-height:1.8}.case-tags{display:flex;flex-wrap:wrap;gap:8px;margin-top:13px}.case-tags span{padding:4px 8px;border:1px solid #dce7df;border-radius:3px;color:#587066;font-size:11px}
.home-capabilities{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-top:6px}.home-capabilities article{display:flex;align-items:flex-start;gap:12px;min-height:94px;padding:17px;background:#fff;border:1px solid #e0e9e3;border-radius:6px}.cap-icon{display:grid;place-items:center;flex:0 0 26px;height:26px;background:#eaf5ef;border-radius:50%;color:#13845d;font-size:14px;font-weight:700}.home-capabilities b{font-size:13px}.home-capabilities p{margin-top:6px;color:#687c73;font-size:12px;line-height:1.6}.security-foot{display:flex;align-items:center;gap:10px;margin-top:16px;padding:12px 14px;background:#f2f6f3;border:1px solid #e2ebe5;border-radius:5px}.security-mark{display:grid;place-items:center;flex:0 0 20px;height:20px;border:1px solid #9bb9a7;border-radius:50%;color:#507360;font-size:12px}.security-foot p{flex:1;min-width:0;color:#687c73;font-size:12px;line-height:1.6}.security-foot p b{color:#354f42}.security-foot a{flex:0 0 auto;color:#176b50;text-decoration:none;font-size:12px;font-weight:600}.security-foot a span{margin-left:5px}
@media(max-width:760px){.welcome{grid-template-columns:1fr;min-height:auto;padding:26px 22px}.welcome h2{font-size:26px}.welcome-mark{display:none}.steps{grid-template-columns:1fr}.step+.step{border-left:0;border-top:1px solid #e7eee9}.home-bottom{align-items:flex-start;flex-direction:column}.home-capabilities{grid-template-columns:1fr}.security-foot{align-items:flex-start;flex-wrap:wrap}.security-foot p{flex-basis:calc(100% - 34px)}.security-foot a{margin-left:30px}.page-intro h2{font-size:24px}.product-flow{grid-template-columns:1fr;gap:12px}.product-flow i{transform:rotate(90deg);text-align:left;padding-left:12px}.feature-grid{grid-template-columns:1fr}.guide-layout{grid-template-columns:1fr}.case-item{grid-template-columns:34px minmax(0,1fr);gap:8px}.quick-links{gap:12px}}
</style>
