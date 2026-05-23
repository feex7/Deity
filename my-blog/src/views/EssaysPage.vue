<script setup>
import { ref, onMounted, reactive } from 'vue'
import { isAdmin } from '../utils/isAdmin.js'
import TagInput from '../components/TagInput.vue'

const API = '/api/essays'

const items = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch(API)
    items.value = await res.json()
  } catch (e) {
    console.error('加载随笔失败:', e)
    error.value = '无法连接到服务器，请确认后端已启动'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', content: '', tags: '' })

const openAdd = () => {
  editingId.value = null
  Object.assign(form, { title: '', content: '', tags: '' })
  showForm.value = true
}

const openEdit = (item) => {
  editingId.value = item.id
  Object.assign(form, {
    title: item.title || '',
    content: item.content || '',
    tags: item.tags || ''
  })
  showForm.value = true
}

const save = async () => {
  saving.value = true
  const body = { ...form }
  try {
    if (editingId.value) {
      const res = await fetch(`${API}/${editingId.value}`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
      })
      const updated = await res.json()
      const idx = items.value.findIndex(i => i.id === editingId.value)
      if (idx !== -1) items.value[idx] = updated
    } else {
      const res = await fetch(API, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
      })
      items.value.push(await res.json())
    }
    showForm.value = false
  } catch (e) {
    console.error('保存随笔失败:', e)
    alert('保存失败，请确认后端服务已启动')
  } finally {
    saving.value = false
  }
}

