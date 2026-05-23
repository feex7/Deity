<!--
  ============================================================
  组件名称: BlogSidebar（博客内容筛选侧边栏）
  功能描述: 提供分类、年份和标签三个维度的文章筛选
            支持收起/展开切换、分类管理、标签搜索、自定义标签
  
  筛选维度:
    1. 分类（多选）- 用户可管理显示哪些分类
    2. 年份（单选）- 从文章日期中提取年份，含"全部"选项
    3. 标签（多选）- 支持搜索过滤 + 手动添加自定义标签
  
  布局策略:
    - 桌面端（>768px）：左侧固定宽度侧边栏，sticky 定位，支持收起/展开
    - 移动端（≤768px）：顶部可折叠面板，默认收起
  
  emits 约定:
    - update:selectedCategories(cat[]) → 分类变化时触发
    - update:selectedYear(year | null)  → 年份变化时触发，null 表示"全部"
    - update:selectedTags(tag[])        → 标签变化时触发
  ============================================================
-->
<script setup>
import { ref, computed } from 'vue'
import { categoryStore } from '../store/categories.js'

/*
 * ======================== Props ========================
 * allCategories  - 所有可选的分类数组，如 ['前端', '后端', '工具']
 * allYears       - 所有可选的年份数组，如 ['2026', '2025']
 * allTags        - 所有可选的标签数组，如 ['Vue3', 'Spring Boot', ...]
 * 
 * selectedCategories - 当前选中的分类数组（[] = 不按分类筛选）
 * selectedYear       - 当前选中的年份（null = "全部"）
 * selectedTags       - 当前选中的标签数组（[] = 不按标签筛选）
 */
const props = defineProps({
  allCategories: { type: Array, default: () => [] },
  allYears: { type: Array, default: () => [] },
  allTags: { type: Array, default: () => [] },
  selectedCategories: { type: Array, default: () => [] },
  selectedYear: { type: [String, null], default: null },
  selectedTags: { type: Array, default: () => [] }
})

/*
 * ======================== Emits ========================
 */
const emit = defineEmits([
  'update:selectedCategories',
  'update:selectedYear',
  'update:selectedTags'
])

/*
 * ======================== 收起/展开状态 ========================
 * collapsed - 侧边栏收起/展开，默认展开
 * 收起时宽度缩小为 ~56px，仅显示图标列
 * 展开时宽度为 260px，显示完整筛选功能
 */
const collapsed = ref(false)

/*
 * ======================== 移动端面板状态 ========================
 */
const filterOpen = ref(false)

/*
 * ======================== 分类管理弹窗 ========================
 * showCategoryManager - 控制分类管理弹窗的显示
 * visibleCategories   - 当前可见的分类列表（由用户自定义）
 *                       初始值 = 全部分类（从 props 传入）
 */
const showCategoryManager = ref(false)
const visibleCategories = ref([...props.allCategories, ...categoryStore.customCategories])

/*
 * ======================== 分类管理：自定义分类 ========================
 * 分类数据使用共享 store（categoryStore），与 WritePage 等组件共用
 * customCategories  → categoryStore.customCategories（reactive 数组）
 * newCategoryInput  → 创建分类弹窗中的输入框绑定值
 * 
 * visibleCategories 需要同时包含：
 *   props.allCategories（来自文章数据的分类）
 *   categoryStore.customCategories（用户自定义的分类）
 */
const newCategoryInput = ref('')

/*
 * ======================== 标签搜索 ========================
 * tagSearchQuery - 标签搜索关键词
 * 标签来源：仅来自文章数据（props.allTags），不含自定义标签
 * 标签区只显示搜索框，输入关键词后才动态展示匹配结果
 */
const tagSearchQuery = ref('')

/*
 * ======================== 搜索过滤后的标签 ========================
 * 根据 tagSearchQuery 实时过滤标签列表
 * 空搜索词时返回空数组（不显示标签，仅当用户搜索时才显示结果）
 * 支持大小写不敏感匹配
 */
