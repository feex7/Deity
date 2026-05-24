<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { API_BASE } from '../api/config.js'

const stats = ref({ projects: 0, articles: 0, plans: 0 })
const statsLoading = ref(true)

const visitorStats = ref({ todayVisitors: 0, monthVisitors: 0 })
const visitorLoading = ref(true)

const TECH_STACK = [
  { name: 'Vue 3', desc: 'Composition API + 响应式系统', icon: '🟩' },
  { name: 'Vite', desc: '极速HMR开发体验', icon: '⚡' },
  { name: 'Spring Boot', desc: 'JPA + H2 + RESTful API', icon: '🍃' },
  { name: 'Vue Router', desc: 'SPA路由 + 懒加载', icon: '🧭' },
  { name: 'H2 Database', desc: '嵌入式文件数据库', icon: '🗄️' },
  { name: 'Markdown', desc: 'marked + highlight.js', icon: '📝' }
]

const FEATURES = [
  { title: '博客系统', desc: 'Markdown写作、代码高亮、分类标签筛选', icon: '📖' },
  { title: '项目管理', desc: '开源项目展示、链接跳转', icon: '🚀' },
  { title: '时间轴', desc: '自动聚合所有创作记录', icon: '⏳' },
  { title: '音乐播放', desc: '管理员上传管理、后台播放', icon: '🎵' },
  { title: '随笔记录', desc: '标签化管理，随手记录灵感', icon: '✍️' },
  { title: '收藏夹', desc: '链接收藏、分类搜索', icon: '⭐' }
]

const statsData = computed(() => [
  { key: 'projects', label: '开源项目', icon: '🚀', color: '#a78bfa', loading: statsLoading.value },
  { key: 'articles', label: '技术文章', icon: '📝', color: '#4ecdc4', loading: statsLoading.value },
  { key: 'plans', label: '未来计划', icon: '📋', color: '#45b7d1', loading: statsLoading.value }
])

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/stats`)
    if (res.ok) stats.value = await res.json()
  } catch (e) { console.error('统计加载失败:', e) }
  finally { statsLoading.value = false }
}

const fetchVisitorStats = async () => {
  visitorLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/visitors/stats`)
    if (res.ok) visitorStats.value = await res.json()
  } catch (e) { console.error('访问统计加载失败:', e) }
  finally { visitorLoading.value = false }
}

const trackVisitor = () => {
  let visitorId = localStorage.getItem('visitorId')
  if (!visitorId) {
    visitorId = crypto.randomUUID()
    localStorage.setItem('visitorId', visitorId)
  }
  fetch(`${API_BASE}/visitors/track`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ visitorId })
  }).catch(() => {})
}

const numAnim = (n) => {
  if (typeof n !== 'number') return 0
  return n
}

let statsTimer = null
onMounted(() => {
  fetchStats()
  fetchVisitorStats()
  trackVisitor()
  statsTimer = setInterval(() => {
    fetchStats()
    fetchVisitorStats()
  }, 30000)
})
onUnmounted(() => {
  if (statsTimer) clearInterval(statsTimer)
})
</script>

