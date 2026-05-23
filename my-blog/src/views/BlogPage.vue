<!--
  ============================================================
  组件名称: BlogPage（博客文章列表页）
  功能描述: 展示博客文章列表，包含侧边栏筛选（年份 + 标签组合筛选）、
            文章卡片和分页器。筛选无刷新即时更新，分页自动联动。
  子组件: BlogSidebar - 侧边栏筛选面板
  数据来源: mockPosts 从 ../store/articles.js 共享导入
  设计风格: 与 SplashScreen 保持一致——深色背景 + 绿色点缀
  ============================================================
-->
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BlogSidebar from '../components/BlogSidebar.vue'
import { categoryStore } from '../store/categories.js'
import { mockPosts, articlesLoading, articlesError, initArticles, removePost } from '../store/articles.js'
import { isAdmin } from '../utils/isAdmin.js'

const router = useRouter()

const goToPost = (id) => {
  router.push('/posts/' + id)
}

const goToEdit = (id) => {
  router.push('/write?edit=' + id)
}

const deleteConfirmId = ref(null)

const confirmDelete = (id) => {
  deleteConfirmId.value = id
}

const cancelDelete = () => {
  deleteConfirmId.value = null
}

const doDelete = async () => {
  if (deleteConfirmId.value) {
    await removePost(deleteConfirmId.value)
    deleteConfirmId.value = null
  }
}

/*
 * ======================== 分页配置 ========================
 * pageSize     - 每页显示的文章数量（设为 6 条）
 * currentPage  - 当前页码，从 1 开始
 */
const pageSize = 6
const currentPage = ref(1)

/*
 * ======================== 筛选状态 ========================
 * selectedCategories - 当前选中的分类数组（[] = 不按分类筛选）
 * selectedYear       - 当前选中的年份（null = "全部"，即不按年份筛选）
 * selectedTags       - 当前选中的标签数组（[] = 无标签筛选）
 *
 * v-model 双向绑定原理：
 *   :selectedYear="selectedYear" @update:selectedYear="selectedYear = $event"
 *   在模板中可简写为 v-model:selectedYear="selectedYear"
 */
const selectedCategories = ref([])
const selectedYear = ref(null)
const selectedTags = ref([])

/*
 * ======================== 模拟文章数据 ========================
 * 模拟 12 篇博客文章，数据结构：
 *   id / title / summary / date / tags / author
 *
 * 数据特点：
 *   - 所有文章均为 2026 年，方便验证年份筛选
 *   - 标签覆盖前端/后端/工具/运维等多个领域
 * - 12 篇文章 2 页（每页 6 条），验证分页 + 筛选联动
 */

/*
 * ======================== 从数据中提取筛选选项 ========================
 *
 * allCategories - 所有唯一的分类，从文章 category 字段提取
 * 提取逻辑：mockPosts.map(post => post.category) → Set 去重 → sort 排序
 * 结果示例：['前端', '后端', '工具', '运维']
 *
 * allYears - 所有唯一的年份，从文章日期中提取
 * 提取逻辑：
 *   mockPosts.map(post => post.date.slice(0, 4)) → 取出每篇文章日期的前4位（年份）
 *   [...new Set(...)] → 用 Set 去重，再展开为数组
 *   .sort().reverse() → 按年份降序排列（最新的在最前面）
 * 结果示例：['2026']
 *
 * allTags - 所有唯一的标签，从文章标签中汇总
 * 提取逻辑：
 *   mockPosts.flatMap(post => post.tags) → 将所有文章的标签数组展平为一维数组
 *   [...new Set(...)] → 用 Set 去重
 *   .sort() → 按字母/中文排序
 * 结果示例：['API', 'CSS', 'Docker', 'Git', 'Redis', ...]
 */
const allCategories = computed(() => {
  const categories = mockPosts.map(post => post.category)
  return [...new Set(categories)].sort()
})

const allYears = computed(() => {
  const years = mockPosts.map(post => post.date.slice(0, 4))
  return [...new Set(years)].sort().reverse()
})

const allTags = computed(() => {
  const tags = mockPosts.flatMap(post => post.tags || [])
  return [...new Set(tags)].sort()
})

/*
 * ======================== 筛选逻辑 ========================
 *
 * filteredPosts - 根据 selectedCategories、selectedYear 和 selectedTags 筛选后的文章列表
 *
 * 筛选规则（三个维度组合筛选，取 AND 交集）：
 *   1. 分类筛选：如果 selectedCategories 不为空，只保留分类匹配的文章
 *   2. 年份筛选：如果 selectedYear 不为 null，只保留该年份的文章
 *   3. 标签筛选：如果 selectedTags 不为空，只保留至少含有一个选中标签的文章
 *   三个维度同时启用时取交集（AND 关系）
 *   如果某个维度为空/为 null，则忽略该维度
 *
 * computed 的优势：
 *   Vue 的 computed 会自动追踪依赖（selectedCategories、selectedYear、selectedTags）
 *   依赖变化时自动重新计算，且结果会被缓存
 */