const filteredTags = computed(() => {
  const query = tagSearchQuery.value.trim().toLowerCase()
  if (!query) return []
  return props.allTags.filter(tag =>
    tag.toLowerCase().includes(query)
  )
})

/*
 * ======================== 激活的筛选总数 ========================
 * 用于在状态栏显示"已筛选 X 项"
 */
const activeFilterCount = computed(() => {
  let count = 0
  if (props.selectedCategories.length > 0) count += props.selectedCategories.length
  if (props.selectedYear) count += 1
  if (props.selectedTags.length > 0) count += props.selectedTags.length
  return count
})

/*
 * ======================== 分类选择处理（多选） ========================
 * 仅允许选择 visibleCategories 中的分类
 */
const handleCategoryClick = (category) => {
  const cats = new Set(props.selectedCategories)
  if (cats.has(category)) {
    cats.delete(category)
  } else {
    cats.add(category)
  }
  emit('update:selectedCategories', [...cats])
}

/*
 * ======================== 年份选择处理（单选） ========================
 */
const handleYearClick = (year) => {
  if (year === props.selectedYear) {
    emit('update:selectedYear', null)
  } else {
    emit('update:selectedYear', year)
  }
}

/*
 * ======================== 标签选择处理（多选） ========================
 */
const handleTagClick = (tag) => {
  const tags = new Set(props.selectedTags)
  if (tags.has(tag)) {
    tags.delete(tag)
  } else {
    tags.add(tag)
  }
  emit('update:selectedTags', [...tags])
}

/*
 * ======================== 分类管理弹窗操作 ========================
 */

/*
 * toggleCategoryVisibility - 切换分类的显示/隐藏
 * 如果分类已在 visibleCategories 中 → 移除
 * 如果分类不在 visibleCategories 中 → 加入
 * 注意：取消显示某个分类时，同时取消该分类的选中状态
 */
const toggleCategoryVisibility = (category) => {
  const cats = new Set(visibleCategories.value)
  if (cats.has(category)) {
    cats.delete(category)
    if (props.selectedCategories.includes(category)) {
      emit('update:selectedCategories',
        props.selectedCategories.filter(c => c !== category)
      )
    }
  } else {
    cats.add(category)
  }
  visibleCategories.value = [...cats]
}

/* 关闭管理弹窗 */
const closeCategoryManager = () => {
  showCategoryManager.value = false
}

/*
 * addCustomCategory - 创建新的自定义分类（写入共享 store）
 * 校验：
 *   1. 输入不能为空
 *   2. 不能与已有分类重复（数据分类 + 自定义分类）
 * 添加后自动设为可见，清空输入框
 */
const addCustomCategory = () => {
  const cat = newCategoryInput.value.trim()
  if (!cat) return
  const allExisting = [...props.allCategories, ...categoryStore.customCategories]
  if (allExisting.includes(cat)) {
    newCategoryInput.value = ''
    return
  }
  categoryStore.addCustom(cat)
  visibleCategories.value.push(cat)
  newCategoryInput.value = ''
}

/*
 * removeCustomCategory - 删除自定义分类（写入共享 store）
 * 从 store 和 visibleCategories 中同时移除
 * 如果该分类已被选中，同步取消选中状态
 */
const removeCustomCategory = (cat) => {
  categoryStore.removeCustom(cat)
  visibleCategories.value = visibleCategories.value.filter(c => c !== cat)
  if (props.selectedCategories.includes(cat)) {
    emit('update:selectedCategories',
      props.selectedCategories.filter(c => c !== cat)
    )
  }
}

/*
 * ======================== 清除全部筛选 ========================
 */
const clearAll = () => {
  emit('update:selectedCategories', [])
  emit('update:selectedYear', null)
  emit('update:selectedTags', [])
}

</script>

