<!--
  ============================================================
  组件名称: WritePage（写文章页面）
  功能描述: 集成式 Markdown 编辑器，用于撰写和发布博客文章
  
  页面布局（桌面端）:
    ┌─────────────────────────────────────────────────┐
    │  ← 返回                      写文章             │
    │─────────────────────────────────────────────────│
    │  文章标题: [________________________]           │
    │  文章分类: [下拉选择 ▼]  [+ 新建分类]          │
    │─────────────────────────────────────────────────│
    │  工具栏: H1 H2 B I ` </> [link] ![img] " L     │
    │─────────────────────────────────────────────────│
    │  ┌──────────────┐ ┌───────────────────────────┐ │
    │  │  预览         │ │  编辑                     │ │
    │  │  (实时渲染)   │ │  (Markdown 输入)         │ │
    │  │              │ │                           │ │
    │  └──────────────┘ └───────────────────────────┘ │
    │─────────────────────────────────────────────────│
    │  文章标签: [输入标签...]  [标签1 ×] [标签2 ×]  │
    │─────────────────────────────────────────────────│
    │              字数: N     [保存草稿] [发布文章]  │
    └─────────────────────────────────────────────────┘

  关键技术:
    - marked 库 → Markdown 文本 → HTML 实时转换
    - v-html → 将渲染后的 HTML 插入预览区域
    - textarea ref → 获取光标位置，实现工具栏插入
    - categoryStore → 共享分类数据，与侧边栏同步
  
  标签管理:
    - 文章标签在撰写完成后由用户手动添加
    - 输入标签名 → 回车/点击添加 → 标签芯片显示
    - 点击 × 删除标签
  ============================================================
-->
<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { renderMarkdown } from '../utils/marked.js'
import { categoryStore } from '../store/categories.js'
import { getPostById, updatePost, addPost } from '../store/articles.js'

const router = useRouter()
const route = useRoute()

const editingId = computed(() => {
  const raw = route.query.edit
  return raw ? parseInt(raw) : null
})

const isEditing = computed(() => editingId.value !== null)

const title = ref('')
const content = ref('')
const selectedCategory = ref('')
const articleTags = ref([])
const tagInput = ref('')
const newCategoryInput = ref('')
const showCategoryDropdown = ref(false)

const toggleCategoryDropdown = () => {
  showCategoryDropdown.value = !showCategoryDropdown.value
}

const selectCategory = (cat) => {
  selectedCategory.value = cat
  showCategoryDropdown.value = false
}

const textareaRef = ref(null)

/*
 * ======================== 字数统计 ========================
 * 统计正文（content）的字符数，不含空格换行
 */
const wordCount = computed(() => {
  return content.value.replace(/\s/g, '').length
})

/*
 * ======================== 发布按钮是否可用 ========================
 * 条件：标题非空 + 已选分类 + 正文非空
 */
const canPublish = computed(() => {
  return title.value.trim() !== ''
    && selectedCategory.value !== ''
    && content.value.trim() !== ''
})

/*
 * ======================== Markdown → HTML 实时转换 ========================
 * computed 自动追踪 content 变化，每次变化重新调用 marked.parse()
 * 无内容时显示占位提示
 */
const renderedHTML = computed(() => {
  if (!content.value.trim()) {
    return '<p class="preview-placeholder">在右侧编辑区输入 Markdown 内容，此处将实时预览...</p>'
  }
  return renderMarkdown(content.value)
})

/*
 * ======================== 标签管理 ========================
 */

/*
 * addTag - 添加标签
 * 校验：输入不能为空、不能与已有标签重复
 * 添加后清空输入框
 */
const addTag = () => {
  const tag = tagInput.value.trim()
  if (!tag) return
  if (articleTags.value.includes(tag)) {
    tagInput.value = ''
    return
  }
  articleTags.value.push(tag)
  tagInput.value = ''
}

const removeCategory = (cat) => {
  categoryStore.removeCustom(cat)
  if (selectedCategory.value === cat) selectedCategory.value = ''
}

/*
 * removeTag - 删除指定标签
 */
const removeTag = (tag) => {
  articleTags.value = articleTags.value.filter(t => t !== tag)
}

/*
 * ======================== 分类管理 ========================
 */

/*
 * addCustomCategory - 在写文章页面新建分类
 * 写入共享 store，自动同步到侧边栏
 */
const addCustomCategory = () => {
  const cat = newCategoryInput.value.trim()
  if (!cat) return
  categoryStore.addCustom(cat)
  selectedCategory.value = cat
  newCategoryInput.value = ''
}

/*
 * ======================== 工具栏：Markdown 语法插入 ========================
 *
 * insertMarkdown(syntax) - 在光标位置插入 Markdown 语法
 * 
 * 工作原理：
 *   1. 获取 textarea 的 selectionStart / selectionEnd（光标位置）
 *   2. 将 content 分为三部分：光标前 + 选中文本 + 光标后
 *   3. 根据 syntax 类型，用对应的包装语法包裹选中文本或插入占位符
 *   4. 更新 content，用 nextTick 等待 DOM 更新后重新设置光标位置
 *
 * 支持的 syntax 类型：
 *   h1/h2/h3   → # / ## / ### 标题
 *   bold       → **加粗**
 *   italic     → *斜体*
 *   code       → `行内代码`
 *   codeblock  → ```代码块```
 *   link       → [文字](url)
 *   image      → ![描述](url)
 *   ul         → - 无序列表
 *   ol         → 1. 有序列表
 *   quote      → > 引用
 *   table      → 表格模板
 *   hr         → --- 分割线
 */
const insertMarkdown = (syntax) => {
  const el = textareaRef.value
  if (!el) return

  const start = el.selectionStart
  const end = el.selectionEnd
  const before = content.value.substring(0, start)
  const selected = content.value.substring(start, end)
  const after = content.value.substring(end)

  let insertion = ''
  let cursorOffset = 0

  const prefix = start === 0 || before.endsWith('\n') ? '' : '\n'

  switch (syntax) {
    case 'h1':
      insertion = `${prefix}# ${selected || '一级标题'}`
      cursorOffset = insertion.length
      break
    case 'h2':
      insertion = `${prefix}## ${selected || '二级标题'}`
      cursorOffset = insertion.length
      break
    case 'h3':
      insertion = `${prefix}### ${selected || '三级标题'}`
      cursorOffset = insertion.length
      break
    case 'bold':
      insertion = `**${selected || '加粗文字'}**`
      cursorOffset = selected ? insertion.length : 2
      break
    case 'italic':
      insertion = `*${selected || '斜体文字'}*`
      cursorOffset = selected ? insertion.length : 1
      break
    case 'code':
      insertion = `\`${selected || '行内代码'}\``
      cursorOffset = selected ? insertion.length : 1
      break
    case 'codeblock':
      insertion = `${prefix}\`\`\`js\n${selected || '// 在此编写代码...'}\n\`\`\``
      cursorOffset = selected ? insertion.length : prefix.length + 5
      break
    case 'link':
      insertion = `[${selected || '链接文字'}](url)`
      cursorOffset = selected ? insertion.length - 1 : insertion.length - 4
      break
    case 'image':
      insertion = `![${selected || '图片描述'}](url)`
      cursorOffset = selected ? insertion.length - 1 : insertion.length - 4
      break
    case 'ul':
      insertion = `${prefix}- ${selected || '列表项'}`
      cursorOffset = insertion.length
      break
    case 'ol':
      insertion = `${prefix}1. ${selected || '列表项'}`
      cursorOffset = insertion.length
      break
    case 'quote':
      insertion = `${prefix}> ${selected || '引用文字'}`
      cursorOffset = insertion.length
      break
    case 'table':
      insertion = `${prefix}| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |`
      cursorOffset = insertion.length
      break
    case 'hr':
      insertion = `${prefix}---`
      cursorOffset = insertion.length
      break
    default:
      return
  }

  content.value = before + insertion + after

  nextTick(() => {
    el.focus()
    const newPos = start + cursorOffset
    el.setSelectionRange(newPos, newPos)
  })
}

