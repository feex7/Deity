<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { API_BASE } from '../api/config.js'

const STOCKS = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'NVDA']
const STOCK_NAMES = {
  AAPL: '苹果', GOOGL: '谷歌', MSFT: '微软', AMZN: '亚马逊', NVDA: '英伟达'
}
const STOCK_COLORS = {
  AAPL: '#ff6b6b', GOOGL: '#4ecdc4', MSFT: '#45b7d1',
  AMZN: '#f9ca24', NVDA: '#76b041'
}

const loading = ref(true)
const refreshing = ref(false)
const error = ref('')
const stocksData = ref({})
const goldData = ref([])
const activeStock = ref('AAPL')
const klineMode = ref('daily')

let stockChart = null
let goldChart = null
let resizeHandler = null

const goldStats = computed(() => {
  if (!goldData.value.length) return null
  const prices = goldData.value.map(d => d.close)
  const first = prices[0]
  const last = prices[prices.length - 1]
  const max = Math.max(...prices)
  const min = Math.min(...prices)
  const changePct = ((last - first) / first * 100).toFixed(2)
  return {
    current: last.toFixed(2),
    change: (last - first).toFixed(2),
    changePct: changePct,
    high: max.toFixed(2),
    low: min.toFixed(2),
    isUp: last >= first
  }
})

const currentStockStats = computed(() => {
  const list = stocksData.value[activeStock.value]
  if (!list || !list.length) return null
  const first = list[0]
  const last = list[list.length - 1]
  const changePct = ((last.close - first.close) / first.close * 100).toFixed(2)
  return {
    current: last.close.toFixed(2),
    change: (last.close - first.close).toFixed(2),
    changePct: changePct,
    high: Math.max(...list.map(d => d.high)).toFixed(2),
    low: Math.min(...list.map(d => d.low)).toFixed(2),
    isUp: last.close >= first.close
  }
})