<template>
  <!--
    ======================== 侧边栏容器 ========================
    :class="{ collapsed }" → 收起状态添加 .collapsed 类
    transition: width 0.3s → 宽度变化平滑过渡
  -->
  <aside class="blog-sidebar" :class="{ collapsed }">

    <!--
      ======================== 收起/展开切换按钮 ========================
      始终可见，位于侧边栏顶部
      收起时显示 ▶ 图标（提示可展开），展开时显示 ◀ 图标（提示可收起）
      collapsed 时按钮居中，展开时按钮靠右
    -->
    <button
      class="collapse-toggle"
      :title="collapsed ? '展开侧边栏' : '收起侧边栏'"
      @click="collapsed = !collapsed"
    >
      <!-- 展开状态 → 左箭头（收起） -->
      <svg v-if="!collapsed" width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M10 4L6 8L10 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M14 4L14 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
      <!-- 收起状态 → 右箭头（展开） -->
      <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M6 4L10 8L6 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M2 4L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </button>

    <!--
      ======================== 移动端筛选按钮 ========================
      仅在 ≤768px 屏幕显示
    -->
    <button
      class="sidebar-toggle"
      :class="{ active: filterOpen }"
      @click="filterOpen = !filterOpen"
    >
      <span>筛选文章</span>
      <span class="toggle-arrow">▾</span>
    </button>

    <!--
      ======================== 展开状态：完整筛选内容 ========================
      v-show → 保持 DOM 结构，仅切换可见性
      collapsed 时隐藏，展开 + 移动端 filterOpen 时显示
    -->
    <div
      class="sidebar-body"
      :class="{ open: filterOpen }"
      v-show="!collapsed"
    >

      <!--
        ======================== 筛选状态栏 ========================
        当有激活的筛选条件时显示
      -->
      <div
        v-if="activeFilterCount > 0"
        class="filter-status"
      >
        <span class="filter-count">
          已筛选 {{ activeFilterCount }} 项
        </span>
        <button class="clear-btn" @click="clearAll">清除全部</button>
      </div>

      <!--
        ======================== 分类筛选区 ========================
        多选模式，仅显示 visibleCategories 中的分类
        右上角有"管理"按钮 → 打开分类管理弹窗
      -->
      <div class="filter-section">
        <div class="filter-section-header">
          <h3 class="filter-title">
            <!-- 分类图标 -->
            <svg class="section-icon" width="14" height="14" viewBox="0 0 14 14" fill="none">
              <rect x="1" y="1" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
              <rect x="8" y="1" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
              <rect x="1" y="8" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
              <rect x="8" y="8" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.2"/>
            </svg>
            按分类
          </h3>
          <button class="manage-btn" @click="showCategoryManager = true" title="管理分类">
            <!-- 齿轮图标 -->
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <circle cx="7" cy="7" r="2.5" stroke="currentColor" stroke-width="1.2"/>
              <path d="M7 1V2.5M7 11.5V13M13 7H11.5M2.5 7H1M11.24 2.76L10.18 3.82M3.82 10.18L2.76 11.24M11.24 11.24L10.18 10.18M3.82 3.82L2.76 2.76" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="filter-list">
          <button
            v-for="cat in visibleCategories"
            :key="cat"
            class="filter-btn"
            :class="{ active: selectedCategories.includes(cat) }"
            @click="handleCategoryClick(cat)"
          >
            {{ cat }}
          </button>
        </div>
        <!-- 分类为空时的提示 -->
        <p v-if="visibleCategories.length === 0" class="empty-hint">
          暂无显示的分类，点击齿轮图标管理
        </p>
      </div>

      <!--
        ======================== 年份筛选区 ========================
      -->
      <div class="filter-section">
        <h3 class="filter-title">
          <!-- 日历图标 -->
          <svg class="section-icon" width="14" height="14" viewBox="0 0 14 14" fill="none">
            <rect x="1.5" y="2.5" width="11" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/>
            <path d="M1.5 6H12.5" stroke="currentColor" stroke-width="1.2"/>
            <path d="M4.5 1V4M9.5 1V4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
          按年份
        </h3>
        <div class="filter-list">
          <button
            class="filter-btn"
            :class="{ active: selectedYear === null }"
            @click="handleYearClick(null)"
          >
            全部
          </button>
          <button
            v-for="year in allYears"
            :key="year"
            class="filter-btn"
            :class="{ active: selectedYear === year }"
            @click="handleYearClick(year)"
          >
            {{ year }}
          </button>
        </div>
      </div>

      <!--
        ======================== 标签筛选区 ========================
        仅保留搜索框，标签云不再默认展示
        用户输入关键词后，匹配的标签动态出现在搜索框下方
        标签数据来源于文章（props.allTags），不含手动添加的自定义标签
      -->
      <div class="filter-section">
        <h3 class="filter-title">
          <!-- 标签图标 -->
          <svg class="section-icon" width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M1.5 2.5V6.5L7.5 12.5L12.5 7.5L6.5 1.5H2.5C1.95 1.5 1.5 1.95 1.5 2.5Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
            <circle cx="4" cy="4" r="0.8" fill="currentColor"/>
          </svg>
          按标签
        </h3>

        <!-- 标签搜索框 -->
        <div class="tag-search-wrap">
          <svg class="search-icon" width="14" height="14" viewBox="0 0 14 14" fill="none">
            <circle cx="6" cy="6" r="4" stroke="currentColor" stroke-width="1.2"/>
            <path d="M9.2 9.2L12.5 12.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
          <input
            v-model="tagSearchQuery"
            type="text"
            class="tag-search-input"
            placeholder="搜索标签..."
          />
          <button
            v-if="tagSearchQuery"
            class="search-clear"
            @click="tagSearchQuery = ''"
            title="清除搜索"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M3 3L9 9M9 3L3 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <!-- 搜索结果：仅当搜索词非空时显示 -->
        <div v-if="tagSearchQuery" class="filter-tags">
          <button
            v-for="tag in filteredTags"
            :key="tag"
            class="filter-tag"
            :class="{ active: selectedTags.includes(tag) }"
            @click="handleTagClick(tag)"
          >
            {{ tag }}
          </button>
        </div>

        <!-- 搜索无结果提示 -->
        <p v-if="tagSearchQuery && filteredTags.length === 0" class="empty-hint">
          无匹配的标签
        </p>

        <!-- 无标签数据时的提示 -->
        <p v-if="!tagSearchQuery && allTags.length === 0" class="empty-hint">
          暂无标签，标签将在撰写文章时添加
        </p>
      </div>
    </div>

  </aside>

  <!--
    ======================== 分类管理弹窗 ========================
    Teleport to body → 避免被侧边栏 overflow hidden 裁剪
    遮罩层 + 居中弹窗
    
    功能：
      - 以复选框列表展示所有分类
      - 勾选/取消勾选控制分类的显示/隐藏
      - "全选" / "全不选" 快捷操作
  -->
  <Teleport to="body">
    <div
      v-if="showCategoryManager"
      class="modal-overlay"
      @click.self="closeCategoryManager"
    >
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 class="modal-title">管理分类</h3>
          <button class="modal-close" @click="closeCategoryManager" title="关闭">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <p class="modal-desc">选择要在侧边栏中显示的分类，或创建新分类</p>

        <!-- 快捷操作按钮 -->
        <div class="modal-actions">
          <button
            class="modal-action-btn"
            @click="visibleCategories = [...allCategories, ...categoryStore.customCategories]"
          >
            全选
          </button>
          <button
            class="modal-action-btn"
            @click="visibleCategories = []"
          >
            全不选
          </button>
        </div>

        <!-- 数据分类复选框列表 -->
        <div class="modal-list">
          <label
            v-for="cat in allCategories"
            :key="cat"
            class="category-checkbox"
          >
            <input
              type="checkbox"
              :checked="visibleCategories.includes(cat)"
              @change="toggleCategoryVisibility(cat)"
            />
            <span class="checkbox-mark"></span>
            <span class="checkbox-label">{{ cat }}</span>
          </label>

          <!-- 自定义分类（带删除按钮） -->
          <label
            v-for="cat in categoryStore.customCategories"
            :key="'custom-' + cat"
            class="category-checkbox"
          >
            <input
              type="checkbox"
              :checked="visibleCategories.includes(cat)"
              @change="toggleCategoryVisibility(cat)"
            />
            <span class="checkbox-mark"></span>
            <span class="checkbox-label">{{ cat }}</span>
            <button
              class="category-remove-btn"
              @click.stop="removeCustomCategory(cat)"
              title="删除此分类"
            >
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M3 3L9 9M9 3L3 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </label>
        </div>

        <!-- 空状态 -->
        <p v-if="allCategories.length === 0 && categoryStore.customCategories.length === 0" class="empty-hint">
          暂无分类数据，请在下方创建
        </p>

        <!-- 自定义分类创建区 -->
        <div class="add-category-wrap">
          <input
            v-model="newCategoryInput"
            type="text"
            class="add-category-input"
            placeholder="输入新分类名称..."
            @keyup.enter="addCustomCategory"
          />
          <button
            class="add-category-btn"
            :disabled="!newCategoryInput.trim()"
            @click="addCustomCategory"
            title="创建分类"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M7 3V11M3 7H11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="modal-footer">
          <button class="modal-btn modal-btn-confirm" @click="closeCategoryManager">
            确定
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/*
 * ================================================================
 *                    侧边栏样式模块
 * ================================================================
 * 1. 侧边栏容器 + 收起/展开过渡
 * 2. 收起/展开切换按钮
 * 3. 移动端筛选按钮
 * 4. 筛选内容区
 * 5. 筛选状态栏
 * 6. 筛选区块（分类 / 年份 / 标签）
 * 7. 筛选按钮通用样式
 * 8. 标签搜索框
 * 9. 标签按钮样式
 * 10. 自定义标签添加区
 * 11. 收起态图标列
 * 12. 分类管理弹窗
 * 13. 滚动条美化
 * 14. 响应式
 * ================================================================
 */