/*
 * ======================== Tab 键缩进支持 ========================
const handleKeydown = (e) => {
  if (e.key === 'Tab') {
    e.preventDefault()
    const el = textareaRef.value
    if (!el) return
    const start = el.selectionStart
    const end = el.selectionEnd
    content.value = content.value.substring(0, start) + '  ' + content.value.substring(end)
    nextTick(() => {
      el.focus()
      el.setSelectionRange(start + 2, start + 2)
    })
  }
}

/*
 * ======================== 发布文章 ========================
 * 校验：标题、分类、正文不为空
 * 当前为模拟操作（无后端），控制台输出文章数据
 */
const publishArticle = async () => {
  if (!canPublish.value) return

  try {
    if (isEditing.value) {
      await updatePost(editingId.value, {
        title: title.value,
        category: selectedCategory.value,
        content: content.value,
        tags: articleTags.value,
      })
      alert(`文章「${title.value}」已更新！`)
    } else {
      const article = {
        title: title.value,
        category: selectedCategory.value,
        content: content.value,
        tags: articleTags.value,
        date: new Date().toISOString().slice(0, 10),
        author: 'Admin',
        summary: title.value
      }
      await addPost(article)
      alert(`文章「${title.value}」发布成功！`)
    }
    router.push('/posts')
  } catch (e) {
    console.error('发布失败:', e)
    alert('发布失败，请确认后端服务已启动')
  }
}