const filteredPosts = computed(() => {
  let posts = mockPosts

  if (selectedCategories.value.length > 0) {
    posts = posts.filter(post => selectedCategories.value.includes(post.category))
  }

  if (selectedYear.value) {
    posts = posts.filter(post => post.date.startsWith(selectedYear.value))
  }

  if (selectedTags.value.length > 0) {
    posts = posts.filter(post =>
      (post.tags || []).some(tag => selectedTags.value.includes(tag))
    )
  }

  return posts
})

/*
 * ======================== 分页计算 ========================
 *
 * totalPages - 基于筛选后文章总数计算的总页数
 * 计算：Math.ceil(筛选后文章数 / 每页数量)
 * 筛选后文章数变化 → totalPages 自动更新 → 分页器自动调整
 */
const totalPages = computed(() => Math.ceil(filteredPosts.value.length / pageSize))

/*
 * pagedPosts - 当前页的文章列表（从筛选结果中截取）
 *
 * 分页截取逻辑：
 *   start = (currentPage - 1) * pageSize → 当前页起始索引
 *   end   = currentPage * pageSize       → 当前页结束索引（不含）
 *   第 1 页：slice(0, 6)，第 2 页：slice(6, 12)
 */
const pagedPosts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredPosts.value.slice(start, start + pageSize)
})

/*
   * ======================== 初始化分类共享 store ========================
   * 将文章数据中提取的分类设置到共享 store，
   * 供 BlogSidebar 和 WritePage 等组件读取
   */
onMounted(() => {
  categoryStore.setDefaults(allCategories.value)
})

const retryLoad = () => {
  initArticles()
}

/*
 * ======================== 筛选变化时自动重置页码 ========================
 *
 * 为什么需要这个 watch：
 *   如果用户在第 2 页，然后选择了一个标签筛选，筛选后可能只有 3 篇文章
 *   （不足 1 页），此时 currentPage 仍为 2，但第 2 页为空。
 *
 * watch 监听 filteredPosts（筛选结果变化），自动将 currentPage 重置为 1
 * 确保筛选后始终从第 1 页开始显示。
 */
watch(filteredPosts, () => {
  currentPage.value = 1
})

/*
 * ======================== 分页操作方法 ========================
 */

/*
 * goToPage - 跳转到指定页码
 * 参数：page - 目标页码
 * 边界保护：确保 page 在 [1, totalPages] 范围内
 * 跳转后自动滚回顶部（smooth 平滑滚动）
 */
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}
</script>