const fetchAll = async () => {
  loading.value = true
  error.value = ''
  try {
    const [stockRes, goldRes] = await Promise.all([
      fetch(`${API_BASE}/market/stocks?symbols=${STOCKS.join(',')}&days=30&t=${Date.now()}`),
      fetch(`${API_BASE}/market/gold?days=90&t=${Date.now()}`)
    ])
    if (!stockRes.ok || !goldRes.ok) throw new Error('数据获取失败')
    stocksData.value = await stockRes.json()
    goldData.value = await goldRes.json()
  } catch (e) {
    console.error('加载市场数据失败:', e)
    error.value = '无法获取市场数据，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  refreshing.value = true
  try {
    await fetch(`${API_BASE}/market/refresh`, { method: 'POST' })
    await new Promise(r => setTimeout(r, 1500))
    await fetchAll()
  } catch (e) {
    console.error('刷新数据失败:', e)
  } finally {
    refreshing.value = false
  }
}

const buildCandlestickData = (rawList, mode) => {
  if (!rawList || !rawList.length) return { dates: [], ohlc: [], volumes: [] }
  const daily = rawList.filter(d => d.open != null && d.close != null)
  if (mode === 'daily') {
    return {
      dates: daily.map(d => d.date),
      ohlc: daily.map(d => [d.open, d.close, d.low, d.high]),
      volumes: daily.map(d => [d.date, d.volume || 0, d.close >= d.open ? 1 : -1])
    }
  }
  const weeks = []
  let week = []
  daily.forEach((d, i) => {
    const date = new Date(d.date)
    const dayOfWeek = date.getDay()
    if ((dayOfWeek === 1 && week.length) || i === daily.length - 1) {
      if (week.length) weeks.push(week)
      week = []
    }
    week.push(d)
  })
  if (week.length) weeks.push(week)
  return {
    dates: weeks.map(w => w[w.length - 1].date),
    ohlc: weeks.map(w => {
      const opens = w.map(d => d.open)
      const closes = w.map(d => d.close)
      const highs = w.map(d => d.high)
      const lows = w.map(d => d.low)
      return [opens[0], closes[closes.length - 1], Math.min(...lows), Math.max(...highs)]
    }),
    volumes: weeks.map(w => {
      const d = w[w.length - 1]
      return [d.date, w.reduce((s, x) => s + (x.volume || 0), 0), d.close >= d.open ? 1 : -1]
    })
  }
}

const initStockChart = () => {
  const dom = document.getElementById('stock-chart')
  if (!dom) return
  if (stockChart) stockChart.dispose()
  stockChart = echarts.init(dom, 'dark')
  renderStockChart()
}

const renderStockChart = () => {
  if (!stockChart) return
  const raw = stocksData.value[activeStock.value] || []
  const { dates, ohlc, volumes } = buildCandlestickData(raw, klineMode.value)

  stockChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      borderColor: '#333',
      backgroundColor: 'rgba(20,20,20,0.95)',
      textStyle: { color: '#e0e0e0', fontSize: 12 },
      formatter(params) {
        const kItem = params.find(p => p.seriesName === 'K线')
        if (!kItem) return ''
        const data = kItem.data
        const color = data[1] >= data[0] ? '#ef5350' : '#26a69a'
        return `<div style="font-weight:600;margin-bottom:6px">${kItem.axisValue}</div>
          <div>开盘 <span style="color:${color};float:right;margin-left:20px">${data[1]}</span></div>
          <div>收盘 <span style="color:${color};float:right">${data[2]}</span></div>
          <div>最低 <span style="float:right">${data[3]}</span></div>
          <div>最高 <span style="float:right">${data[4]}</span></div>`
      }
    },
    axisPointer: {
      link: [{ xAxisIndex: 'all' }],
      label: { backgroundColor: '#333' }
    },
    grid: [
      { left: '3%', right: '3%', top: '5%', height: '55%' },
      { left: '3%', right: '3%', top: '68%', height: '18%' }
    ],
    xAxis: [
      {
        type: 'category', data: dates, gridIndex: 0,
        axisLine: { lineStyle: { color: '#444' } },
        axisLabel: { color: '#888', fontSize: 10 },
        boundaryGap: true
      },
      {
        type: 'category', data: dates, gridIndex: 1,
        axisLine: { lineStyle: { color: '#444' } },
        axisLabel: { show: false },
        boundaryGap: true
      }
    ],
    yAxis: [
      {
        scale: true, gridIndex: 0, splitNumber: 5,
        axisLine: { lineStyle: { color: '#444' } },
        axisLabel: { color: '#888', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
      },
      {
        scale: true, gridIndex: 1, splitNumber: 3,
        axisLine: { lineStyle: { color: '#444' } },
        axisLabel: { color: '#888', fontSize: 9 },
        splitLine: { show: false }
      }
    ],
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1], start: 0, end: 100 },
      { type: 'slider', xAxisIndex: [0, 1], start: 0, end: 100, height: 18, bottom: 6,
        borderColor: '#444', backgroundColor: '#1a1a1a',
        fillerColor: 'rgba(0,255,128,0.1)', handleStyle: { color: '#00ff80' },
        textStyle: { color: '#888' }
      }
    ],
    series: [
      {
        name: 'K线', type: 'candlestick', xAxisIndex: 0, yAxisIndex: 0,
        data: ohlc.map(d => [d[0], d[1], d[2], d[3]]),
        itemStyle: {
          color: '#ef5350', color0: '#26a69a',
          borderColor: '#ef5350', borderColor0: '#26a69a'
        }
      },
      {
        name: '成交量', type: 'bar', xAxisIndex: 1, yAxisIndex: 1,
        data: volumes.map(d => ({
          value: d[1],
          itemStyle: { color: d[2] > 0 ? '#ef5350' : '#26a69a' }
        }))
      }
    ]
  }, true)
}

const initGoldChart = () => {
  const dom = document.getElementById('gold-chart')
  if (!dom) return
  if (goldChart) goldChart.dispose()
  goldChart = echarts.init(dom, 'dark')
  renderGoldChart()
}

