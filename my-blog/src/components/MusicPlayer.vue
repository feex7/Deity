<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { API_BASE } from '../api/config.js'

const playlist = ref([])
const currentIndex = ref(-1)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const showPanel = ref(false)
const loading = ref(false)
const audio = ref(null)
const progressRef = ref(null)
const isAdmin = ref(sessionStorage.getItem('adminVerified') === 'true')
const uploadError = ref('')
const uploading = ref(false)
const playError = ref(false)

const currentSong = computed(() => playlist.value[currentIndex.value] || null)
const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < playlist.value.length - 1)
const isEmpty = computed(() => playlist.value.length === 0)

const progressPct = computed(() => {
  if (!duration.value) return 0
  return (currentTime.value / duration.value) * 100
})

const formatTime = (sec) => {
  if (!sec || !isFinite(sec)) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

const initAudio = () => {
  if (!audio.value) {
    audio.value = new Audio()
    audio.value.preload = 'auto'
    audio.value.addEventListener('loadedmetadata', () => {
      duration.value = audio.value.duration
    })
    audio.value.addEventListener('timeupdate', () => {
      currentTime.value = audio.value.currentTime
    })
    audio.value.addEventListener('ended', () => {
      playNext()
    })
    audio.value.addEventListener('error', () => {
      isPlaying.value = false
    })
  }
}

const fetchPlaylist = async () => {
  try {
    const res = await fetch(`${API_BASE}/music`)
    if (res.ok) {
      const data = await res.json()
      playlist.value = data
      if (data.length && currentIndex.value < 0) {
        currentIndex.value = 0
      }
    }
  } catch (e) {
    console.error('获取音乐列表失败:', e)
  }
}

const play = (index) => {
  if (!playlist.value.length) return
  initAudio()
  playError.value = false
  const song = playlist.value[index || 0]
  if (index !== undefined) currentIndex.value = index
  audio.value.src = `${API_BASE}/music/${song.id}/stream`
  audio.value.play().then(() => {
    isPlaying.value = true
    playError.value = false
  }).catch(() => {
    isPlaying.value = false
    playError.value = true
  })
  loading.value = false
}

const togglePlay = () => {
  if (!currentSong.value) return
  if (isPlaying.value) {
    if (audio.value) {
      audio.value.pause()
      isPlaying.value = false
    }
  } else {
    if (!audio.value) {
      play(currentIndex.value)
      return
    }
    audio.value.play().then(() => {
      isPlaying.value = true
      playError.value = false
    }).catch(() => {
      isPlaying.value = false
      playError.value = true
    })
  }
}

const playPrev = () => {
  if (!hasPrev.value) return
  loading.value = true
  play(currentIndex.value - 1)
}

const playNext = () => {
  if (!hasNext.value) return
  loading.value = true
  play(currentIndex.value + 1)
}

const seek = (e) => {
  if (!audio.value || !duration.value) return
  const rect = progressRef.value.getBoundingClientRect()
  const pct = (e.clientX - rect.left) / rect.width
  audio.value.currentTime = Math.max(0, Math.min(pct * duration.value, duration.value))
}

let dragActive = false
const onDragStart = (e) => {
  dragActive = true
  seek(e)
  const onMove = (ev) => {
    if (dragActive) seek(ev)
  }
  const onUp = () => {
    dragActive = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

const handleUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  uploadError.value = ''
  try {
    const form = new FormData()
    form.append('title', file.name.replace(/\.[^.]+$/, ''))
    form.append('file', file)
    const res = await fetch(`${API_BASE}/music`, { method: 'POST', body: form })
    if (!res.ok) {
      const err = await res.json()
      throw new Error(err.error || '上传失败')
    }
    await fetchPlaylist()
  } catch (err) {
    uploadError.value = err.message
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

const deleteSong = async (id) => {
  if (!confirm('确定删除这首歌曲？')) return
  try {
    await fetch(`${API_BASE}/music/${id}`, { method: 'DELETE' })
    if (currentSong.value?.id === id) {
      audio.value?.pause()
      isPlaying.value = false
    }
    await fetchPlaylist()
    if (currentIndex.value >= playlist.value.length) {
      currentIndex.value = Math.max(0, playlist.value.length - 1)
    }
  } catch (e) {
    console.error('删除失败:', e)
  }
}

const togglePanel = () => {
  showPanel.value = !showPanel.value
}

const handleClickOutside = (e) => {
  const panel = document.querySelector('.music-player')
  if (panel && !panel.contains(e.target)) {
    showPanel.value = false
  }
}

let pollTimer = null
onMounted(() => {
  fetchPlaylist()
  pollTimer = setInterval(() => {
    isAdmin.value = sessionStorage.getItem('adminVerified') === 'true'
  }, 2000)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('click', handleClickOutside)
  if (audio.value) {
    audio.value.pause()
    audio.value = null
  }
})
</script>

<template>
  <div class="music-player" :class="{ expanded: showPanel, empty: isEmpty }">
    <div class="player-bar" @click="togglePanel">
      <div class="player-icon">
        <span v-if="isEmpty">🎵</span>
        <span v-else class="equalizer" :class="{ active: isPlaying }">
          <i></i><i></i><i></i>
        </span>
      </div>
      <div class="player-info">
        <span class="player-song" :title="currentSong?.title">
          {{ playError ? '⚠ 播放失败' : (currentSong?.title || '暂无音乐') }}
        </span>
        <span v-if="!isEmpty && currentSong" class="player-time">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </span>
      </div>
      <button class="player-btn" @click.stop="playPrev" :disabled="!hasPrev" title="上一曲">
        <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
          <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
        </svg>
      </button>
      <button class="player-btn play-btn" @click.stop="togglePlay" :disabled="isEmpty" title="播放/暂停">
        <svg v-if="isPlaying" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
          <rect x="6" y="4" width="4" height="16" rx="1"/>
          <rect x="14" y="4" width="4" height="16" rx="1"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
          <path d="M8 5v14l11-7z"/>
        </svg>
      </button>
      <button class="player-btn" @click.stop="playNext" :disabled="!hasNext" title="下一曲">
        <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
          <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
        </svg>
      </button>
    </div>

    <div
      v-if="!isEmpty"
      class="progress-bar"
      ref="progressRef"
      @mousedown="onDragStart"
    >
      <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
      <div class="progress-thumb" :style="{ left: progressPct + '%' }"></div>
    </div>

    <div v-if="showPanel" class="player-panel">
      <div class="panel-title">播放列表</div>
      <div class="playlist">
        <div
          v-for="(song, i) in playlist"
          :key="song.id"
          class="playlist-item"
          :class="{ current: i === currentIndex }"
          @click="play(i)"
        >
          <span class="playlist-index">{{ i + 1 }}</span>
          <span class="playlist-song">{{ song.title }}</span>
          <button
            v-if="isAdmin"
            class="delete-song-btn"
            @click.stop="deleteSong(song.id)"
            title="删除"
          >&times;</button>
        </div>
      </div>

      <div v-if="isAdmin" class="admin-section">
        <label class="upload-label" :class="{ disabled: uploading }">
          {{ uploading ? '上传中...' : '+ 添加歌曲' }}
          <input
            type="file"
            accept="audio/*"
            @change="handleUpload"
            :disabled="uploading"
            hidden
          />
        </label>
        <p v-if="uploadError" class="upload-error">{{ uploadError }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.music-player {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.player-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(20, 20, 20, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 8px 14px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.player-bar:hover {
  border-color: rgba(255, 255, 255, 0.18);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.5);
}

.player-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.equalizer {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 16px;
}

.equalizer i {
  display: block;
  width: 3px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  transition: height 0.2s ease, background 0.2s ease;
}

.equalizer i:nth-child(1) { height: 6px; }
.equalizer i:nth-child(2) { height: 12px; }
.equalizer i:nth-child(3) { height: 8px; }

.equalizer.active i {
  background: #00ff80;
}

.equalizer.active i:nth-child(1) { animation: eq1 0.6s ease infinite alternate; }
.equalizer.active i:nth-child(2) { animation: eq2 0.5s ease infinite alternate; }
.equalizer.active i:nth-child(3) { animation: eq3 0.7s ease infinite alternate; }

@keyframes eq1 { to { height: 12px; } }
@keyframes eq2 { to { height: 5px; } }
@keyframes eq3 { to { height: 14px; } }

.player-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  max-width: 150px;
}

.player-song {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.player-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.35);
  font-variant-numeric: tabular-nums;
}

.music-player.empty .player-song {
  color: rgba(255, 255, 255, 0.35);
}

.player-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
  border-radius: 4px;
}

.player-btn:hover:not(:disabled) {
  color: #fff;
}

.player-btn:disabled {
  color: rgba(255, 255, 255, 0.15);
  cursor: default;
}

.play-btn {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  width: 30px;
  height: 30px;
}

.play-btn:hover:not(:disabled) {
  color: #00ff80;
  background: rgba(0, 255, 128, 0.1);
}

.progress-bar {
  width: 100%;
  height: 3px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  margin-top: -1px;
  cursor: pointer;
  position: relative;
  transition: height 0.15s ease;
}

.progress-bar:hover {
  height: 5px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00ff80, #4ecdc4);
  border-radius: 2px;
  transition: width 0.15s linear;
}

.progress-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
  height: 10px;
  background: #00ff80;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.progress-bar:hover .progress-thumb {
  opacity: 1;
}

.player-panel {
  position: absolute;
  bottom: 100%;
  right: 0;
  margin-bottom: 8px;
  width: 260px;
  max-height: 300px;
  background: rgba(20, 20, 20, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.2s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.panel-title {
  padding: 14px 16px 10px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.playlist {
  overflow-y: auto;
  flex: 1;
}

.playlist-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.playlist-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.playlist-item.current {
  background: rgba(0, 255, 128, 0.08);
}

.playlist-index {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  width: 20px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.playlist-item.current .playlist-index {
  color: #00ff80;
}

.playlist-song {
  flex: 1;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.playlist-item.current .playlist-song {
  color: #00ff80;
}

.delete-song-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.25);
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  padding: 0 4px;
  transition: color 0.2s;
}

.delete-song-btn:hover {
  color: #ef5350;
}

.admin-section {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.upload-label {
  display: block;
  text-align: center;
  padding: 8px;
  background: rgba(0, 255, 128, 0.08);
  border: 1px dashed rgba(0, 255, 128, 0.25);
  border-radius: 8px;
  color: #00ff80;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-label:hover {
  background: rgba(0, 255, 128, 0.12);
  border-color: rgba(0, 255, 128, 0.4);
}

.upload-label.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.upload-error {
  color: #ef5350;
  font-size: 12px;
  margin: 8px 0 0;
  text-align: center;
}

@media (max-width: 768px) {
  .music-player {
    bottom: 16px;
    right: 16px;
  }

  .player-bar {
    padding: 6px 12px;
    gap: 6px;
  }

  .player-info {
    max-width: 100px;
  }

  .player-song {
    font-size: 11px;
  }

  .player-panel {
    width: 240px;
    right: 0;
  }
}
</style>