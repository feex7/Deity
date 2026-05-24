<script setup>
import { ref, computed, onMounted } from 'vue'
import { isAdmin } from '../utils/isAdmin.js'
import { API_BASE } from '../api/config.js'

const API = `${API_BASE}/favorites`

const favorites = ref([])
const loading = ref(true)
const error = ref('')
const favCategory = ref('')
const favKeyword = ref('')

const CATEGORIES = [
  { value: 'GITHUB', label: 'GitHub', icon: '🐙' },
  { value: 'WEBSITE', label: '网站', icon: '🌐' },
  { value: 'ARTICLE', label: '文章', icon: '📄' }
]

const fetchFavorites = async () => {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (favCategory.value) params.set('category', favCategory.value)
    if (favKeyword.value) params.set('keyword', favKeyword.value)
    const qs = params.toString()
    const res = await fetch(API + (qs ? '?' + qs : ''))
    if (!res.ok) throw new Error('加载失败')
    favorites.value = await res.json()
  } catch (e) {
    error.value = '无法连接到服务器，请确认后端已启动'
  } finally {
    loading.value = false
  }
}

onMounted(fetchFavorites)

const filteredFavorites = computed(() => {
  return favorites.value
})

const catName = (cat) => {
  const c = CATEGORIES.find(x => x.value === cat)
  return c ? c.label : cat
}

const catIcon = (cat) => {
  const c = CATEGORIES.find(x => x.value === cat)
  return c ? c.icon : '🔗'
}

const getHost = (url) => {
  try { return new URL(url).hostname.replace('www.', '') }
  catch { return url }
}

const showForm = ref(false)
const editingId = ref(null)
const form = ref({ title: '', url: '', description: '', category: 'WEBSITE' })
const formError = ref('')
const saving = ref(false)

const openAdd = () => {
  editingId.value = null
  form.value = { title: '', url: '', description: '', category: 'WEBSITE' }
  formError.value = ''
  showForm.value = true
}

const openEdit = (fav) => {
  editingId.value = fav.id
  form.value = {
    title: fav.title,
    url: fav.url,
    description: fav.description || '',
    category: fav.category
  }
  formError.value = ''
  showForm.value = true
}

const closeForm = () => {
  showForm.value = false
  editingId.value = null
}

