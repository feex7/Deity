import { API_BASE } from './config.js'

export const articlesAPI = {
  async list() {
    const res = await fetch(`${API_BASE}/articles`)
    const data = await res.json()
    return data.map(formatArticle)
  },

  async get(id) {
    const res = await fetch(`${API_BASE}/articles/${id}`)
    return formatArticle(await res.json())
  },

  async create(article) {
    const res = await fetch(`${API_BASE}/articles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: article.title,
        summary: article.summary || article.title,
        category: article.category,
        content: article.content,
        tags: JSON.stringify(article.tags || []),
        date: article.date || new Date().toISOString().slice(0, 10)
      })
    })
    return formatArticle(await res.json())
  },

  async update(id, data) {
    const body = { ...data }
    if (body.tags && Array.isArray(body.tags)) {
      body.tags = JSON.stringify(body.tags)
    }
    const res = await fetch(`${API_BASE}/articles/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return formatArticle(await res.json())
  },

  async remove(id) {
    await fetch(`${API_BASE}/articles/${id}`, { method: 'DELETE' })
  }
}

function formatArticle(data) {
  let tags = []
  if (data.tags) {
    try {
      tags = JSON.parse(data.tags)
    } catch {
      tags = data.tags.split(',').filter(Boolean)
    }
  }
  return {
    id: data.id,
    title: data.title,
    summary: data.summary || '',
    category: data.category || '',
    date: data.date,
    author: data.author || 'Admin',
    content: data.content || '',
    tags
  }
}