<template>
  <div class="about-page">
    <section class="hero">
      <div class="hero-badge">
        <span class="badge-dot"></span>
        ABOUT
      </div>
      <h1 class="hero-title">
        <span class="title-line">关于本站</span>
      </h1>
      <p class="hero-desc">
        基于 Vue 3 + Spring Boot 构建的个人全栈博客，集文章写作、项目管理、
        时间轴记录、随笔灵感、音乐播放与收藏管理于一体。
      </p>

      <div class="hero-stats">
        <div v-for="s in statsData" :key="s.key" class="stat-item">
          <span v-if="s.loading" class="stat-num skeleton-text">&mdash;</span>
          <span v-else class="stat-num" :style="{ color: s.color }">
            {{ numAnim(stats[s.key]) }}
          </span>
          <span class="stat-label">{{ s.label }}</span>
        </div>
      </div>
    </section>

    <section class="tech-section">
      <h2 class="section-title">技术架构</h2>
      <div class="tech-grid">
        <div v-for="tech in TECH_STACK" :key="tech.name" class="tech-card">
          <span class="tech-icon">{{ tech.icon }}</span>
          <div class="tech-info">
            <span class="tech-name">{{ tech.name }}</span>
            <span class="tech-desc">{{ tech.desc }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="features-section">
      <h2 class="section-title">功能特点</h2>
      <div class="features-grid">
        <div v-for="feat in FEATURES" :key="feat.title" class="feature-card">
          <span class="feature-icon">{{ feat.icon }}</span>
          <h3 class="feature-title">{{ feat.title }}</h3>
          <p class="feature-desc">{{ feat.desc }}</p>
        </div>
      </div>
    </section>

    <section class="visitors-section">
      <h2 class="section-title">访问统计</h2>
      <div class="visitors-grid">
        <div class="visitor-card">
          <div class="visitor-ring">
            <svg width="100" height="100" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="6"/>
              <circle
                cx="50" cy="50" r="42"
                fill="none"
                stroke="#00ff80"
                stroke-width="6"
                stroke-linecap="round"
                stroke-dasharray="264"
                :stroke-dashoffset="264 - (264 * Math.min(visitorStats.todayVisitors / Math.max(visitorStats.todayVisitors + 10, 20), 1))"
                transform="rotate(-90 50 50)"
                style="transition: stroke-dashoffset 0.8s ease"
              />
            </svg>
            <div class="visitor-ring-text">
              <span v-if="visitorLoading" class="visitor-num skeleton-text">&mdash;</span>
              <span v-else class="visitor-num">{{ visitorStats.todayVisitors }}</span>
            </div>
          </div>
          <span class="visitor-label">今日访问</span>
          <span class="visitor-sub">独立访客 (UV)</span>
        </div>
        <div class="visitor-card">
          <div class="visitor-ring">
            <svg width="100" height="100" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="6"/>
              <circle
                cx="50" cy="50" r="42"
                fill="none"
                stroke="#4ecdc4"
                stroke-width="6"
                stroke-linecap="round"
                stroke-dasharray="264"
                :stroke-dashoffset="264 - (264 * Math.min(visitorStats.monthVisitors / Math.max(visitorStats.monthVisitors + 50, 100), 1))"
                transform="rotate(-90 50 50)"
                style="transition: stroke-dashoffset 0.8s ease"
              />
            </svg>
            <div class="visitor-ring-text">
              <span v-if="visitorLoading" class="visitor-num skeleton-text">&mdash;</span>
              <span v-else class="visitor-num">{{ visitorStats.monthVisitors }}</span>
            </div>
          </div>
          <span class="visitor-label">本月访问</span>
          <span class="visitor-sub">独立访客 (UV)</span>
        </div>
      </div>
      <p class="visitors-note">
        <span class="note-dot"></span>
        数据实时更新，同一访客当日多次访问仅计一次
      </p>
    </section>

    <footer class="about-footer">
      <p>Stay hungry, stay foolish.</p>
      <span class="footer-divider">—</span>
      <p class="footer-sub">不断学习，不断创造。</p>
    </footer>
  </div>
</template>

<style scoped>
.about-page {
  min-height: 100vh;
  padding-top: 100px;
  padding-bottom: 80px;
  max-width: 960px;
  margin: 0 auto;
  padding-left: 24px;
  padding-right: 24px;
}

.hero {
  text-align: center;
  margin-bottom: 72px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 16px;
  border: 1px solid rgba(0, 255, 128, 0.2);
  border-radius: 20px;
  color: #00ff80;
  font-size: 12px;
  letter-spacing: 2px;
  margin-bottom: 28px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #00ff80;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.hero-title {
  margin-bottom: 16px;
}

.title-line {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #fff 0%, rgba(255,255,255,0.6) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  color: rgba(255, 255, 255, 0.5);
  font-size: 15px;
  line-height: 1.8;
  max-width: 560px;
  margin: 0 auto 36px;
}

.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #00ff80;
  transition: all 0.3s ease;
}

.skeleton-text {
  color: rgba(255,255,255,0.15);
  animation: skPulse 1.2s ease-in-out infinite;
}

@keyframes skPulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.6; }
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
}

.stat-divider {
  display: none;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #00ff80, #4ecdc4);
  border-radius: 2px;
}

.tech-section {
  margin-bottom: 64px;
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.tech-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 16px;
  transition: all 0.25s ease;
}

.tech-card:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
  transform: translateY(-1px);
}

.tech-icon { font-size: 24px; flex-shrink: 0; }

.tech-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tech-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.tech-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

.features-section {
  margin-bottom: 64px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 20px;
  text-align: center;
  transition: all 0.25s ease;
}

.feature-card:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
  transform: translateY(-2px);
}

.feature-icon { font-size: 28px; display: block; margin-bottom: 10px; }

.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
}

.feature-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin: 0;
  line-height: 1.5;
}

.visitors-section {
  margin-bottom: 64px;
}

.visitors-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.visitor-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 14px;
  padding: 28px 20px;
  text-align: center;
  transition: all 0.25s ease;
}

.visitor-card:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
}

.visitor-ring {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

.visitor-ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.visitor-num {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  transition: all 0.3s ease;
}

.visitor-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.visitor-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 0.5px;
}

.visitors-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
}

.note-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: rgba(0, 255, 128, 0.4);
}

.about-footer {
  text-align: center;
  padding-top: 40px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.footer-divider { color: rgba(0, 255, 128, 0.3); }
.footer-sub { color: rgba(255, 255, 255, 0.2); font-size: 13px; }

@media (max-width: 768px) {
  .about-page { padding-top: 80px; }
  .title-line { font-size: 30px; }
  .hero-desc { font-size: 14px; padding: 0 8px; }
  .hero-stats { gap: 20px; }
  .stat-num { font-size: 24px; }
  .tech-grid,
  .features-grid { grid-template-columns: repeat(2, 1fr); }
  .fav-grid { display: none; }
  .about-footer { flex-direction: column; gap: 6px; }
  .footer-divider { display: none; }
}
</style>