/*
 * ======================== 1. 侧边栏容器 ========================
 * transition: width 0.3s → 宽度变化平滑过渡
 * .collapsed 时 width: 56px（仅容纳图标列）
 * 展开时 width: 260px（完整筛选功能）
 * overflow: hidden → 收起时隐藏文字内容
 */
.blog-sidebar {
  width: 260px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
  align-self: flex-start;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.blog-sidebar.collapsed {
  width: 56px;
}

/*
 * ======================== 2. 收起/展开切换按钮 ========================
 * 始终显示在侧边栏顶部
 * 展开时右对齐，收起时居中
 * hover 时颜色变亮 + 背景微现
 */
.collapse-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 36px;
  padding: 0;
  background: none;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  transition: color 0.2s, background-color 0.2s;
  margin-bottom: 8px;
}

.collapse-toggle:hover {
  color: rgba(255, 255, 255, 0.8);
  background-color: rgba(255, 255, 255, 0.03);
}

/* 展开状态：箭头靠右 */
.blog-sidebar:not(.collapsed) .collapse-toggle {
  justify-content: flex-end;
  padding-right: 14px;
}

/*
 * ======================== 3. 移动端筛选按钮 ========================
 */
.sidebar-toggle {
  display: none;
  width: 100%;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: none;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
  letter-spacing: 0.5px;
}