<template>
  <!--
    ======================== 博客页面容器 ========================
    桌面端布局：flex 横向排列 → 侧边栏 + 主内容区
    min-height: 100vh → 至少占满一屏
    padding: 80px 24px 60px → 导航栏空间 + 页面呼吸空间
    max-width: 1100px → 比以前更宽（800px），因为需要容纳侧边栏
    margin: 0 auto → 水平居中
    
    gap: 40px → 侧边栏与主内容区之间的间距
  -->
  <div class="blog-page">
    <!--
      ======================== 侧边栏 ========================
      BlogSidebar 负责展示筛选选项并 emit 用户选择
      BlogPage 负责接收选择并执行筛选逻辑
      
      v-model:selectedYear → 双向绑定年份选择
      v-model:selectedTags → 双向绑定标签选择
      :allYears / :allTags   → 传递可选的数据给侧边栏渲染
    -->
    <BlogSidebar
      v-model:selected-categories="selectedCategories"
      v-model:selected-year="selectedYear"
      v-model:selected-tags="selectedTags"
      :all-categories="allCategories"
      :all-years="allYears"
      :all-tags="allTags"
    />

    <!--
      ======================== 主内容区 ========================
      flex: 1 → 占据剩余空间（除侧边栏 260px + 40px gap 外的全部）
      min-width: 0 → 防止 flex 子元素内容溢出（flex 布局常见坑）
    -->
    <div class="blog-main">
      <!--
        ======================== 页面头部 ========================
      -->
      <div class="page-header">
        <h1 class="page-title">博客</h1>
        <p class="page-desc">记录学习心得与技术分享</p>
        <div class="header-divider"></div>
      </div>

      <div v-if="articlesLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p class="loading-text">加载文章中...</p>
      </div>

      <div v-else-if="articlesError" class="error-state">
        <div class="error-icon">⚠️</div>
        <p class="error-title">加载失败</p>
        <p class="error-desc">{{ articlesError }}</p>
        <button class="retry-btn" @click="retryLoad">重新加载</button>
      </div>

      <!--
        ======================== 空状态提示 ========================
        当筛选后没有匹配的文章时显示
        v-if="filteredPosts.length === 0" → 筛选结果为空
        
        显示内容：
          主提示："暂无匹配的文章"
          副提示："请尝试调整筛选条件"
          操作按钮："清除筛选" → 一键重置
      -->
      <div v-if="filteredPosts.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p class="empty-title">暂无匹配的文章</p>
        <p class="empty-desc">请尝试调整筛选条件</p>
        <button class="empty-btn" @click="selectedCategories = []; selectedYear = null; selectedTags = []">
          清除筛选
        </button>
      </div>

      <!--
        ======================== 文章列表 ========================
      -->
      <div v-else class="post-list">
        <article
          v-for="post in pagedPosts"
          :key="post.id"
          class="post-card"
          @click="goToPost(post.id)"
        >
          <div class="post-card-header">
            <h2 class="post-title">
              <router-link :to="'/posts/' + post.id" class="post-title-link">{{ post.title }}</router-link>
            </h2>
            <div v-if="isAdmin()" class="post-actions" @click.stop>
              <button class="action-icon edit-icon" @click="goToEdit(post.id)" title="编辑文章">
                <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
                  <path d="M10.5 2L13 4.5L5 12.5L2 13L2.5 10L10.5 2Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>
                </svg>
              </button>
              <button class="action-icon delete-icon" @click="confirmDelete(post.id)" title="删除文章">
                <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
                  <path d="M2.5 4.5H12.5M5 4.5V2.5H10V4.5M6 7.5V11M9 7.5V11M3.5 4.5L4 12.5H11L11.5 4.5Z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="post-meta">
            <span class="post-date">{{ post.date }}</span>
            <span class="post-separator">·</span>
            <span class="post-author">{{ post.author }}</span>
          </div>

          <p class="post-summary">{{ post.summary }}</p>

          <div class="post-tags" v-if="post.tags && post.tags.length > 0">
            <span
              v-for="tag in post.tags"
              :key="tag"
              class="post-tag"
            >{{ tag }}</span>
          </div>
        </article>
      </div>

      <!--
        ======================== 分页器 ========================
        仅当筛选后文章数超过一页时显示
        上一页 / 页码们 / 下一页
      -->
      <div v-if="totalPages > 1" class="pagination">
        <button
          class="page-btn"
          :disabled="currentPage === 1"
          @click="prevPage"
        >
          &lt; 上一页
        </button>

        <button
          v-for="page in totalPages"
          :key="page"
          class="page-btn"
          :class="{ active: page === currentPage }"
          @click="goToPage(page)"
        >
          {{ page }}
        </button>

        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="nextPage"
        >
          下一页 &gt;
        </button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="deleteConfirmId" class="delete-overlay" @click="cancelDelete">
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
 *                      博客页面样式模块
 * ================================================================
 * 1. 页面容器：flex 横向布局（侧边栏 + 主内容）
 * 2. 主内容区：flex: 1 自适应宽度
 * 3. 页面头部：标题 + 描述 + 分割线
 * 4. 文章卡片：样式 + 悬停效果
 * 5. 文章内容：标题 / 元信息 / 摘要 / 标签
 * 6. 空状态提示
 * 7. 分页器
 * 8. 响应式适配
 * ================================================================
 */

/*
 * ======================== 1. 页面容器 ========================
 * display: flex → 侧边栏和主内容区横向排列
 * gap: 40px     → 两者之间的间距
 * align-items: flex-start → 两者从顶部对齐
 *
 * max-width: 1100px → 侧边栏 260px + gap 40px + 主内容 ~800px
 */
.blog-page {
  display: flex;
  gap: 40px;
  align-items: flex-start;
  min-height: 100vh;
  background-color: #0a0a0a;
  padding: 80px 24px 60px;
  max-width: 1100px;
  margin: 0 auto;
}

/*
 * ======================== 2. 主内容区 ========================
 * flex: 1 → 占据侧边栏之外的所有剩余空间
 * min-width: 0 → 至关重要！防止内容溢出
 *   
 *   为什么需要 min-width: 0？
 *   flex 子元素默认 min-width: auto，会基于内容的最小宽度计算
 *   当内容（如长标题、长摘要）超出容器时，flex 子元素会撑破布局
 *   设置 min-width: 0 后，子元素可以缩小到 0，overflow 机制才能生效
 */
.blog-main {
  flex: 1;
  min-width: 0;
}

/*
 * ======================== 3. 页面头部 ========================
 */
.page-header {
  margin-bottom: 40px;
}

.page-title {
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  letter-spacing: 1px;
}

.page-desc {
  color: rgba(255, 255, 255, 0.45);
  font-size: 15px;
  margin: 0 0 20px 0;
}

/*
 * 标题下方分割线
 * 绿色渐变 → 透明，比纯色线更有设计感
 */
