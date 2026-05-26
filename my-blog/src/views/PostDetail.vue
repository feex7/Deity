<!--
  ============================================================
  组件名称: PostDetail（博客文章详情页）
  功能描述: 展示单篇文章的完整内容，包含标题、发布时间、
            作者、分类、标签、Markdown 正文渲染

  路由参数:
    /posts/:id → 通过 $route.params.id 获取文章 id

  数据来源:
    getPostById(id) → 从 ../store/articles.js 按 id 查找文章
    
  内容渲染:
    marked 库将 content（Markdown 文本）转换为 HTML
    v-html 在 .markdown-body 容器内渲染

  页面布局:
    ┌─────────────────────────────────────────────┐
    │  ← 返回列表                                 │
    │─────────────────────────────────────────────│
    │  文章标题 (H1, 大号字体)                     │
    │  发布日期 · 作者 · 分类标签                  │
    │  ───────────────────────────────────────    │
    │                                              │
    │  正文内容（Markdown 渲染）                   │
    │                                              │
    │─────────────────────────────────────────────│
    │  上一篇 ← → 下一篇                           │
    └─────────────────────────────────────────────┘
  ============================================================
-->
<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { renderMarkdown } from '../utils/marked.js'
import { getPostById, mockPosts, removePost } from '../store/articles.js'
import { isAdmin } from '../utils/isAdmin.js'

const route = useRoute()
const router = useRouter()

const postId = computed(() => parseInt(route.params.id))
const post = computed(() => getPostById(postId.value))

const renderedContent = computed(() => {
  if (!post.value || !post.value.content) return ''
  return renderMarkdown(post.value.content)
})

const prevPost = computed(() => {
  if (!post.value) return null
  const prev = mockPosts.filter(p => p.id < post.value.id)
  return prev.length > 0 ? prev[prev.length - 1] : null
})

const nextPost = computed(() => {
  if (!post.value) return null
  return mockPosts.find(p => p.id > post.value.id) || null
})

const goToEdit = () => {
  if (post.value) {
    router.push('/write?edit=' + post.value.id)
  }
}

const deleteConfirmVisible = ref(false)

const confirmDelete = () => {
  deleteConfirmVisible.value = true
}

const cancelDelete = () => {
  deleteConfirmVisible.value = false
}