/*
 * ======================== 保存草稿 ========================
 * 将文章数据存入 localStorage，key 为 blog-draft
 */
const saveDraft = () => {
  const draft = {
    title: title.value,
    category: selectedCategory.value,
    content: content.value,
    tags: articleTags.value,
    savedAt: new Date().toISOString()
  }
  localStorage.setItem('blog-draft', JSON.stringify(draft))
  alert('草稿已保存！')
}

/*
 * ======================== 恢复草稿 ========================
 * 页面加载时检查 localStorage 是否有草稿
 */
const loadDraft = () => {
  const raw = localStorage.getItem('blog-draft')
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    if (draft.title) title.value = draft.title
    if (draft.category) selectedCategory.value = draft.category
    if (draft.content) content.value = draft.content
    if (draft.tags) articleTags.value = draft.tags
  } catch {
    // 草稿数据损坏，忽略
  }
}

/*
 * ======================== 编辑模式：预填文章数据 ========================
 * 当 URL 包含 ?edit=id 时，加载已有文章内容到表单
 */
if (route.query.edit) {
  const id = parseInt(route.query.edit)
  const existingPost = getPostById(id)
  if (existingPost) {
    title.value = existingPost.title
    content.value = existingPost.content
    selectedCategory.value = existingPost.category
    articleTags.value = [...(existingPost.tags || [])]
  }
}

/*
 * ======================== 恢复草稿（仅新建模式） ========================
 * 编辑模式下不加载草稿
 */
if (!route.query.edit) {
  loadDraft()
}