.header-divider {
  height: 1px;
  background: linear-gradient(
    to right,
    rgba(0, 255, 128, 0.3),
    transparent
  );
}

/*
 * ======================== 4. 文章卡片 ========================
 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  background-color: #0f0f0f;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: border-color 0.3s ease, transform 0.3s ease;
}

.post-card:hover {
  border-color: rgba(0, 255, 128, 0.3);
  transform: translateX(4px);
}

/*
 * ======================== 5. 文章内容 ========================
 */

/* 5a. 卡片头部（标题 + 操作按钮） */
.post-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

/* 5b. 标题 */
.post-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}

.post-title-link {
  color: #fff;
  text-decoration: none;
  transition: color 0.3s ease;
}

.post-title-link:hover {
  color: #00ff80;
}

/* 5c. 操作按钮（编辑/删除） */
.post-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  padding-top: 2px;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  transition: all 0.2s;
}

.action-icon:hover {
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.edit-icon:hover {
  border-color: rgba(0, 255, 128, 0.3);
  color: #00ff80;
  background-color: rgba(0, 255, 128, 0.06);
}

.delete-icon:hover {
  border-color: rgba(255, 68, 68, 0.3);
  color: #ff4444;
  background-color: rgba(255, 68, 68, 0.06);
}

/* 5d. 元信息（日期 + 作者） */
.post-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

.post-separator {
  color: rgba(255, 255, 255, 0.15);
}

/* 5c. 摘要（2行截断） */
.post-summary {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  line-height: 1.7;
  margin: 0 0 14px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 5d. 标签 */
.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.post-tag {
  display: inline-block;
  padding: 3px 10px;
  background-color: rgba(0, 255, 128, 0.08);
  border: 1px solid rgba(0, 255, 128, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: rgba(0, 255, 128, 0.8);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  gap: 16px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: #00ff80;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: rgba(255, 255, 255, 0.35);
  font-size: 14px;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  text-align: center;
}

.error-icon {
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.7;
}

.error-title {
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
  margin: 0 0 6px 0;
}

.error-desc {
  color: rgba(255, 255, 255, 0.35);
  font-size: 13px;
  margin: 0 0 20px 0;
}

.retry-btn {
  padding: 8px 20px;
  border: 1px solid rgba(0, 255, 128, 0.3);
  border-radius: 8px;
  background-color: transparent;
  color: #00ff80;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-btn:hover {
  background-color: rgba(0, 255, 128, 0.1);
  border-color: rgba(0, 255, 128, 0.6);
}

/*
 * ======================== 6. 空状态提示 ========================
 * 当筛选后没有匹配文章时显示
 *
 * align-items: center → 图标和文字水平居中
 * padding: 60px 0 → 足够的纵向留白，不显得空旷
 *
 * 设计思路：
 *   不应该是冰冷的"无数据"，而是一个友好的引导提示
 *   图标 + 主提示 + 副提示 + 操作按钮 → 完整的用户引导链路
 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  text-align: center;
}

/* 空状态图标 */
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

/* 主提示文字 */
.empty-title {
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
  margin: 0 0 8px 0;
}

/* 副提示文字 */
.empty-desc {
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
  margin: 0 0 20px 0;
}

/* "清除筛选" 操作按钮 */
.empty-btn {
  padding: 8px 20px;
  border: 1px solid rgba(0, 255, 128, 0.3);
  border-radius: 8px;
  background-color: transparent;
  color: #00ff80;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.empty-btn:hover {
  background-color: rgba(0, 255, 128, 0.1);
  border-color: rgba(0, 255, 128, 0.6);
}

/*
 * ======================== 7. 分页器 ========================
 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 40px;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background-color: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.page-btn.active {
  background-color: rgba(0, 255, 128, 0.15);
  border-color: rgba(0, 255, 128, 0.5);
  color: #00ff80;
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/*
 * ======================== 8. 响应式适配 ========================
 *
 * 屏幕宽度 ≤ 768px：
 *   1. 布局改为纵向（flex-direction: column）
 *      侧边栏在上方，主内容在下方
 *   2. 侧边栏变为全宽（width: 100%），失去 sticky 定位
 *      转为顶部可折叠面板（BlogSidebar 内部处理）
 *   3. 减小整体内边距
 *   4. 缩小标题字号
 *   5. 减小卡片内边距
 */
@media (max-width: 768px) {
  .blog-page {
    flex-direction: column;
    padding: 70px 16px 40px;
    gap: 0;
  }

  .page-title {
    font-size: 24px;
  }

  .post-card {
    padding: 18px;
  }

  .post-title {
    font-size: 16px;
  }

  .post-summary {
    font-size: 13px;
  }

  .empty-state {
    padding: 40px 0;
  }
}
</style>