const renderGoldChart = () => {
  if (!goldChart) return
  const data = goldData.value || []
  const dates = data.map(d => d.date)
  const prices = data.map(d => d.close)
  const maxIdx = prices.indexOf(Math.max(...prices))
  const minIdx = prices.indexOf(Math.min(...prices))

  goldChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      borderColor: '#333',
      backgroundColor: 'rgba(20,20,20,0.95)',
      textStyle: { color: '#e0e0e0', fontSize: 12 },
      formatter(params) {
        const p = params[0]
        return `<div style="font-weight:600;margin-bottom:4px">${p.axisValue}</div>
          <div>黄金价格 <span style="color:#f9ca24;float:right;margin-left:16px">$${p.value}</span></div>`
      }
    },
    grid: { left: '3%', right: '8%', top: '8%', bottom: '8%' },
    xAxis: {
      type: 'category', data: dates, boundaryGap: false,
      axisLine: { lineStyle: { color: '#444' } },
      axisLabel: { color: '#888', fontSize: 10, interval: Math.floor(dates.length / 8) }
    },
    yAxis: {
      type: 'value', scale: true,
      axisLine: { lineStyle: { color: '#444' } },
      axisLabel: { color: '#888', fontSize: 10, formatter: '$ {value}' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 18, bottom: 6,
        borderColor: '#444', backgroundColor: '#1a1a1a',
        fillerColor: 'rgba(249,202,36,0.1)', handleStyle: { color: '#f9ca24' },
        textStyle: { color: '#888' }
      }
    ],
    series: [{
      type: 'line', data: prices, smooth: true, symbol: 'none',
      lineStyle: { color: '#f9ca24', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(249,202,36,0.25)' },
          { offset: 1, color: 'rgba(249,202,36,0.02)' }
        ])
      },
      markPoint: {
        data: [
          { name: '最高', coord: [maxIdx, prices[maxIdx]], value: prices[maxIdx].toFixed(2),
            symbol: 'pin', symbolSize: 40,
            itemStyle: { color: '#ef5350' },
            label: { formatter: 'H\n$ {c}', color: '#fff', fontSize: 10 } },
          { name: '最低', coord: [minIdx, prices[minIdx]], value: prices[minIdx].toFixed(2),
            symbol: 'pin', symbolSize: 40, symbolRotate: 180,
            itemStyle: { color: '#26a69a' },
            label: { formatter: 'L\n$ {c}', color: '#fff', fontSize: 10, position: 'inside' } }
        ]
      }
    }]
  }, true)
}

watch(activeStock, () => nextTick(renderStockChart))
watch(klineMode, () => nextTick(renderStockChart))
watch(goldData, () => nextTick(renderGoldChart), { deep: true })

