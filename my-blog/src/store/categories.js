import { reactive } from 'vue'

const STORAGE_KEY = 'blog_custom_categories'

function loadCustomCategories() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveCustomCategories(cats) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cats))
  } catch { }
}

export const categoryStore = reactive({
  defaultCategories: [],
  customCategories: loadCustomCategories(),

  get all() {
    return [...this.defaultCategories, ...this.customCategories]
  },

  setDefaults(cats) {
    this.defaultCategories = [...cats]
  },

  addCustom(name) {
    if (!this.all.includes(name)) {
      this.customCategories.push(name)
      saveCustomCategories(this.customCategories)
    }
  },

  removeCustom(name) {
    this.customCategories = this.customCategories.filter(c => c !== name)
    saveCustomCategories(this.customCategories)
  }
})