let dropdownEl = null
const handleClickOutside = (e) => {
  if (dropdownEl && !dropdownEl.contains(e.target)) {
    showCategoryDropdown.value = false
  }
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="write-page">
    <!--
      ======================== 页面头部 ========================
      返回按钮 + 页面标题
    -->
    <div class="write-header">
      <button class="write-back" @click="router.push('/posts')">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M10 4L6 8L10 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>返回博客</span>
      </button>
      <h1 class="write-heading">{{ isEditing ? '编辑文章' : '写文章' }}</h1>
    </div>

    <!--
      ======================== 文章元数据区 ========================
      标题输入 + 分类选择（含新建分类）
    -->
    <div class="write-meta">
      <!-- 标题输入 -->
      <input
        v-model="title"
        type="text"
        class="write-title-input"
        placeholder="输入文章标题..."
        maxlength="200"
      />

      <!-- 分类选择 -->
      <div class="write-category-row">
        <label class="write-label">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <rect x="1" y="1" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            <rect x="8" y="1" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            <rect x="1" y="8" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            <rect x="8" y="8" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
          </svg>
          文章分类
        </label>
        <div class="category-dropdown" ref="el => dropdownEl = el">
          <button class="dropdown-trigger" @click="toggleCategoryDropdown" type="button">
            <span v-if="selectedCategory" class="trigger-text">{{ selectedCategory }}</span>
            <span v-else class="trigger-placeholder">请选择分类</span>
            <svg class="trigger-arrow" :class="{ open: showCategoryDropdown }" width="10" height="6" viewBox="0 0 10 6" fill="none">
              <path d="M1 1L5 5L9 1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <div v-show="showCategoryDropdown" class="dropdown-list">
            <button
              v-for="cat in categoryStore.all"
              :key="cat"
              class="dropdown-item"
              :class="{ active: selectedCategory === cat }"
              @click="selectCategory(cat)"
              type="button"
            >
              <span class="dropdown-item-text">{{ cat }}</span>
              <span
                v-if="categoryStore.customCategories.includes(cat)"
                class="dropdown-item-del"
                @click.stop="removeCategory(cat)"
                title="删除此分类"
              >
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                  <path d="M3 3L9 9M9 3L3 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </span>
            </button>
          </div>
        </div>
        <span class="category-required" v-if="!selectedCategory">* 必选</span>

        <div class="add-category-inline">
          <input
            v-model="newCategoryInput"
            type="text"
            class="add-category-input"
            placeholder="新建分类..."
            @keyup.enter="addCustomCategory"
          />
          <button
            class="add-category-btn"
            :disabled="!newCategoryInput.trim()"
            @click="addCustomCategory"
            title="新建分类"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M7 3V11M3 7H11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!--
      ======================== Markdown 工具栏 ========================
      提供常用 Markdown 语法的快捷插入按钮
      点击后自动在光标位置插入对应语法
    -->
    <div class="write-toolbar">
      <button
        v-for="btn in toolbarButtons"
        :key="btn.syntax"
        class="toolbar-btn"
        :title="btn.title"
        @click="insertMarkdown(btn.syntax)"
        v-html="btn.icon"
      ></button>
      <span class="toolbar-divider"></span>
      <span class="word-count">字数: {{ wordCount }}</span>
    </div>

    <!--
      ======================== 编辑器分栏区域 ========================
      左：预览面板（实时渲染 Markdown）
      右：编辑面板（原生 textarea）
      
      等宽分栏（flex: 1 + flex: 1）
      切换标签：移动端通过 radio 按钮切换显示预览/编辑面板
    -->
    <div class="write-editor">
      <!-- 预览面板 -->
      <div class="editor-pane preview-pane">
        <div class="pane-header">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.2"/>
            <circle cx="7" cy="7" r="2" fill="currentColor" opacity="0.5"/>
          </svg>
          <span>预览</span>
        </div>
        <div
          class="preview-content markdown-body"
          v-html="renderedHTML"
        ></div>
      </div>

      <!-- 编辑面板 -->
      <div class="editor-pane edit-pane">
        <div class="pane-header">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M2 11L2 3L7 1L12 3L12 11L7 13L2 11Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
            <path d="M12 3L7 5.5M2 3L7 5.5M7 5.5V13" stroke="currentColor" stroke-width="1.2"/>
          </svg>
          <span>编辑</span>
        </div>
        <textarea
          ref="textareaRef"
          v-model="content"
          class="edit-textarea"
          placeholder="# 开始写作...

使用 Markdown 语法书写，右侧实时预览效果。

## 标题
**加粗** *斜体* `行内代码`

- 无序列表
1. 有序列表

> 引用文字

[链接](https://example.com)
![图片](https://example.com/image.png)

```js
// 代码块
console.log('Hello World')
```

| 表格 | 列2 |
| --- | --- |
| 内容 | 内容 |
"
          spellcheck="false"
          @keydown="handleKeydown"
        ></textarea>
      </div>
    </div>

    <!--
      ======================== 标签输入区 ========================
      手动为文章添加标签
      回车或点击按钮添加
    -->
    <div class="write-tags-section">
      <label class="write-label">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M1.5 2.5V6.5L7.5 12.5L12.5 7.5L6.5 1.5H2.5C1.95 1.5 1.5 1.95 1.5 2.5Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
          <circle cx="4" cy="4" r="0.8" fill="currentColor"/>
        </svg>
        文章标签
      </label>
      <div class="tags-input-row">
        <input
          v-model="tagInput"
          type="text"
          class="tag-input"
          placeholder="输入标签后按回车添加..."
          @keyup.enter="addTag"
          maxlength="30"
        />
        <button
          class="tag-add-btn"
          :disabled="!tagInput.trim()"
          @click="addTag"
        >
          添加
        </button>
      </div>
      <!-- 标签芯片列表 -->
      <div class="tags-chips" v-if="articleTags.length > 0">
        <span
          v-for="tag in articleTags"
          :key="tag"
          class="tag-chip"
        >
          {{ tag }}
          <button class="tag-chip-remove" @click="removeTag(tag)" title="移除标签">
            <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
              <path d="M2.5 2.5L7.5 7.5M7.5 2.5L2.5 7.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </span>
      </div>
      <p v-else class="tags-hint">在内容撰写完成后为文章添加标签</p>
    </div>

    <!--
      ======================== 底部操作栏 ========================
      保存草稿 → localStorage
      发布文章 → 校验后模拟发布，跳转到博客列表
    -->
    <div class="write-actions">
      <button class="action-btn action-draft" @click="saveDraft">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M2 12V2H9L12 5V12H2Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
          <path d="M4 2V5H9.5" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
          <path d="M4 9H10M4 11H8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
        </svg>
        保存草稿
      </button>
      <button
        class="action-btn action-publish"
        :disabled="!canPublish"
        @click="publishArticle"
      >
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M2 12L2 7L7 2L12 7L12 12H9.5V8.5H4.5V12H2Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
        </svg>
        {{ isEditing ? '更新文章' : '发布文章' }}
      </button>
    </div>
  </div>
</template>

<script>
/*
 * ======================== 工具栏按钮配置（普通 script 块） ========================
 * 使用单独的 <script> 块定义非响应式常量
 * 每个按钮包含：显示图标（SVG HTML）、提示文字、对应 syntax 值
 */
export const toolbarButtons = [
  { syntax: 'h1', title: '一级标题 (H1)', icon: '<b>H1</b>' },
  { syntax: 'h2', title: '二级标题 (H2)', icon: '<b>H2</b>' },
  { syntax: 'h3', title: '三级标题 (H3)', icon: '<b>H3</b>' },
  { syntax: 'bold', title: '加粗 (Ctrl+B)', icon: '<b>B</b>' },
  { syntax: 'italic', title: '斜体 (Ctrl+I)', icon: '<i>I</i>' },
  { syntax: 'code', title: '行内代码', icon: '&lt;/&gt;' },
  { syntax: 'codeblock', title: '代码块', icon: '{ }' },
  { syntax: 'link', title: '链接', icon: '🔗' },
  { syntax: 'image', title: '图片', icon: '🖼' },
  { syntax: 'ul', title: '无序列表', icon: '• ≡' },
  { syntax: 'ol', title: '有序列表', icon: '1.' },
  { syntax: 'quote', title: '引用', icon: '"' },
  { syntax: 'table', title: '表格', icon: '⊞' },
  { syntax: 'hr', title: '分割线', icon: '—' }
]
</script>

<style scoped>
/*
 * ================================================================
 *                    写文章页面样式模块
 * ================================================================
 * 1. 页面容器
 * 2. 页面头部
 * 3. 元数据区（标题 + 分类）
 * 4. 工具栏
 * 5. 编辑器分栏
 * 6. 预览面板 + Markdown 渲染样式
 * 7. 编辑面板
 * 8. 标签输入区
 * 9. 底部操作栏
 * 10. 响应式
 * ================================================================
 */

/*
 * ======================== 1. 页面容器 ========================
 */
.write-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px 40px;
  min-height: 100vh;
  color: #fff;
}

/*
 * ======================== 2. 页面头部 ========================
 */
.write-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.write-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.write-back:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
  background-color: rgba(255, 255, 255, 0.03);
}

.write-heading {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin: 0;
}

/*
 * ======================== 3. 元数据区 ========================
 */
.write-meta {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
  padding: 20px;
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

/* 标题输入 */
.write-title-input {
  width: 100%;
  padding: 12px 0;
  background: none;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff;
  font-size: 20px;
  font-weight: 500;
  outline: none;
  transition: border-color 0.2s;
}

.write-title-input::placeholder {
  color: rgba(255, 255, 255, 0.2);
}

.write-title-input:focus {
  border-bottom-color: rgba(0, 255, 128, 0.4);
}

/* 分类选择行 */
.write-category-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.write-label {
  display: flex;
  align-items: center;
  gap: 5px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  flex-shrink: 0;
}

.category-dropdown {
  position: relative;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 170px;
  padding: 7px 12px;
  background-color: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s;
  font-family: inherit;
}

.dropdown-trigger:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

.dropdown-trigger:focus {
  border-color: rgba(0, 255, 128, 0.4);
  outline: none;
}

.trigger-placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.trigger-text {
  color: #fff;
}

.trigger-arrow {
  color: rgba(255, 255, 255, 0.4);
  transition: transform 0.2s;
  flex-shrink: 0;
}

.trigger-arrow.open {
  transform: rotate(180deg);
}

.dropdown-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 220px;
  overflow-y: auto;
  background: #181818;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 4px;
  z-index: 100;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.dropdown-list::-webkit-scrollbar {
  width: 4px;
}

.dropdown-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 10px;
  background: none;
  border: none;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  text-align: left;
}

.dropdown-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}