onMounted(async () => {
  await fetchAll()
  await nextTick()
  initStockChart()
  initGoldChart()
  resizeHandler = () => {
    stockChart?.resize()
    goldChart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  stockChart?.dispose()
  goldChart?.dispose()
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<template>
  <div class="market-page">
    <header class="page-header">
      <h1 class="page-title">金融市场数据</h1>
      <p class="page-subtitle">实时追踪全球主流股票走势与国际黄金价格变动</p>
      <button class="refresh-btn" :disabled="refreshing" @click="refreshData">
        <span class="refresh-icon" :class="{ spinning: refreshing }">&#x21bb;</span>
        {{ refreshing ? '刷新中...' : '刷新数据' }}
      </button>
    </header>

    <div v-if="loading" class="loading-area">
      <div class="spinner"></div>
      <p>正在加载市场数据...</p>
    </div>

    <div v-else-if="error" class="error-area">
      <p class="error-msg">{{ error }}</p>
      <button class="retry-btn" @click="fetchAll">重新加载</button>
    </div>

    <template v-else>
      <section class="chart-section stock-section">
        <div class="section-header">
          <h2 class="section-title">股票走势</h2>
          <div class="section-controls">
            <div class="kline-toggle">
              <button
                :class="{ active: klineMode === 'daily' }"
                @click="klineMode = 'daily'"
              >日K</button>
              <button
                :class="{ active: klineMode === 'weekly' }"
                @click="klineMode = 'weekly'"
              >周K</button>
            </div>
          </div>
        </div>

        <div class="stock-tabs">
          <button
            v-for="sym in STOCKS"
            :key="sym"
            :class="{ active: activeStock === sym }"
            :style="{ '--stock-color': STOCK_COLORS[sym] }"
            @click="activeStock = sym"
          >
            <span class="stock-symbol">{{ sym }}</span>
            <span class="stock-name">{{ STOCK_NAMES[sym] }}</span>
          </button>
        </div>

        <div v-if="currentStockStats" class="stock-stats">
          <div class="stat-item">
            <span class="stat-label">当前价</span>
            <span class="stat-value" :class="currentStockStats.isUp ? 'up' : 'down'">
              ${{ currentStockStats.current }}
            </span>
          </div>
          <div class="stat-item">
            <span class="stat-label">涨跌</span>
            <span class="stat-value" :class="currentStockStats.isUp ? 'up' : 'down'">
              {{ currentStockStats.change }} ({{ currentStockStats.changePct }}%)
            </span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最高</span>
            <span class="stat-value">${{ currentStockStats.high }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最低</span>
            <span class="stat-value">${{ currentStockStats.low }}</span>
          </div>
        </div>

        <div id="stock-chart" class="chart-container"></div>
      </section>

      <section class="chart-section gold-section">
        <div class="section-header">
          <h2 class="section-title">国际黄金</h2>
          <span class="gold-badge">XAU/USD</span>
        </div>

        <div v-if="goldStats" class="gold-stats">
          <div class="stat-item">
            <span class="stat-label">当前价格</span>
            <span class="stat-value gold-price">${{ goldStats.current }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">90日涨跌</span>
            <span class="stat-value" :class="goldStats.isUp ? 'up' : 'down'">
              {{ goldStats.changePct }}%
            </span>
          </div>
          <div class="stat-item">
            <span class="stat-label">历史最高</span>
            <span class="stat-value high">${{ goldStats.high }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">历史最低</span>
            <span class="stat-value low">${{ goldStats.low }}</span>
          </div>
        </div>

        <div id="gold-chart" class="chart-container"></div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.market-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 100px 24px 60px;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #00ff80, #4ecdc4, #f9ca24);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 12px;
}

.page-subtitle {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  margin: 0 0 20px;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-icon {
  display: inline-block;
  font-size: 16px;
}

.refresh-icon.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
  color: rgba(255, 255, 255, 0.5);
}

.spinner {
  width: 36px;
  height: 36px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: #00ff80;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
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
  background: rgba(0, 255, 128, 0.1);
  border: 1px solid rgba(0, 255, 128, 0.3);
  color: #00ff80;
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.retry-btn:hover {
  background: rgba(0, 255, 128, 0.15);
}

.chart-section {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.section-controls {
  display: flex;
  align-items: center;
}

.kline-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  overflow: hidden;
}

.kline-toggle button {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.kline-toggle button.active {
  background: rgba(0, 255, 128, 0.15);
  color: #00ff80;
}

.stock-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stock-tabs button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  padding: 10px 18px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 90px;
}

.stock-tabs button:hover {
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.8);
}

.stock-tabs button.active {
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--stock-color);
  color: #fff;
}

.stock-symbol {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
}

.stock-name {
  font-size: 11px;
  opacity: 0.6;
}

.stock-tabs button.active .stock-name {
  opacity: 0.8;
}

.stock-stats,
.gold-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.stat-value.up {
  color: #ef5350;
}

.stat-value.down {
  color: #26a69a;
}

.stat-value.gold-price {
  color: #f9ca24;
}

.stat-value.high {
  color: #ef5350;
}

.stat-value.low {
  color: #26a69a;
}

.gold-badge {
  display: inline-block;
  font-size: 11px;
  color: #f9ca24;
  background: rgba(249, 202, 36, 0.1);
  border: 1px solid rgba(249, 202, 36, 0.2);
  padding: 2px 10px;
  border-radius: 10px;
}

.chart-container {
  width: 100%;
  height: 420px;
}

@media (max-width: 768px) {
  .market-page {
    padding: 80px 16px 40px;
  }

  .page-title {
    font-size: 26px;
  }

  .chart-section {
    padding: 16px;
  }

  .chart-container {
    height: 320px;
  }

  .stock-tabs button {
    min-width: 70px;
    padding: 8px 12px;
  }

  .stock-stats,
  .gold-stats {
    gap: 12px;
  }

  .stat-value {
    font-size: 13px;
  }
}
</style>