const doDelete = async () => {
  if (post.value) {
    await removePost(post.value.id)
    router.push('/posts')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const [year, month, day] = dateStr.split('-')
  return `${year}年${parseInt(month)}月${parseInt(day)}日`
}
</script>

<template>
  <div class="post-detail">
    <!--
      ======================== 页面头部：返回按钮 ========================
    -->
    <div class="detail-header">
      <button class="back-btn" @click="router.push('/posts')">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M10 4L6 8L10 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>返回列表</span>
      </button>
      <div v-if="isAdmin()" class="detail-actions">
        <button class="detail-action-btn edit-action" @click="goToEdit" title="编辑文章">
          <svg width="14" height="14" viewBox="0 0 15 15" fill="none">
            <path d="M10.5 2L13 4.5L5 12.5L2 13L2.5 10L10.5 2Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>
          </svg>
          <span>编辑</span>
        </button>
        <button class="detail-action-btn delete-action" @click="confirmDelete" title="删除文章">
          <svg width="14" height="14" viewBox="0 0 15 15" fill="none">
            <path d="M2.5 4.5H12.5M5 4.5V2.5H10V4.5M6 7.5V11M9 7.5V11M3.5 4.5L4 12.5H11L11.5 4.5Z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>删除</span>
        </button>
      </div>
    </div>

    <!--
      ======================== 文章不存在时的提示 ========================
    -->
    <div v-if="!post" class="not-found">
      <div class="nf-icon">📄</div>
      <h2>文章不存在</h2>
      <p>该文章可能已被删除或链接已失效</p>
      <button class="nf-btn" @click="router.push('/posts')">返回博客列表</button>
    </div>

    <!--
      ======================== 文章内容 ========================
    -->
    <article v-else class="article-content">
      <!--
        ======================== 文章标题 ========================
      -->
      <h1 class="article-title">{{ post.title }}</h1>

      <!--
        ======================== 文章元信息 ========================
        发布日期 · 作者 · 分类
      -->
      <div class="article-meta">
        <span class="meta-item meta-date">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <rect x="1.5" y="2.5" width="11" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/>
            <path d="M1.5 6H12.5" stroke="currentColor" stroke-width="1.2"/>
            <path d="M4.5 1V4M9.5 1V4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
          {{ formatDate(post.date) }}
        </span>
        <span class="meta-separator">·</span>
        <span class="meta-item meta-author">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <circle cx="7" cy="5" r="2.5" stroke="currentColor" stroke-width="1.2"/>
            <path d="M2 12.5C2 10 4.5 8 7 8C9.5 8 12 10 12 12.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
          {{ post.author }}
        </span>
        <span class="meta-separator">·</span>
        <span class="meta-item meta-category">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <rect x="1" y="1" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            <rect x="8" y="1" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            <rect x="1" y="8" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            <rect x="8" y="8" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
          </svg>
          {{ post.category }}
        </span>
      </div>

      <!--
        ======================== 分割线 ========================
      -->
      <div class="article-divider"></div>

      <!--
        ======================== Markdown 正文渲染 ========================
        markdown-body 类包含完整的 Markdown 元素样式
        v-html 将 renderedContent（HTML）直接插入 DOM
      -->
      <div class="markdown-body" v-html="renderedContent"></div>

      <!--
        ======================== 文章标签 ========================
        仅在有标签时显示
      -->
      <div class="article-tags" v-if="post.tags && post.tags.length > 0">
        <span class="article-tags-label">标签：</span>
        <span
          v-for="tag in post.tags"
          :key="tag"
          class="article-tag"
        >{{ tag }}</span>
      </div>

      <!--
        ======================== 文章底部操作 ========================
      -->
      <div class="article-footer">
        <button class="back-to-list" @click="router.push('/posts')">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M10 4L6 8L10 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回博客列表
        </button>
      </div>

      <!--
        ======================== 上一篇 / 下一篇导航 ========================
        文章底部的前后导航链接
      -->
      <nav class="post-nav">
        <router-link
          v-if="prevPost"
          :to="'/posts/' + prevPost.id"
          class="post-nav-link post-nav-prev"
        >
          <span class="nav-label">上一篇</span>
          <span class="nav-title">{{ prevPost.title }}</span>
        </router-link>
        <div v-else class="post-nav-link post-nav-placeholder"></div>

        <router-link
          v-if="nextPost"
          :to="'/posts/' + nextPost.id"
          class="post-nav-link post-nav-next"
        >
          <span class="nav-label">下一篇</span>
          <span class="nav-title">{{ nextPost.title }}</span>
        </router-link>
        <div v-else class="post-nav-link post-nav-placeholder"></div>
      </nav>
    </article>

    <Teleport to="body">
      <div v-if="deleteConfirmVisible" class="delete-overlay" @click="cancelDelete">
        <div class="delete-dialog" @click.stop>
          <p class="delete-dialog-title">确认删除</p>
          <p class="delete-dialog-desc">删除后无法恢复，确定要删除这篇文章吗？</p>
          <div class="delete-dialog-actions">
            <button class="delete-dialog-btn btn-cancel" @click="cancelDelete">取消</button>
            <button class="delete-dialog-btn btn-confirm" @click="doDelete">确认删除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/*
 * ================================================================
 *                    文章详情页样式模块
 * ================================================================
 * 1. 页面容器
 * 2. 头部返回按钮
 * 3. 404 提示
 * 4. 文章标题 + 元信息
 * 5. Markdown 正文渲染样式
 * 6. 文章标签
 * 7. 文章底部
 * 8. 上下篇导航
 * 9. 响应式
 * ================================================================
 */

/*
 * ======================== 1. 页面容器 ========================
 */
.post-detail {
  max-width: 750px;
  margin: 0 auto;
  padding: 80px 24px 60px;
  min-height: 100vh;
  color: #fff;
}

/*
 * ======================== 2. 头部返回按钮 ========================
 */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.detail-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-action-btn:hover {
  border-color: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.edit-action:hover {
  border-color: rgba(0, 255, 128, 0.3);
  color: #00ff80;
  background-color: rgba(0, 255, 128, 0.06);
}

.delete-action:hover {
  border-color: rgba(255, 68, 68, 0.3);
  color: #ff4444;
  background-color: rgba(255, 68, 68, 0.06);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
  background-color: rgba(255, 255, 255, 0.03);
}

/*
 * ======================== 3. 文章不存在 ========================
 */
.not-found {
  text-align: center;
  padding: 80px 0;
}

.nf-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.not-found h2 {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 8px 0;
}

.not-found p {
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
  margin: 0 0 24px 0;
}

.nf-btn {
  padding: 8px 20px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.nf-btn:hover {
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

/*
 * ======================== 4. 文章标题 + 元信息 ========================
 */
.article-content {
  animation: fadeInUp 0.4s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.article-title {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  line-height: 1.4;
  margin: 0 0 16px 0;
  letter-spacing: 0.5px;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

.meta-separator {
  color: rgba(255, 255, 255, 0.15);
}

.article-divider {
  height: 1px;
  background: linear-gradient(
    to right,
    rgba(0, 255, 128, 0.3),
    rgba(0, 255, 128, 0.05),
    transparent
  );
  margin-bottom: 32px;
}

/*
 * ======================== 5. Markdown 正文渲染样式 ========================
 * 覆盖 marked 生成的所有 HTML 元素样式
 * 与写文章页面预览的样式保持一致
 */
.markdown-body :deep(h1) {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 32px 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.markdown-body :deep(h2) {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  margin: 28px 0 12px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.markdown-body :deep(h3) {
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 24px 0 10px 0;
}

.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin: 20px 0 8px 0;
}

.markdown-body :deep(p) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
  line-height: 1.9;
  margin: 0 0 18px 0;
}

.markdown-body :deep(a) {
  color: #00ff80;
  text-decoration: none;
  border-bottom: 1px solid rgba(0, 255, 128, 0.3);
  transition: border-color 0.2s;
}

.markdown-body :deep(a:hover) {
  border-bottom-color: #00ff80;
}

.markdown-body :deep(strong) {
  color: #fff;
  font-weight: 600;
}

.markdown-body :deep(em) {
  color: rgba(255, 255, 255, 0.8);
}

/* 行内代码 */
.markdown-body :deep(code) {
  padding: 2px 6px;
  background-color: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  color: #00ff80;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

/* 代码块 */
.markdown-body :deep(pre) {
  background-color: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 18px;
  overflow-x: auto;
  margin: 0 0 20px 0;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: none;
  border: none;
  color: unset;
  font-size: 13px;
  line-height: 1.8;
}

/* 引用块 */
.markdown-body :deep(blockquote) {
  margin: 0 0 20px 0;
  padding: 12px 20px;
  border-left: 3px solid #00ff80;
  background-color: rgba(0, 255, 128, 0.03);
  border-radius: 0 8px 8px 0;
}

.markdown-body :deep(blockquote p) {
  color: rgba(255, 255, 255, 0.55);
  margin: 4px 0;
}

/* 列表 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 0 0 18px 0;
}

.markdown-body :deep(li) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
  line-height: 1.9;
}

/* 分割线 */
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin: 28px 0;
}

/* 图片 */
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  margin: 12px 0;
}

/* 表格 */
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 20px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.markdown-body :deep(th) {
  background-color: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
}

/*
 * ======================== 6. 文章标签 ========================
 */
.article-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.article-tags-label {
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
}

.article-tag {
  padding: 3px 10px;
  background-color: rgba(0, 255, 128, 0.06);
  border: 1px solid rgba(0, 255, 128, 0.15);
  border-radius: 4px;
  color: rgba(0, 255, 128, 0.7);
  font-size: 12px;
}

/*
 * ======================== 7. 文章底部 ========================
 */
.article-footer {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.back-to-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-to-list:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.25);
  background-color: rgba(255, 255, 255, 0.03);
}

/*
 * ======================== 8. 上下篇导航 ========================
 * 前后两篇文章的链接，左右分布
 */
.post-nav {
  display: flex;
  gap: 16px;
  margin-top: 24px;
}

.post-nav-link {
  flex: 1;
  min-width: 0;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.2s;
}

.post-nav-link:hover {
  border-color: rgba(0, 255, 128, 0.2);
  background-color: rgba(0, 255, 128, 0.03);
}

.post-nav-placeholder {
  visibility: hidden;
}

.post-nav-prev {
  text-align: left;
}

.post-nav-next {
  text-align: right;
}

.nav-label {
  display: block;
  color: rgba(255, 255, 255, 0.25);
  font-size: 11px;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.nav-title {
  display: block;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.post-nav-link:hover .nav-title {
  color: #00ff80;
}

/*
 * ======================== 9. 响应式 ========================
 */
@media (max-width: 768px) {
  .post-detail {
    padding: 70px 16px 40px;
  }

  .article-title {
    font-size: 22px;
  }

  .markdown-body :deep(h1) {
    font-size: 22px;
  }

  .markdown-body :deep(h2) {
    font-size: 19px;
  }

  .markdown-body :deep(pre) {
    padding: 14px;
    font-size: 12px;
  }

  .post-nav {
    flex-direction: column;
  }
}
</style>