.sidebar-toggle:hover {
  color: #fff;
}

.toggle-arrow {
  font-size: 12px;
  transition: transform 0.3s ease;
}

.sidebar-toggle.active .toggle-arrow {
  transform: rotate(180deg);
}

/*
 * ======================== 4. 筛选内容区 ========================
 * padding → 展开时的内边距
 * .collapsed .sidebar-body → 隐藏内容
 */
.sidebar-body {
  padding: 0 16px 16px;
  transition: max-height 0.35s ease, opacity 0.2s;
}

/*
 * ======================== 5. 筛选状态栏 ========================
 */
.filter-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  margin-bottom: 4px;
}

.filter-count {
  font-size: 12px;
  color: rgba(0, 255, 128, 0.8);
}

.clear-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: color 0.2s;
}

.clear-btn:hover {
  color: #ff4444;
}

/*
 * ======================== 6. 筛选区块 ========================
 */
.filter-section {
  margin-bottom: 24px;
}

/* 区块标题行：标题 + 管理按钮（分类区专用） */
.filter-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.filter-section-header .filter-title {
  margin-bottom: 0;
}

/* 管理按钮（齿轮图标） */
.manage-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  transition: all 0.2s;
}

.manage-btn:hover {
  color: #00ff80;
  border-color: rgba(0, 255, 128, 0.3);
  background-color: rgba(0, 255, 128, 0.05);
}