.dropdown-item.active {
  background: rgba(0, 255, 128, 0.08);
  color: #00ff80;
}

.dropdown-item-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-item-del {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.2);
  transition: all 0.2s;
  flex-shrink: 0;
  margin-left: 8px;
}

.dropdown-item-del:hover {
  background: rgba(255, 68, 68, 0.2);
  color: #ff4444;
}

.category-required {
  color: rgba(255, 68, 68, 0.6);
  font-size: 12px;
}

/* 新建分类内联区 */
.add-category-inline {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-left: 4px;
}

.add-category-inline .add-category-input {
  width: 120px;
  padding: 6px 10px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  outline: none;
  transition: border-color 0.2s;
}

.add-category-inline .add-category-input::placeholder {
  color: rgba(255, 255, 255, 0.2);
}

.add-category-inline .add-category-input:focus {
  border-color: rgba(0, 255, 128, 0.3);
  border-style: solid;
}

.add-category-inline .add-category-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  background: none;
  border: 1px solid rgba(0, 255, 128, 0.25);
  border-radius: 6px;
  color: #00ff80;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.add-category-inline .add-category-btn:hover:not(:disabled) {
  background-color: rgba(0, 255, 128, 0.1);
  border-color: rgba(0, 255, 128, 0.5);
}

.add-category-inline .add-category-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/*
 * ======================== 4. 工具栏 ========================
 * flex 横向排列，按钮间无间距
 * 分隔线 + 字数统计在右侧
 */
