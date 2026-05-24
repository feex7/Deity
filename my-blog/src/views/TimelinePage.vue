<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { API_BASE } from '../api/config.js'

const router = useRouter()
const items = ref([])
const loading = ref(true)
const error = ref('')
const typeFilter = ref('ALL')

const typeConfig = {
  ARTICLE: { label: '文章', icon: '📝', color: '#4ecdc4', bg: 'rgba(78,205,196,0.12)' },
  PLAN:    { label: '计划', icon: '📋', color: '#45b7d1', bg: 'rgba(69,183,209,0.12)' },
  ESSAY:   { label: '随笔', icon: '✍️', color: '#f9ca24', bg: 'rgba(249,202,36,0.12)' },
  PROJECT: { label: '项目', icon: '🚀', color: '#a78bfa', bg: 'rgba(167,139,250,0.12)' }
}

const typeList = [
  { value: 'ALL', label: '全部' },
  { value: 'ARTICLE', label: '文章' },
  { value: 'PLAN', label: '计划' },
  { value: 'ESSAY', label: '随笔' },
  { value: 'PROJECT', label: '项目' }
]

const timelineItems = computed(() => {
  if (typeFilter.value === 'ALL') return items.value
  return items.value.filter(i => i.type === typeFilter.value)
})

const typeCounts = computed(() => {
  const counts = { ALL: items.value.length }
  items.value.forEach(i => {
    counts[i.type] = (counts[i.type] || 0) + 1
  })
  return counts
})

const groupedByYear = computed(() => {
  const groups = []
  timelineItems.value.forEach(item => {
    if (!item.date) return
    const year = item.date.substring(0, 4)
    let group = groups.find(g => g.year === year)
    if (!group) {
      group = { year, items: [] }
      groups.push(group)
    }
    group.items.push(item)
  })
  return groups
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  if (parts.length !== 3) return dateStr
  const month = parseInt(parts[1], 10)
  const day = parseInt(parts[2], 10)
  return `${month}月${day}日`
}

const goTo = (link) => {
  if (link) router.push(link)
}

const fetchTimeline = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch(`${API_BASE}/timeline`)
    if (!res.ok) throw new Error('数据获取失败')
    items.value = await res.json()
  } catch (e) {
    console.error('加载时间轴失败:', e)
    error.value = '无法加载时间轴数据，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

onMounted(fetchTimeline)
</script>

<template>
  <div class="timeline-page">
    <header class="page-header">
      <h1 class="page-title">时间轴</h1>
      <p class="page-subtitle">记录每一次创作，见证每一步成长</p>
    </header>

    <div v-if="loading" class="loading-area">
      <div class="spinner"></div>
      <p>正在加载时间轴...</p>
    </div>

    <div v-else-if="error" class="error-area">
      <p class="error-msg">{{ error }}</p>
      <button class="retry-btn" @click="fetchTimeline">重新加载</button>
    </div>

    <template v-else>
      <div class="filter-bar">
        <button
          v-for="t in typeList"
          :key="t.value"
          :class="{ active: typeFilter === t.value }"
          @click="typeFilter = t.value"
        >
          <span v-if="t.value !== 'ALL'" class="filter-dot" :style="{ background: typeConfig[t.value]?.color }"></span>
          {{ t.label }}
          <span class="filter-count" v-if="typeCounts[t.value]">{{ typeCounts[t.value] }}</span>
        </button>
      </div>

      <div v-if="!timelineItems.length" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无记录</p>
        <span>开始创作你的第一篇内容吧</span>
      </div>

      <div v-else class="timeline">
        <div class="timeline-line"></div>

        <div v-for="group in groupedByYear" :key="group.year" class="year-group">
          <div class="year-marker">
            <span class="year-text">{{ group.year }}</span>
          </div>

          <div
            v-for="item in group.items"
            :key="`${item.type}-${item.id}`"
            class="timeline-item"
            :style="{ '--dot-color': typeConfig[item.type]?.color, '--card-bg': typeConfig[item.type]?.bg }"
            @click="goTo(item.link)"
          >
            <div class="timeline-dot">
              <span class="dot-icon">{{ typeConfig[item.type]?.icon }}</span>
            </div>

            <div class="timeline-card">
              <div class="card-header">
                <span class="card-date">{{ formatDate(item.date) }}</span>
                <span class="card-type" :style="{ color: typeConfig[item.type]?.color }">
                  {{ typeConfig[item.type]?.label }}
                </span>
              </div>
              <h3 class="card-title">{{ item.title }}</h3>
              <p v-if="item.summary" class="card-summary">{{ item.summary }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.timeline-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 100px 24px 60px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4ecdc4, #45b7d1, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 10px;
}

.page-subtitle {
  color: rgba(255, 255, 255, 0.45);
  font-size: 14px;
  margin: 0;
}

.loading-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120px 0;
  color: rgba(255, 255, 255, 0.5);
}