/* 区块标题 */
.filter-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #00ff80;
  font-size: 13px;
  font-weight: 500;
  margin: 0 0 12px 0;
  letter-spacing: 0.5px;
}

/* 标题旁的小图标 */
.section-icon {
  flex-shrink: 0;
  opacity: 0.8;
}

/* 空提示（无分类 / 无匹配标签） */
.empty-hint {
  color: rgba(255, 255, 255, 0.25);
  font-size: 12px;
  margin: 8px 0 0 0;
  text-align: center;
}

/*
 * ======================== 7. 筛选按钮通用样式（年份/分类用） ========================
 */
.filter-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-btn {
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background-color: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  letter-spacing: 0.3px;
}

.filter-btn:hover {
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.filter-btn.active {
  background-color: rgba(0, 255, 128, 0.1);
  border-color: rgba(0, 255, 128, 0.5);
  color: #00ff80;
  font-weight: 500;
}

/*
 * ======================== 8. 标签搜索框 ========================
 * 相对定位容器 → 搜索图标和清空按钮绝对定位
 * margin-bottom → 与标签云的间距
 * transition → 聚焦时边框变色
 */
.tag-search-wrap {
  position: relative;
  margin-bottom: 12px;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.3);
  pointer-events: none;
}

.tag-search-input {
  width: 100%;
  padding: 7px 30px 7px 30px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  outline: none;
  transition: border-color 0.2s, background-color 0.2s;
}

.tag-search-input::placeholder {
  color: rgba(255, 255, 255, 0.25);
}

.tag-search-input:focus {
  border-color: rgba(0, 255, 128, 0.4);
  background-color: rgba(0, 255, 128, 0.03);
}

/* 清空搜索按钮 */
.search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  border-radius: 50%;
  transition: color 0.2s, background-color 0.2s;
}

.search-clear:hover {
  color: rgba(255, 255, 255, 0.8);
  background-color: rgba(255, 255, 255, 0.08);
}

/*
 * ======================== 9. 标签按钮样式 ========================
 */
.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 160px;
  overflow-y: auto;
}

.filter-tag {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background-color: transparent;
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tag:hover {
  border-color: rgba(0, 255, 128, 0.3);
  color: rgba(0, 255, 128, 0.8);
}

.filter-tag.active {
  background-color: rgba(0, 255, 128, 0.1);
  border-color: rgba(0, 255, 128, 0.5);
  color: #00ff80;
  box-shadow: 0 0 8px rgba(0, 255, 128, 0.1);
}

/*
 * ======================== 10. 分类创建区（管理弹窗内） ========================
 * 输入框 + 创建按钮横向排列
 */
.add-category-wrap {
  display: flex;
  gap: 6px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.add-category-input {
  flex: 1;
  padding: 7px 10px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.02);
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  outline: none;
  min-width: 0;
  transition: border-color 0.2s;
}

.add-category-input::placeholder {
  color: rgba(255, 255, 255, 0.25);
}

.add-category-input:focus {
  border-color: rgba(0, 255, 128, 0.3);
  border-style: solid;
}

.add-category-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(0, 255, 128, 0.25);
  border-radius: 6px;
  background-color: transparent;
  color: #00ff80;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.add-category-btn:hover:not(:disabled) {
  background-color: rgba(0, 255, 128, 0.1);
  border-color: rgba(0, 255, 128, 0.5);
}