.write-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 8px 12px;
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-bottom: none;
  border-radius: 12px 12px 0 0;
  overflow-x: auto;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 30px;
  padding: 0 7px;
  background: none;
  border: 1px solid transparent;
  border-radius: 5px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}

.toolbar-btn:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

.toolbar-btn:active {
  color: #00ff80;
  background-color: rgba(0, 255, 128, 0.08);
}

.toolbar-divider {
  width: 1px;
  height: 18px;
  background-color: rgba(255, 255, 255, 0.08);
  margin: 0 6px;
}

.word-count {
  margin-left: auto;
  color: rgba(255, 255, 255, 0.25);
  font-size: 12px;
  flex-shrink: 0;
}

/*
 * ======================== 5. 编辑器分栏 ========================
 */
.write-editor {
  display: flex;
  gap: 0;
  min-height: 500px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 0 0 12px 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.editor-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 预览和编辑之间的分隔线 */
.preview-pane {
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

/* 面板头部 */
.pane-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background-color: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  letter-spacing: 0.5px;
}

/*
 * ======================== 6. 预览面板 ========================
 * 注：Markdown 渲染样式已移至下方非 scoped <style> 块，以确保
 *     v-html 注入的 highlight.js 语法高亮样式不被 scoped 覆盖。
 */
.preview-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #0d0d0d;
}