.spinner {
  width: 36px;
  height: 36px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: #4ecdc4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-area {
  text-align: center;
  padding: 120px 0;
}

.error-msg {
  color: #ef5350;
  font-size: 14px;
  margin: 0 0 16px;
}

.retry-btn {
  background: rgba(78, 205, 196, 0.1);
  border: 1px solid rgba(78, 205, 196, 0.3);
  color: #4ecdc4;
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.retry-btn:hover {
  background: rgba(78, 205, 196, 0.15);
}

.filter-bar {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.filter-bar button {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.filter-bar button:hover {
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.8);
}

.filter-bar button.active {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.filter-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.filter-count {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  margin-left: 2px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: rgba(255, 255, 255, 0.35);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 16px;
  margin: 0 0 8px;
}

.empty-state span {
  font-size: 13px;
}

.timeline {
  position: relative;
  padding-left: 40px;
}

.timeline-line {
  position: absolute;
  left: 19px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg,
    rgba(78, 205, 196, 0.3),
    rgba(69, 183, 209, 0.3) 35%,
    rgba(167, 139, 250, 0.3) 70%,
    rgba(255, 255, 255, 0.05)
  );
  border-radius: 1px;
}

.year-group {
  position: relative;
  margin-bottom: 32px;
}

.year-marker {
  position: relative;
  left: -52px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.year-marker::after {
  content: '';
  width: 26px;
  height: 2px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 1px;
}

.year-text {
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 2px;
  background: linear-gradient(135deg, rgba(255,255,255,0.4), rgba(255,255,255,0.15));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.timeline-item {
  position: relative;
  margin-bottom: 20px;
  cursor: pointer;
  animation: fadeSlideIn 0.5s ease backwards;
}

.timeline-item:nth-child(1) { animation-delay: 0.05s; }
.timeline-item:nth-child(2) { animation-delay: 0.1s; }
.timeline-item:nth-child(3) { animation-delay: 0.15s; }
.timeline-item:nth-child(4) { animation-delay: 0.2s; }
.timeline-item:nth-child(5) { animation-delay: 0.25s; }
.timeline-item:nth-child(6) { animation-delay: 0.3s; }
.timeline-item:nth-child(7) { animation-delay: 0.35s; }
.timeline-item:nth-child(8) { animation-delay: 0.4s; }

@keyframes fadeSlideIn {
  from {
    opacity: 0;
    transform: translateX(-16px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.timeline-dot {
  position: absolute;
  left: -40px;
  top: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--card-bg);
  border: 2px solid var(--dot-color);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.timeline-item:hover .timeline-dot {
  transform: scale(1.15);
  box-shadow: 0 0 16px var(--dot-color);
}

.dot-icon {
  font-size: 16px;
  line-height: 1;
}

.timeline-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 16px 20px;
  transition: all 0.25s ease;
}

.timeline-item:hover .timeline-card {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
  transform: translateX(6px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.card-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  font-variant-numeric: tabular-nums;
}

.card-type {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  background: var(--card-bg);
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
  line-height: 1.4;
}

.card-summary {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 768px) {
  .timeline-page {
    padding: 80px 16px 40px;
  }

  .page-title {
    font-size: 26px;
  }

  .timeline {
    padding-left: 32px;
  }

  .timeline-line {
    left: 15px;
  }

  .timeline-dot {
    left: -32px;
    width: 34px;
    height: 34px;
  }

  .dot-icon {
    font-size: 14px;
  }

  .timeline-card {
    padding: 14px 16px;
  }

  .card-title {
    font-size: 14px;
  }

  .year-marker {
    left: -44px;
  }

  .year-text {
    font-size: 17px;
  }
}
</style>