const save = async () => {
  formError.value = ''
  if (!form.value.title.trim()) { formError.value = '标题不能为空'; return }
  if (!form.value.url.trim()) { formError.value = '链接不能为空'; return }
  saving.value = true
  try {
    const method = editingId.value ? 'PUT' : 'POST'
    const url = editingId.value ? `${API}/${editingId.value}` : API
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    if (!res.ok) throw new Error('保存失败')
    closeForm()
    await fetchFavorites()
  } catch (e) {
    formError.value = '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

const deleteConfirmId = ref(null)

const doDelete = async () => {
  if (!deleteConfirmId.value) return
  try {
    await fetch(`${API}/${deleteConfirmId.value}`, { method: 'DELETE' })
    favorites.value = favorites.value.filter(f => f.id !== deleteConfirmId.value)
  } catch (e) {
    error.value = '删除失败'
  }
  deleteConfirmId.value = null
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1>收藏夹</h1>
        <p class="subtitle">收藏的链接、项目与文章</p>
      </div>
      <button v-if="isAdmin()" class="btn-add" @click="openAdd">+ 添加收藏</button>
    </header>

    <div class="controls">
      <div class="categories">
        <button
          :class="{ active: !favCategory }"
          @click="favCategory = ''; fetchFavorites()"
        >全部</button>
        <button
          v-for="cat in CATEGORIES"
          :key="cat.value"
          :class="{ active: favCategory === cat.value }"
          @click="favCategory = cat.value; fetchFavorites()"
        >{{ cat.icon }} {{ cat.label }}</button>
      </div>
      <div class="search-box">
        <input
          v-model="favKeyword"
          type="text"
          placeholder="搜索收藏..."
          @input="fetchFavorites"
        />
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="error" class="error-state">
      <p class="error-msg">{{ error }}</p>
      <button class="retry-btn" @click="fetchFavorites">重新加载</button>
    </div>

    <div v-else-if="!filteredFavorites.length" class="empty">
      <span class="empty-icon">{{ favCategory ? '🔍' : '⭐' }}</span>
      <p>{{ favCategory ? '该分类暂无收藏' : '收藏夹为空' }}</p>
      <span v-if="isAdmin() && !favCategory" class="empty-hint">点击上方"+ 添加收藏"开始收藏</span>
    </div>

    <div v-else class="grid">
      <article v-for="fav in filteredFavorites" :key="fav.id" class="card">
        <a :href="fav.url" target="_blank" rel="noopener" class="card-link">
          <div class="card-top">
            <span class="cat-badge">{{ catIcon(fav.category) }} {{ catName(fav.category) }}</span>
            <div v-if="isAdmin()" class="card-actions" @click.prevent.stop>
              <button class="action edit" @click="openEdit(fav)" title="编辑">✎</button>
              <button class="action del" @click="deleteConfirmId = fav.id" title="删除">✕</button>
            </div>
          </div>
          <h3 class="card-title">{{ fav.title }}</h3>
          <p v-if="fav.description" class="card-desc">{{ fav.description }}</p>
          <span class="card-url">{{ getHost(fav.url) }}</span>
        </a>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="overlay" @click.self="closeForm">
        <div class="modal">
          <h3 class="modal-title">{{ editingId ? '编辑收藏' : '添加收藏' }}</h3>

          <label class="input-label">类型</label>
          <div class="type-select">
            <button
              v-for="cat in CATEGORIES"
              :key="cat.value"
              :class="{ active: form.category === cat.value }"
              @click="form.category = cat.value"
            >{{ cat.icon }} {{ cat.label }}</button>
          </div>

          <label class="input-label">标题</label>
          <input v-model="form.title" type="text" placeholder="收藏标题" class="input" />

          <label class="input-label">链接</label>
          <input v-model="form.url" type="url" placeholder="https://..." class="input" />

          <label class="input-label">描述（可选）</label>
          <textarea v-model="form.description" placeholder="简短描述..." rows="3" class="input textarea"></textarea>

          <p v-if="formError" class="form-error">{{ formError }}</p>

          <div class="modal-actions">
            <button class="btn-cancel" @click="closeForm">取消</button>
            <button class="btn-save" @click="save" :disabled="saving">
              {{ saving ? '保存中...' : (editingId ? '保存' : '添加') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="deleteConfirmId" class="overlay" @click.self="deleteConfirmId = null">
        <div class="confirm-dialog">
          <p class="confirm-title">确认删除</p>
          <p class="confirm-desc">删除后无法恢复</p>
          <div class="confirm-actions">
            <button class="btn-cancel" @click="deleteConfirmId = null">取消</button>
            <button class="btn-danger" @click="doDelete">确认删除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; padding: 100px 24px 80px; min-height: 100vh; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 28px; }
h1 { font-size: 26px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.subtitle { font-size: 13px; color: rgba(255,255,255,0.35); }
.btn-add { padding: 8px 20px; background: rgba(0,255,128,0.1); border: 1px solid rgba(0,255,128,0.25); color: #00ff80; border-radius: 8px; font-size: 13px; cursor: pointer; transition: all .2s; }
.btn-add:hover { background: rgba(0,255,128,0.18); }

.controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.categories {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.categories button {
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.45);
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s;
}

.categories button:hover {
  border-color: rgba(255,255,255,0.15);
  color: rgba(255,255,255,0.7);
}

.categories button.active {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.2);
  color: #fff;
}

.search-box input {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  padding: 8px 14px;
  color: #fff;
  font-size: 13px;
  width: 200px;
  outline: none;
  transition: border-color .2s;
}

.search-box input:focus {
  border-color: rgba(0,255,128,0.4);
}

.search-box input::placeholder {
  color: rgba(255,255,255,0.25);
}

.loading { text-align: center; color: rgba(255,255,255,0.3); padding: 80px 0; font-size: 14px; }
.empty { text-align: center; color: rgba(255,255,255,0.3); padding: 80px 0; font-size: 14px; }
.empty-icon { font-size: 36px; display: block; margin-bottom: 12px; }
.empty-hint { font-size: 12px; color: rgba(255,255,255,0.2); display: block; margin-top: 8px; }
.error-state { text-align: center; padding: 80px 0; }
.error-msg { color: rgba(255,255,255,0.4); font-size: 14px; margin-bottom: 16px; }
.retry-btn { padding: 8px 20px; border: 1px solid rgba(0,255,128,0.3); border-radius: 8px; background: transparent; color: #00ff80; font-size: 13px; cursor: pointer; transition: all .2s; }
.retry-btn:hover { background: rgba(0,255,128,0.1); }

.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }

.card {
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  transition: all .25s;
}

.card:hover {
  background: rgba(255,255,255,0.04);
  border-color: rgba(255,255,255,0.14);
  transform: translateY(-1px);
}

.card-link {
  display: block;
  padding: 18px 20px;
  text-decoration: none;
  color: inherit;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.cat-badge {
  font-size: 11px;
  color: rgba(255,255,255,0.4);
}

.card-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity .2s;
}

.card:hover .card-actions {
  opacity: 1;
}

.action {
  background: none;
  border: none;
  color: rgba(255,255,255,0.25);
  font-size: 14px;
  cursor: pointer;
  padding: 2px 6px;
  transition: color .2s;
}

.action:hover { color: rgba(255,255,255,0.6); }
.action.del:hover { color: #ef5350; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
  word-break: break-word;
}

.card-desc {
  font-size: 12px;
  color: rgba(255,255,255,0.35);
  margin: 0 0 10px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-url {
  font-size: 11px;
  color: rgba(0,255,128,0.4);
}

.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(4px); }
.modal { background: #1a1a1a; border: 1px solid rgba(255,255,255,0.1); border-radius: 14px; padding: 28px; width: 90%; max-width: 460px; }
.modal-title { font-size: 17px; font-weight: 600; color: #fff; margin-bottom: 20px; }

.input-label {
  display: block;
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  margin-bottom: 6px;
}

.input { width: 100%; padding: 10px 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; color: #fff; font-size: 13px; margin-bottom: 14px; outline: none; box-sizing: border-box; }
.input:focus { border-color: rgba(0,255,128,0.3); }
.textarea { resize: vertical; font-family: inherit; }

.type-select {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.type-select button {
  flex: 1;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.45);
  padding: 8px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s;
}

.type-select button.active {
  background: rgba(0,255,128,0.08);
  border-color: rgba(0,255,128,0.3);
  color: #00ff80;
}

.form-error { color: #ef5350; font-size: 12px; margin: 0 0 8px; }

.modal-actions, .confirm-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
.btn-save { padding: 8px 24px; background: rgba(0,255,128,0.15); border: 1px solid rgba(0,255,128,0.25); color: #00ff80; border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-save:disabled { opacity: 0.3; cursor: not-allowed; }
.btn-cancel { padding: 8px 24px; background: none; border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.5); border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-cancel:hover { border-color: rgba(255,255,255,0.2); color: #fff; }
.btn-danger { padding: 8px 24px; background: rgba(255,68,68,0.15); border: 1px solid rgba(255,68,68,0.3); color: #ff4444; border-radius: 8px; font-size: 13px; cursor: pointer; }
.confirm-dialog { background: #1a1a1a; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 28px; max-width: 380px; width: 90%; text-align: center; }
.confirm-title { color: #fff; font-size: 17px; font-weight: 600; margin-bottom: 8px; }
.confirm-desc { color: rgba(255,255,255,0.45); font-size: 13px; margin-bottom: 24px; }

@media (max-width: 768px) {
  .grid { grid-template-columns: 1fr; }
  .controls { flex-direction: column; align-items: stretch; }
  .search-box input { width: 100%; }
}
</style>