/*
 * ======================== 7. 编辑面板 ========================
 */
.edit-textarea {
  flex: 1;
  padding: 20px;
  background-color: #0d0d0d;
  border: none;
  outline: none;
  resize: none;
  color: rgba(255, 255, 255, 0.75);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  tab-size: 2;
}

.edit-textarea::placeholder {
  color: rgba(255, 255, 255, 0.15);
}

/*
 * 编辑器滚动条
 */
.preview-content::-webkit-scrollbar,
.edit-textarea::-webkit-scrollbar {
  width: 5px;
}

.preview-content::-webkit-scrollbar-thumb,
.edit-textarea::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
}

/*
 * ======================== 8. 标签输入区 ========================
 */
.write-tags-section {
  padding: 16px 20px;
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  margin-bottom: 20px;
}

.tags-input-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.tag-input {
  flex: 1;
  padding: 8px 12px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.tag-input::placeholder {
  color: rgba(255, 255, 255, 0.2);
}

.tag-input:focus {
  border-color: rgba(0, 255, 128, 0.4);
}

.tag-add-btn {
  padding: 8px 16px;
  background-color: rgba(0, 255, 128, 0.08);
  border: 1px solid rgba(0, 255, 128, 0.2);
  border-radius: 6px;
  color: #00ff80;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.tag-add-btn:hover:not(:disabled) {
  background-color: rgba(0, 255, 128, 0.15);
  border-color: rgba(0, 255, 128, 0.4);
}

.tag-add-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 标签芯片 */
.tags-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: rgba(0, 255, 128, 0.06);
  border: 1px solid rgba(0, 255, 128, 0.15);
  border-radius: 4px;
  color: rgba(0, 255, 128, 0.8);
  font-size: 12px;
  transition: all 0.2s;
}

.tag-chip:hover {
  border-color: rgba(0, 255, 128, 0.3);
}

.tag-chip-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  padding: 0;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 0.15s;
}

.tag-chip-remove:hover {
  opacity: 1;
  color: #ff4444;
}

.tags-hint {
  color: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  margin: 10px 0 0 0;
}

/*
 * ======================== 9. 底部操作栏 ========================
 */
.write-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-draft {
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.6);
}

.action-draft:hover {
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
  background-color: rgba(255, 255, 255, 0.03);
}

.action-publish {
  background-color: rgba(0, 255, 128, 0.1);
  border: 1px solid rgba(0, 255, 128, 0.3);
  color: #00ff80;
  font-weight: 500;
}

