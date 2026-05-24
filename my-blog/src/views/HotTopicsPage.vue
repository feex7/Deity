<script setup>
import { ref, computed, onMounted } from 'vue'
import { API_BASE } from '../api/config.js'

const API = `${API_BASE}/hot-topics`
const items = ref([])
const loading = ref(true)
const error = ref('')
const sortKey = ref('heat')
const expandedId = ref(null)
const categoryFilter = ref('全部')

const categories = ['全部', 'AI', '开发', '安全', '科技']

const catIcons = { 'AI': '🤖', '开发': '💻', '安全': '🔒', '科技': '🚀' }

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch(`${API}?t=${Date.now()}`)
    items.value = await res.json()
  } catch (e) {
    console.error('加载热点失败:', e)
    error.value = '无法连接到服务器，请确认后端已启动'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const filteredItems = computed(() => {
  const base = categoryFilter.value === '全部'
    ? items.value
    : items.value.filter(i => i.category === categoryFilter.value)
  const arr = [...base]
  if (sortKey.value === 'time') {
    arr.sort((a, b) => (b.date || '').localeCompare(a.date || ''))
  } else {
    arr.sort((a, b) => (b.heatScore || 0) - (a.heatScore || 0))
  }
  return arr
})

const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return items.value.filter(i => i.date === today).length
})

const categoryCounts = computed(() => {
  const counts = {}
  items.value.forEach(i => {
    const c = i.category || '其他'
    counts[c] = (counts[c] || 0) + 1
  })
  return counts
})

const heatColor = (score) => {
  if (score >= 8000) return '#ff4444'
  if (score >= 5000) return '#ffaa22'
  return '#00ff80'
}

const heatLabel = (score) => {
  if (score >= 8000) return '爆'
  if (score >= 5000) return '热'
  return '新'
}

const formatTime = (d) => d || '——'

const platformColor = (platform) => {
  switch (platform) {
    case 'GitHub': return { bg: 'rgba(139,92,246,0.12)', color: '#a78bfa', border: 'rgba(139,92,246,0.25)' }
    case 'ArXiv': return { bg: 'rgba(96,165,250,0.12)', color: '#60a5fa', border: 'rgba(96,165,250,0.25)' }
    case 'HackerNews': return { bg: 'rgba(251,146,60,0.12)', color: '#fb923c', border: 'rgba(251,146,60,0.25)' }
    default: return { bg: 'rgba(255,255,255,0.06)', color: 'rgba(255,255,255,0.5)', border: 'rgba(255,255,255,0.1)' }
  }
}