const deleteConfirmId = ref(null)
const doDelete = async () => {
  if (!deleteConfirmId.value) return
  await fetch(`${API}/${deleteConfirmId.value}`, { method: 'DELETE' })
  items.value = items.value.filter(i => i.id !== deleteConfirmId.value)
  deleteConfirmId.value = null
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1>随笔</h1>
        <p class="subtitle">日常思考与技术感悟</p>
      </div>
      <button v-if="isAdmin()" class="btn-add" @click="openAdd">+ 写随笔</button>
    </header>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="error" class="error-state">
      <p class="error-msg">{{ error }}</p>
      <button class="retry-btn" @click="load">重新加载</button>
    </div>

    <div v-else-if="items.length === 0" class="empty">暂无随笔</div>

    <div v-else class="list">
      <article v-for="item in items" :key="item.id" class="essay-card">
        <div class="essay-left">
          <time class="essay-date">{{ item.date || '——' }}</time>
        </div>
        <div class="essay-body">
          <h3 class="essay-title">{{ item.title }}</h3>
          <p class="essay-content">{{ item.content }}</p>
          <div v-if="item.tags" class="tags-row">
            <span v-for="t in item.tags.split(',')" :key="t" class="tag-chip">{{ t.trim() }}</span>
          </div>
        </div>
        <div v-if="isAdmin()" class="essay-actions">
          <button class="action edit" @click="openEdit(item)">编辑</button>
          <button class="action del" @click="deleteConfirmId = item.id">删除</button>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="overlay" @click.self="showForm = false">
        <div class="modal">
          <h3 class="modal-title">{{ editingId ? '编辑随笔' : '写随笔' }}</h3>
          <input v-model="form.title" placeholder="标题" class="input" />
          <textarea v-model="form.content" placeholder="内容..." class="input textarea" rows="6"></textarea>
          <TagInput v-model="form.tags" />
          <div class="modal-actions">
            <button class="btn-cancel" @click="showForm = false">取消</button>
            <button class="btn-save" @click="save" :disabled="!form.title || !form.content || saving">
              {{ saving ? '保存中...' : (editingId ? '保存' : '发布') }}
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
.page { max-width: 800px; margin: 0 auto; padding: 100px 24px 80px; min-height: 100vh; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 36px; }
h1 { font-size: 26px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.subtitle { font-size: 13px; color: rgba(255,255,255,0.35); }
.btn-add { padding: 8px 20px; background: rgba(0,255,128,0.1); border: 1px solid rgba(0,255,128,0.25); color: #00ff80; border-radius: 8px; font-size: 13px; cursor: pointer; transition: all .2s; }
.btn-add:hover { background: rgba(0,255,128,0.18); }
.loading { text-align: center; color: rgba(255,255,255,0.3); padding: 80px 0; font-size: 14px; }
.empty { text-align: center; color: rgba(255,255,255,0.3); padding: 80px 0; font-size: 14px; }
.error-state { text-align: center; padding: 80px 0; }
.error-msg { color: rgba(255,255,255,0.4); font-size: 14px; margin-bottom: 16px; }
.retry-btn { padding: 8px 20px; border: 1px solid rgba(0,255,128,0.3); border-radius: 8px; background: transparent; color: #00ff80; font-size: 13px; cursor: pointer; transition: all .2s; }
.retry-btn:hover { background: rgba(0,255,128,0.1); }
.list { display: flex; flex-direction: column; gap: 24px; }
.essay-card { display: flex; gap: 20px; padding: 24px 0; border-bottom: 1px solid rgba(255,255,255,0.05); transition: border-color .3s; }
.essay-card:hover { border-bottom-color: rgba(0,255,128,0.12); }
.essay-left { width: 90px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; }
.essay-date { font-size: 12px; color: #00ff80; letter-spacing: 1px; }
.essay-body { flex: 1; min-width: 0; }
.essay-title { font-size: 17px; font-weight: 600; color: #fff; margin-bottom: 8px; }
.essay-content { font-size: 13px; color: rgba(255,255,255,0.45); line-height: 1.8; }
.tags-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.tag-chip { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: rgba(0,255,128,0.06); color: rgba(0,255,128,0.6); }
.essay-actions { display: flex; flex-direction: column; gap: 4px; flex-shrink: 0; }
.action { padding: 6px 14px; border: 1px solid rgba(255,255,255,0.06); border-radius: 6px; background: none; font-size: 12px; cursor: pointer; transition: all .2s; }
.edit { color: rgba(255,255,255,0.35); }
.edit:hover { border-color: rgba(0,255,128,0.25); color: #00ff80; }
.del { color: rgba(255,255,255,0.25); }
.del:hover { border-color: rgba(255,68,68,0.25); color: #ff4444; }

.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(4px); }
.modal { background: #1a1a1a; border: 1px solid rgba(255,255,255,0.1); border-radius: 14px; padding: 28px; width: 90%; max-width: 500px; }
.modal-title { font-size: 17px; font-weight: 600; color: #fff; margin-bottom: 20px; }
.input { width: 100%; padding: 10px 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; color: #fff; font-size: 13px; margin-bottom: 12px; outline: none; box-sizing: border-box; }
.input:focus { border-color: rgba(0,255,128,0.3); }
.textarea { resize: vertical; font-family: inherit; }
select.input { appearance: none; }
.modal-actions, .confirm-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
.btn-save { padding: 8px 24px; background: rgba(0,255,128,0.15); border: 1px solid rgba(0,255,128,0.25); color: #00ff80; border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-save:disabled { opacity: 0.3; cursor: not-allowed; }
.btn-cancel { padding: 8px 24px; background: none; border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.5); border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-cancel:hover { border-color: rgba(255,255,255,0.2); color: #fff; }
.btn-danger { padding: 8px 24px; background: rgba(255,68,68,0.15); border: 1px solid rgba(255,68,68,0.3); color: #ff4444; border-radius: 8px; font-size: 13px; cursor: pointer; }
.confirm-dialog { background: #1a1a1a; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 28px; max-width: 380px; width: 90%; text-align: center; }
.confirm-title { color: #fff; font-size: 17px; font-weight: 600; margin-bottom: 8px; }
.confirm-desc { color: rgba(255,255,255,0.45); font-size: 13px; margin-bottom: 24px; }

@media (max-width: 768px) { .essay-card { flex-direction: column; gap: 12px; } .essay-left { width: auto; flex-direction: row; gap: 12px; align-items: center; } }
</style>