.action-publish:hover:not(:disabled) {
  background-color: rgba(0, 255, 128, 0.2);
  border-color: rgba(0, 255, 128, 0.5);
}

.action-publish:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/*
 * ======================== 10. 响应式 ========================
 * ≤768px 时：
 *   - 分栏改为上下排列（预览在上，编辑在下）
 *   - 每栏高度 350px
 *   - 工具栏横向滚动
 *   - 分类选择行纵向排列
 */
@media (max-width: 768px) {
  .write-page {
    padding: 70px 12px 30px;
  }

  .write-heading {
    font-size: 18px;
  }

  .write-title-input {
    font-size: 17px;
  }

  .write-editor {
    flex-direction: column;
  }

  .preview-pane {
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .editor-pane {
    min-height: 350px;
  }

  .preview-content,
  .edit-textarea {
    min-height: 350px;
  }

  .write-category-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .dropdown-trigger {
    width: 100%;
  }

  .add-category-inline {
    margin-left: 0;
    width: 100%;
  }

  .add-category-inline .add-category-input {
    flex: 1;
    width: auto;
  }

  .toolbar-btn {
    min-width: 28px;
    height: 28px;
    font-size: 11px;
    padding: 0 5px;
  }

  .write-actions {
    flex-direction: column;
  }

  .action-btn {
    justify-content: center;
  }
}
</style>

<style>
/*
 * ================================================================
 *          Markdown 渲染样式（非 scoped，确保 hljs 语法高亮生效）
 * ================================================================
 * 此块不使用 scoped，因为预览内容通过 v-html 动态注入 DOM，
 * scoped 样式的高 specificity 会覆盖 highlight.js 的主题颜色。
 */

.preview-placeholder {
  color: rgba(255, 255, 255, 0.2);
  font-style: italic;
  text-align: center;
  padding-top: 60px;
}

.markdown-body h1 {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.markdown-body h2 {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  margin: 24px 0 12px 0;
}

.markdown-body h3 {
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 20px 0 10px 0;
}

.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin: 16px 0 8px 0;
}

.markdown-body p {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.8;
  margin: 0 0 14px 0;
}

.markdown-body a {
  color: #00ff80;
  text-decoration: none;
  border-bottom: 1px solid rgba(0, 255, 128, 0.3);
  transition: border-color 0.2s;
}

.markdown-body a:hover {
  border-bottom-color: #00ff80;
}

.markdown-body strong {
  color: #fff;
  font-weight: 600;
}

.markdown-body em {
  color: rgba(255, 255, 255, 0.8);
}

.markdown-body code {
  padding: 3px 8px;
  background-color: rgba(0, 255, 128, 0.08);
  border: 1px solid rgba(0, 255, 128, 0.2);
  border-radius: 6px;
  color: #00ff80;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  font-weight: 500;
}

.markdown-body pre {
  background-color: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin: 0 0 16px 0;
}

.markdown-body pre code {
  padding: 0;
  border: none;
  border-radius: 0;
  background: none;
  color: inherit;
  font-size: 13px;
  line-height: 1.7;
  font-weight: normal;
}

.markdown-body blockquote {
  margin: 0 0 16px 0;
  padding: 8px 16px;
  border-left: 3px solid #00ff80;
  background-color: rgba(0, 255, 128, 0.03);
  border-radius: 0 6px 6px 0;
}

.markdown-body blockquote p {
  color: rgba(255, 255, 255, 0.55);
  margin: 4px 0;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
  margin: 0 0 14px 0;
}

.markdown-body li {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.8;
}

.markdown-body hr {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin: 24px 0;
}

.markdown-body img {
  max-width: 100%;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  margin: 8px 0;
}

.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 16px 0;
}

.markdown-body th,
.markdown-body td {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.markdown-body th {
  background-color: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
}
</style>