.add-category-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 自定义分类删除按钮 */
.category-remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  margin-left: auto;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.2);
  cursor: pointer;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.15s;
}

.category-checkbox:hover .category-remove-btn {
  opacity: 0.6;
}

.category-remove-btn:hover {
  opacity: 1 !important;
  background-color: rgba(255, 68, 68, 0.15);
  color: #ff4444;
}

/*
 * ======================== 12. 分类管理弹窗 ========================
 */

/* 遮罩层 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}

/* 弹窗主体 */
.modal-dialog {
  width: 340px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  background-color: #121212;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 24px;
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to   { transform: translateY(0); opacity: 1; }
}

/* 弹窗头部 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.modal-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.2s;
}

.modal-close:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.2);
  background-color: rgba(255, 255, 255, 0.05);
}

.modal-desc {
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  margin: 0 0 16px 0;
}

/* 快捷操作按钮 */
.modal-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.modal-action-btn {
  padding: 5px 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background-color: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-action-btn:hover {
  border-color: rgba(0, 255, 128, 0.3);
  color: #00ff80;
}

/* 分类列表 */
.modal-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 240px;
  overflow-y: auto;
}

/* 自定义复选框样式 */
.category-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.category-checkbox:hover {
  background-color: rgba(255, 255, 255, 0.03);
}

/* 隐藏原生 checkbox */
.category-checkbox input[type="checkbox"] {
  display: none;
}

/* 自定义复选框图形 */
.checkbox-mark {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  flex-shrink: 0;
  transition: all 0.2s;
  position: relative;
}

/* 勾选状态 */
.category-checkbox input:checked + .checkbox-mark {
  background-color: #00ff80;
  border-color: #00ff80;
}

/* 勾选状态的对勾 */
.category-checkbox input:checked + .checkbox-mark::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 6px;
  height: 10px;
  border: solid #0a0a0a;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

/* 弹窗底部 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.modal-btn {
  padding: 8px 28px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn-confirm {
  background-color: rgba(0, 255, 128, 0.15);
  border: 1px solid rgba(0, 255, 128, 0.3);
  color: #00ff80;
}

.modal-btn-confirm:hover {
  background-color: rgba(0, 255, 128, 0.25);
  border-color: rgba(0, 255, 128, 0.5);
}

/*
 * ======================== 13. 滚动条美化 ========================
 */
.filter-tags,
.blog-sidebar,
.modal-list {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.1) transparent;
}

.filter-tags::-webkit-scrollbar,
.blog-sidebar::-webkit-scrollbar,
.modal-list::-webkit-scrollbar {
  width: 4px;
}

.filter-tags::-webkit-scrollbar-thumb,
.blog-sidebar::-webkit-scrollbar-thumb,
.modal-list::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

/*
 * ======================== 14. 响应式断点 ========================
 */
@media (max-width: 768px) {
  .blog-sidebar {
    width: 100%;
    position: static;
    max-height: none;
    overflow: visible;
    background-color: #0f0f0f;
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 12px;
    margin-bottom: 20px;
  }

  /* 移动端不支持收起态（宽度已经是 100%） */
  .blog-sidebar.collapsed {
    width: 100%;
  }

  /* 移动端隐藏收起切换按钮 */
  .collapse-toggle {
    display: none;
  }

  .sidebar-toggle {
    display: flex;
  }

  .sidebar-body {
    max-height: 0;
    overflow: hidden;
    padding: 0 16px;
  }

  .sidebar-body.open {
    max-height: 800px;
    overflow: visible;
    padding: 12px 16px 16px;
  }
}
</style>