const refresh = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch(`${API}?t=${Date.now()}`)
    items.value = await res.json()
  } catch (e) {
    error.value = '刷新失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span>多平台聚合 · 每小时更新 · 今日 {{ todayCount }} 条</span>
        </div>
        <h1 class="hero-title">
          <span class="title-icon">&#9733;</span>
          科技热点
        </h1>
        <p class="hero-subtitle">GitHub Trending · Hacker News · ArXiv 论文，一站式聚合</p>
        <button class="refresh-btn" @click="refresh" :disabled="loading">&#8635; 立即刷新</button>
      </div>
      <div class="hero-bg"></div>
    </section>

    <div class="category-bar">
      <button
        v-for="cat in categories" :key="cat"
        class="cat-btn"
        :class="{ active: categoryFilter === cat }"
        @click="categoryFilter = cat"
      >
        <span v-if="catIcons[cat]" class="cat-icon">{{ catIcons[cat] }}</span>
        {{ cat }}
        <span class="cat-count">{{ cat === '全部' ? items.length : (categoryCounts[cat] || 0) }}</span>
      </button>
    </div>

    <div class="toolbar">
      <div class="sort-btns">
        <button class="sort-btn" :class="{ active: sortKey === 'heat' }" @click="sortKey = 'heat'">&#9757; 按热度</button>
        <button class="sort-btn" :class="{ active: sortKey === 'time' }" @click="sortKey = 'time'">&#128339; 按时间</button>
      </div>
      <span class="update-hint">{{ filteredItems.length }} 条结果</span>
    </div>

    <div v-if="loading" class="loading">
      <div v-for="n in 4" :key="n" class="skeleton-card">
        <div class="skeleton skeleton-rank"></div>
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-line"></div>
      </div>
    </div>

    <div v-else-if="error" class="error-state">
      <p class="error-msg">{{ error }}</p>
      <button class="retry-btn" @click="load">重新加载</button>
    </div>

    <div v-else-if="filteredItems.length === 0" class="empty">
      <div class="empty-icon">&#9788;</div>
      <p class="empty-title">暂无匹配热点</p>
      <p class="empty-desc">后端正在抓取 {{ categoryFilter }} 类数据，请稍后再试</p>
    </div>

    <div v-else class="hot-list">
      <article
        v-for="(item, index) in filteredItems"
        :key="item.id"
        class="hot-card"
        :class="{ expanded: expandedId === item.id }"
      >
        <div class="card-main" @click="toggleExpand(item.id)">
          <div class="rank-col">
            <span class="rank-num" :class="{ top: index < 3 }">{{ index + 1 }}</span>
          </div>
          <div class="content-col">
            <div class="card-header">
              <span class="platform-badge" :style="{
                background: platformColor(item.platform).bg,
                color: platformColor(item.platform).color,
                borderColor: platformColor(item.platform).border
              }">{{ item.source || item.platform }}</span>
              <h3 class="card-title">{{ item.title }}</h3>
              <span class="heat-badge" :style="{ background: heatColor(item.heatScore) + '18', color: heatColor(item.heatScore), borderColor: heatColor(item.heatScore) + '30' }">
                <span class="flame">&#9757;</span>
                {{ heatLabel(item.heatScore) }} {{ item.heatScore || 0 }}
              </span>
            </div>
            <p class="card-summary">{{ item.summary }}</p>
            <div class="card-footer">
              <span class="cat-tag">{{ catIcons[item.category] || '' }} {{ item.category }}</span>
              <time class="card-date">{{ formatTime(item.date) }}</time>
              <div class="heat-bar">
                <div class="heat-fill" :style="{ width: Math.min((item.heatScore || 0) / 100, 100) + '%', background: heatColor(item.heatScore) }"></div>
              </div>
            </div>
            <span class="expand-hint">{{ expandedId === item.id ? '收起 ▲' : '展开详情 ▼' }}</span>
          </div>
        </div>
        <transition name="slide">
          <div v-if="expandedId === item.id" class="card-detail">
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">热度指数</span>
                <span class="detail-value" :style="{ color: heatColor(item.heatScore) }">{{ item.heatScore || 0 }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">来源平台</span>
                <a v-if="item.sourceUrl" :href="item.sourceUrl" target="_blank" class="detail-link">{{ item.source || item.platform }} &#8599;</a>
                <span v-else class="detail-value">{{ item.source || item.platform }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">日期 / 分类</span>
                <span class="detail-value">{{ item.date || '——' }} · {{ item.category }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">排名</span>
                <span class="detail-value">#{{ index + 1 }}</span>
              </div>
            </div>
          </div>
        </transition>
      </article>
    </div>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; padding-bottom: 80px; }

.hero {
  position: relative;
  padding: 120px 24px 60px;
  text-align: center;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 0%, rgba(0,255,128,0.06) 0%, transparent 70%),
              radial-gradient(ellipse at 80% 50%, rgba(139,92,246,0.04) 0%, transparent 60%),
              radial-gradient(ellipse at 20% 50%, rgba(59,130,246,0.04) 0%, transparent 60%);
  pointer-events: none;
}
.hero-content { position: relative; z-index: 1; }
.hero-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 14px; border-radius: 20px;
  background: rgba(0,255,128,0.06); border: 1px solid rgba(0,255,128,0.15);
  font-size: 12px; color: rgba(0,255,128,0.7); margin-bottom: 16px;
}
.badge-dot { width: 6px; height: 6px; border-radius: 50%; background: #00ff80; animation: pulse 2s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
.hero-title { font-size: 42px; font-weight: 800; color: #fff; margin: 0 0 12px 0; letter-spacing: -0.5px; }
.title-icon { color: #ffaa22; font-size: 36px; margin-right: 6px; vertical-align: middle; }
.hero-subtitle { font-size: 15px; color: rgba(255,255,255,0.35); margin: 0 0 16px 0; }
.refresh-btn {
  padding: 8px 24px; border: 1px solid rgba(0,255,128,0.25);
  border-radius: 8px; background: rgba(0,255,128,0.08); color: #00ff80;
  font-size: 13px; cursor: pointer; transition: all .2s;
}
.refresh-btn:hover { background: rgba(0,255,128,0.15); }
.refresh-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.category-bar {
  max-width: 820px; margin: 0 auto 20px; padding: 0 24px;
  display: flex; gap: 8px; flex-wrap: wrap; justify-content: center;
}
.cat-btn {
  padding: 7px 14px; border: 1px solid rgba(255,255,255,0.08);
  border-radius: 20px; background: none; color: rgba(255,255,255,0.4);
  font-size: 13px; cursor: pointer; transition: all .2s;
  display: inline-flex; align-items: center; gap: 5px;
}
.cat-btn:hover { border-color: rgba(255,255,255,0.15); color: #fff; }
.cat-btn.active { border-color: rgba(0,255,128,0.3); color: #00ff80; background: rgba(0,255,128,0.06); }
.cat-icon { font-size: 14px; }
.cat-count {
  font-size: 11px; background: rgba(255,255,255,0.06); padding: 1px 6px;
  border-radius: 8px; min-width: 18px; text-align: center;
  color: rgba(255,255,255,0.3);
}
.cat-btn.active .cat-count { background: rgba(0,255,128,0.1); color: #00ff80; }

.toolbar {
  max-width: 820px; margin: 0 auto 32px; padding: 0 24px;
  display: flex; align-items: center; justify-content: space-between;
}
.sort-btns { display: flex; gap: 8px; }
.sort-btn {
  padding: 7px 16px; border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px; background: none; color: rgba(255,255,255,0.4);
  font-size: 13px; cursor: pointer; transition: all .2s;
}
.sort-btn:hover { border-color: rgba(255,255,255,0.15); color: #fff; }
.sort-btn.active { border-color: rgba(0,255,128,0.3); color: #00ff80; background: rgba(0,255,128,0.06); }
.update-hint { font-size: 11px; color: rgba(255,255,255,0.18); }

.loading { max-width: 820px; margin: 0 auto; padding: 0 24px; display: flex; flex-direction: column; gap: 12px; }
.skeleton-card { border: 1px solid rgba(255,255,255,0.05); border-radius: 14px; padding: 24px; display: flex; gap: 16px; align-items: center; }
.skeleton { background: rgba(255,255,255,0.04); border-radius: 6px; animation: shimmer 1.5s infinite; }
@keyframes shimmer { 0%,100% { opacity: .3; } 50% { opacity: .6; } }
.skeleton-rank { width: 40px; height: 40px; border-radius: 10px; flex-shrink: 0; }
.skeleton-title { height: 18px; flex: 1; max-width: 400px; }
.skeleton-line { height: 12px; width: 160px; }

.empty { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 48px; color: rgba(255,255,255,0.08); margin-bottom: 16px; }
.empty-title { color: rgba(255,255,255,0.35); font-size: 16px; margin: 0 0 6px 0; }
.empty-desc { color: rgba(255,255,255,0.2); font-size: 13px; margin: 0; }

.error-state { text-align: center; padding: 80px 0; }
.error-msg { color: rgba(255,255,255,0.4); font-size: 14px; margin-bottom: 16px; }
.retry-btn { padding: 8px 20px; border: 1px solid rgba(0,255,128,0.3); border-radius: 8px; background: transparent; color: #00ff80; font-size: 13px; cursor: pointer; transition: all .2s; }
.retry-btn:hover { background: rgba(0,255,128,0.1); }

.hot-list { max-width: 820px; margin: 0 auto; padding: 0 24px; display: flex; flex-direction: column; gap: 12px; }

.hot-card { background: rgba(255,255,255,0.015); border: 1px solid rgba(255,255,255,0.05); border-radius: 16px; overflow: hidden; transition: border-color .3s, transform .2s; }
.hot-card:hover { border-color: rgba(255,255,255,0.1); }
.hot-card.expanded { border-color: rgba(0,255,128,0.15); }
.card-main { display: flex; gap: 18px; padding: 20px 22px; cursor: pointer; }
.rank-col { flex-shrink: 0; display: flex; align-items: flex-start; padding-top: 4px; }
.rank-num { display: flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 10px; background: rgba(255,255,255,0.04); color: rgba(255,255,255,0.3); font-size: 16px; font-weight: 700; transition: all .3s; }
.rank-num.top { background: rgba(0,255,128,0.08); color: #00ff80; }
.hot-card:nth-child(1) .rank-num.top { background: linear-gradient(135deg, rgba(255,170,34,0.2), rgba(255,100,50,0.15)); color: #ffaa22; }
.hot-card:nth-child(2) .rank-num.top { background: rgba(0,255,128,0.1); color: #00ff80; }
.hot-card:nth-child(3) .rank-num.top { background: rgba(100,149,237,0.1); color: #6495ed; }

.content-col { flex: 1; min-width: 0; }
.card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.platform-badge {
  padding: 3px 8px; border-radius: 4px; border: 1px solid;
  font-size: 10px; font-weight: 600; letter-spacing: 0.5px; white-space: nowrap; flex-shrink: 0;
}
.card-title { font-size: 16px; font-weight: 600; color: #fff; margin: 0; flex: 1; min-width: 0; }
.heat-badge {
  display: inline-flex; align-items: center; gap: 3px; flex-shrink: 0;
  padding: 3px 10px; border-radius: 12px; border: 1px solid;
  font-size: 11px; font-weight: 600; letter-spacing: 0.5px;
}
.flame { font-size: 12px; }
.card-summary { font-size: 13px; color: rgba(255,255,255,0.45); line-height: 1.7; margin: 0 0 12px 0; }
.card-footer { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.cat-tag { font-size: 11px; color: rgba(255,255,255,0.3); }
.card-date { font-size: 11px; color: rgba(255,255,255,0.2); }
.heat-bar { width: 60px; height: 3px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; }
.heat-fill { height: 100%; border-radius: 2px; transition: width 0.6s ease; }

.expand-hint { display: inline-block; margin-top: 10px; font-size: 11px; color: rgba(255,255,255,0.2); transition: color .2s; }
.hot-card:hover .expand-hint { color: rgba(255,255,255,0.35); }

.card-detail { border-top: 1px solid rgba(255,255,255,0.05); padding: 20px 22px 20px 76px; }
.detail-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 11px; color: rgba(255,255,255,0.25); }
.detail-value { font-size: 14px; color: rgba(255,255,255,0.7); font-weight: 500; }
.detail-link { font-size: 14px; color: rgba(0,255,128,0.6); text-decoration: none; font-weight: 500; }
.detail-link:hover { color: #00ff80; }

.slide-enter-active { transition: all .35s ease; }
.slide-leave-active { transition: all .25s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { opacity: 1; max-height: 300px; }

@media (max-width: 768px) {
  .hero-title { font-size: 28px; }
  .detail-grid { grid-template-columns: repeat(2, 1fr); }
  .card-main { flex-direction: column; gap: 12px; }
  .card-detail { padding-left: 22px; }
  .toolbar { flex-direction: column; gap: 8px; align-items: flex-start; }
  .category-bar { justify-content: flex-start; }
}
</style>