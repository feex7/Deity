const STORAGE_KEY = 'blog_articles'

export function loadArticles() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function saveArticles(articles) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(articles))
  } catch (e) {
    console.error('保存文章失败，localStorage 可能已满:', e)
  }
}

export function clearArticles() {
  localStorage.removeItem(STORAGE_KEY)
}