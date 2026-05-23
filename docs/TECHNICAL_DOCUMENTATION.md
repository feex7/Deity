# Deity's Blog — 全栈个人博客技术文档

> **版本**: 1.0.0  
> **最后更新**: 2026-05-24  
> **作者**: Deity

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈详解](#2-技术栈详解)
3. [网站实现原理](#3-网站实现原理)
4. [模块设计与实现](#4-模块设计与实现)
5. [系统架构分析](#5-系统架构分析)
6. [运维指南](#6-运维指南)
7. [测试策略与结果](#7-测试策略与结果)
8. [未来优化方向](#8-未来优化方向)

---

## 1. 项目概述

### 1.1 项目定位

Deity's Blog 是一个**全栈个人博客系统**，集文章写作发布、项目管理、时间轴聚合、随笔记录、音乐播放、收藏管理、访客统计、留言互动等八大功能模块于一体。项目采用前后端分离架构，前端侧重建交互体验与视觉设计，后端侧重数据持久化与业务逻辑。

### 1.2 核心功能矩阵

| 功能模块 | 用户可见 | 管理员专属 | 数据来源 |
|---------|---------|-----------|---------|
| 📖 博客系统 | 文章浏览、分类/标签/年份三维筛选、分页、详情阅读 | 撰写文章（Markdown）、编辑、删除 | H2 数据库 |
| 📊 关于页面 | 站点统计、技术栈展示、功能特性一览 | — | 后端 `/api/stats` + 静态数据 |
| 📈 访客统计 | — | 今日/本月独立访客数（圆形进度图+数字动画） | H2 数据库 COUNT(DISTINCT) |
| ⭐ 收藏夹 | 分类浏览、关键词搜索 | 添加/编辑/删除收藏 | H2 数据库 |
| 💬 留言板 | 提交留言（昵称选填） | 查看/删除所有留言 | H2 数据库 |
| 🎵 音乐播放器 | 播放列表浏览、播放/暂停、上一曲/下一曲、进度条拖拽 | 上传音频文件、删除歌曲 | 本地文件系统 + H2 元数据 |
| 🚀 项目展示 | 项目列表展示 | — | 后端 API |
| ✍️ 随笔 | 随笔列表展示 | — | 后端 API |
| 📋 计划 | 计划列表展示 | — | 后端 API |
| ⏳ 时间轴 | 自动聚合创作记录 | — | `TimelineService` 聚合各模块数据 |
| 🔐 管理员验证 | — | 密码验证（SHA-256 + 盐值 + 时序安全比对） | `application.properties` 中的密码哈希 |
| 🛡️ 权限控制 | — | sessionStorage `adminVerified` 标记 + 路由守卫 + 操作按钮条件渲染 | 浏览器会话存储 |

---

## 2. 技术栈详解

### 2.1 前端技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **框架** | Vue 3 | 3.5.34 | Composition API + SFC 组件化 |
| **构建工具** | Vite | 8.0.12 | 极速 HMR 开发 + Rollup 生产构建 |
| **路由** | Vue Router | 4.6.4 | SPA 路由、懒加载、导航守卫 |
| **Markdown** | marked | 18.0.4 | Markdown 文本 → HTML 实时渲染 |
| **代码高亮** | highlight.js | 11.11.1 | 文章/编辑器代码块语法高亮 |
| **图表** | ECharts | 6.1.0 | 访客统计可视化（可选扩展） |
| **样式** | 原生 CSS (Scoped) | — | 暗色主题 + CSS 变量 + 响应式 |
| **状态管理** | Vue reactive() | — | 轻量级共享响应式状态（无需 Pinia） |
| **持久化** | localStorage / sessionStorage | — | 自定义分类 / 草稿 / 验证状态 |

### 2.2 后端技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **框架** | Spring Boot | 3.4.5 | 自动配置 + 嵌入式 Tomcat |
| **语言** | Java | 25 | 最新 LTS 长期支持版本 |
| **ORM** | Spring Data JPA | — | 声明式数据访问 + 自动建表 |
| **数据库** | H2 | 2.x (runtime) | 嵌入式文件数据库（零配置部署） |
| **密码安全** | SHA-256 + 盐值 | — | `MessageDigest` + `SecureRandom` 盐值生成 + `MessageDigest.isEqual()` 时序安全比对 |
| **文件上传** | Spring Multipart | — | 音乐文件上传（50MB 上限） |
| **音频流** | `RandomAccessFile` + Range | — | HTTP 206 Partial Content 范围请求 |

### 2.3 开发/部署工具链

| 工具 | 用途 |
|------|------|
| **npm** | 前端依赖管理 |
| **Maven** | 后端依赖管理 + 构建 |
| **Vite Dev Server** | 前端开发服务器（端口 5173，代理 `/api` 到后端） |
| **Spring Boot DevTools** | 后端热重载（可选） |
| **H2 Console** | 数据库 Web 管理界面（`http://localhost:8080/h2-console`） |
| **Git** | 版本控制 |

### 2.4 第三方服务/API 集成

| 服务 | 用途 | 状态 |
|------|------|------|
| **GitHub** | 社交链接（导航到外部） | 已配置占位链接 |
| **抖音** | 社交链接（导航到外部） | 已配置占位链接 |
| **QQ** | 社交链接（导航到外部） | 已配置占位链接 |
| **Polyhaven** | 3D 模型/HDR 资源（Blender MCP 集成） | 可选扩展 |
| **Hyper3D / Hunyuan3D** | AI 3D 模型生成（Blender MCP 集成） | 可选扩展 |

---

## 3. 网站实现原理

### 3.1 整体工作流程与数据流转

```
┌──────────────────────────────────────────────────────────────────────┐
│                          浏览器 (Browser)                              │
│                                                                       │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────────┐  │
│  │ Vue 3 App │   │  Pinia   │   │ Vue      │   │ sessionStorage / │  │
│  │ (SPA)     │◄──│  Store   │◄──│ Router   │   │ localStorage     │  │
│  │           │   │          │   │          │   │                  │  │
│  └─────┬─────┘   └──────────┘   └──────────┘   └──────────────────┘  │
│        │                                                              │
│        │ fetch('/api/...')                                            │
│        │                                                              │
│  ┌─────▼─────┐                                                        │
│  │   Vite    │                                                        │
│  │  Proxy    │  (开发环境: localhost:5173 → localhost:8080)           │
│  └─────┬─────┘                                                        │
└────────┼──────────────────────────────────────────────────────────────┘
         │
         │ HTTP Request (JSON / FormData / Range)
         │
┌────────▼──────────────────────────────────────────────────────────────┐
│                     Spring Boot (localhost:8080)                       │
│                                                                       │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────┐                  │
│  │ Controller│──►│   Service    │──►│  Repository  │                  │
│  │  Layer   │   │    Layer     │   │   (JPA)      │                  │
│  └──────────┘   └──────────────┘   └──────┬───────┘                  │
│                                            │                           │
│                                   ┌────────▼───────┐                  │
│                                   │   H2 Database  │                  │
│                                   │ (file: blogdb)  │                  │
│                                   └────────────────┘                  │
│                                                                       │
│  ┌───────────────┐   ┌───────────────────┐                           │
│  │  AdminConfig  │   │ uploads/music/     │                           │
│  │  (SHA-256)    │   │  (文件系统)         │                           │
│  └───────────────┘   └───────────────────┘                           │
└──────────────────────────────────────────────────────────────────────┘
```

**数据流转关键路径**:

1. **用户请求文章列表**: 浏览器 → Vue Router → BlogPage 组件 → `initArticles()` → `fetch('/api/articles')` → Vite 代理 → Spring Boot `ArticleController.list()` → `ArticleRepository.findAll()` → H2 查询 → JSON 响应 → 响应式更新 `mockPosts` → Vue 模板重渲染

2. **管理员发布文章**: 浏览器 → WritePage → 表单校验 → `addPost(article)` → `articlesAPI.create()` → `POST /api/articles` → `ArticleController.create()` → 参数校验 → `articleRepository.save()` → H2 INSERT → 返回新文章 JSON → 前端跳转 `/posts`

3. **音乐流播放**: HTML5 Audio → `GET /api/music/{id}/stream` → `Range: bytes=0-` → `MusicController.stream()` → `RandomAccessFile` 读取文件片段 → `206 Partial Content` + `Content-Range` 头 → 浏览器音频解码播放

4. **访客追踪**: 页面加载 → `localStorage.getItem('visitorId')` → 不存在则 `crypto.randomUUID()` 生成 → `POST /api/visitors/track` → 当日去重检查 → `INSERT` 或跳过 → 返回 `{ok: true}`

### 3.2 关键技术实现细节

#### 3.2.1 状态管理方案

项目采用 **Vue 3 `reactive()` 共享响应式对象** 替代 Pinia，避免引入额外依赖：

```js
// store/categories.js — 共享分类状态
import { reactive } from 'vue'

export const categoryStore = reactive({
  customCategories: loadCustomCategories(),  // 从 localStorage 恢复
  defaultCategories: [],

  addCustom(name) {
    if (!this.all.includes(name)) {
      this.customCategories.push(name)
      saveCustomCategories(this.customCategories)  // 持久化到 localStorage
    }
  },
  removeCustom(name) {
    this.customCategories = this.customCategories.filter(c => c !== name)
    saveCustomCategories(this.customCategories)
  }
})

// store/articles.js — 共享文章数据
export const mockPosts = reactive([])          // 响应式文章列表

export async function initArticles() {         // 全局初始化函数
  const data = await articlesAPI.list()
  mockPosts.splice(0, mockPosts.length, ...data)  // 原地替换保持响应性
}
```

**设计考量**: 对于 3~5 个共享状态的小型项目，`reactive()` 比 Pinia 更轻量，减少了工具链复杂度。跨组件使用时直接 `import { categoryStore } from '../store/categories.js'` 即可访问同一实例（ES Module 单例特性）。

#### 3.2.2 路由系统与权限控制

```js
// router/index.js
const router = createRouter({
  history: createWebHistory(),    // HTML5 History 模式（URL 无 # 号）
  linkActiveClass: 'active',       // 自动激活 CSS 类
  routes: [
    { path: '/',           component: SplashScreen },
    { path: '/posts',       component: BlogPage },
    { path: '/posts/:id',   component: PostDetail },
    { path: '/write',       component: WritePage },        // 需管理权限
    { path: '/favorites',   component: FavoritesPage },
    // ... 其他路由
  ]
})

// 全局前置守卫：拦截未授权访问 /write
router.beforeEach((to, from, next) => {
  if (to.path === '/write') {
    const verified = sessionStorage.getItem('adminVerified') === 'true'
    if (!verified) {
      alert('请先点击网站首页头像进行管理员验证')
      return next({ path: '/' })           // 重定向到首页
    }
  }
  next()
})
```

**权限控制三层防护**:
1. **路由守卫** (`beforeEach`): 拦截 `/write` 路由，未验证 → 重定向
2. **条件渲染** (`v-if="isAdmin()"`): 管理员专属 UI（编辑/删除按钮）仅对已验证用户可见
3. **后端验证**: 密码校验在 Spring Boot 侧完成，前端密码绝不暴露

#### 3.2.3 管理员验证密码安全

```
┌──────────────┐                      ┌──────────────────┐
│  Frontend    │   POST /api/verify   │  Backend         │
│              │  {username,password} │                  │
│  只接收      │ ──────────────────► │  SHA-256(salt +  │
│  success:    │                      │  password)       │
│  true/false  │ ◄────────────────── │  ↓               │
│              │  {success, message}  │  MessageDigest   │
└──────────────┘                      │  .isEqual()      │
                                      │  (时序安全比对)   │
                                      └──────────────────┘
```

**关键安全措施**:
- 密码原文**从未**出现在前端 JavaScript Bundle 中
- H2 数据库不存储明文密码，仅存储 `SHA-256(salt + password)` 哈希
- 使用 `MessageDigest.isEqual()` 做常数时间字符串比对，防止时序攻击
- 最多 5 次尝试，超出后冷却 5 分钟
- sessionStorage 仅存储 `"true"` 标记，关闭标签页即失效

#### 3.2.4 音频流 Range 请求实现

HTML5 `<audio>` 元素在播放长音频时会发送 Range 请求以支持 Seek 操作。如果服务器只返回 `200 OK`（完整文件），浏览器将无法拖拽进度条。

```java
// MusicController.java — 正确处理 Range 请求
@GetMapping("/{id}/stream")
public ResponseEntity<Resource> stream(@PathVariable Long id, HttpServletRequest request) {
    Path filePath = service.getFile(id);
    long fileSize = Files.size(filePath);
    String rangeHeader = request.getHeader("Range");

    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
        // 解析 Range: bytes=0-1024
        long start = Long.parseLong(rangeHeader.substring(6).split("-")[0]);
        long end = Math.min(start + 1MB, fileSize - 1);

        // 使用 RandomAccessFile 读取指定范围
        byte[] data = new byte[(int)(end - start + 1)];
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(start);
            raf.readFully(data);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Ranges", "bytes");
        headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        headers.setContentLength(data.length);
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));

        return new ResponseEntity<>(new ByteArrayResource(data), headers,
                                    HttpStatus.PARTIAL_CONTENT);  // 206
    }
    // 无 Range 头时返回完整文件（兼容不支持 Range 的旧客户端）
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .header("Accept-Ranges", "bytes")
            .body(new UrlResource(filePath.toUri()));
}
```

#### 3.2.5 访客追踪与去重

```
页面加载
  │
  ▼
localStorage.getItem('visitorId')
  │
  ├─ 已存在 ──► 使用已有 visitorId
  │
  └─ 不存在 ──► crypto.randomUUID() 生成新 visitorId
                  │
                  └─► localStorage.setItem('visitorId', visitorId)
                       │
                       ▼
                  POST /api/visitors/track  { visitorId }
                       │
                       ▼
                  VisitorRepository.existsByVisitorIdAndVisitDate(visitorId, today)
                       │
                       ├─ true  ──► 今日已记录，跳过
                       └─ false ──► INSERT INTO visitors (visitor_id, visit_date)
```

**统计查询** (使用 H2 SQL):

```java
@Query("SELECT COUNT(DISTINCT v.visitorId) FROM Visitor v WHERE v.visitDate = :date")
long countDistinctByVisitDate(@Param("date") LocalDate date);

@Query("SELECT COUNT(DISTINCT v.visitorId) FROM Visitor v WHERE v.visitDate BETWEEN :start AND :end")
long countDistinctByVisitDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
```

#### 3.2.6 性能优化策略

| 策略 | 实现方式 | 效果 |
|------|---------|------|
| **路由级代码分割** | 全部路由使用 `() => import()` 动态导入 | 首屏仅加载当前页面 JS，其他页面按需加载 |
| **Vite 依赖预构建** | `optimizeDeps` 自动将 CommonJS 模块转为 ESM | 减少浏览器请求数量 |
| **Rollup Tree Shaking** | 生产构建自动消除未使用代码 | 减小最终 bundle 体积 |
| **computed 缓存** | 筛选/排序/分页全部使用 `computed()` | 依赖不变时跳过重新计算 |
| **scroll 事件优化** | `addEventListener('scroll', fn, { passive: true })` | 不阻塞主线程滚动 |
| **RequestAnimationFrame** | 数字滚动动画使用 `requestAnimationFrame` | 60fps 流畅动画 |
| **localStorage 分类缓存** | 自定义分类写入 `localStorage` | 避免每次重新创建 |

---

## 4. 模块设计与实现

### 4.1 前端完整目录结构

```
my-blog/
├── index.html                          # HTML 入口
├── package.json                        # 依赖声明
├── vite.config.js                      # Vite 配置（代理、端口）
├── .env                                # 环境变量（VITE_API_URL）
│
└── src/
    ├── main.js                         # Vue 应用入口
    ├── App.vue                         # 根组件（NavBar + router-view）
    ├── style.css                       # 全局样式（暗色主题、滚动条、选择器、弹窗通用样式）
    │
    ├── router/
    │   └── index.js                    # 路由配置 + beforeEach 权限守卫
    │
    ├── store/
    │   ├── articles.js                 # 文章共享数据（reactive + API 封装）
    │   └── categories.js               # 分类共享数据（reactive + localStorage 持久化）
    │
    ├── api/
    │   ├── articles.js                 # 文章 CRUD API（GET/POST/PUT/DELETE /api/articles）
    │   └── auth.js                     # 管理员验证 API（POST /api/verify）
    │
    ├── utils/
    │   ├── isAdmin.js                  # 管理员状态查询（sessionStorage）
    │   ├── marked.js                   # Markdown 渲染器（marked + highlight.js）
    │   └── storage.js                  # localStorage 文章缓存工具
    │
    ├── components/
    │   ├── NavBar.vue                  # 全局导航栏（桌面端横向 + 移动端侧滑菜单）
    │   ├── BlogSidebar.vue             # 博客侧边栏（分类/年份/标签三维筛选 + 分类管理弹窗）
    │   ├── MusicPlayer.vue             # 固定底部音乐播放器（播放列表 + 上传管理）
    │   └── TagInput.vue               # 标签输入组件（逗号/空格/回车分隔）
    │
    └── views/
        ├── SplashScreen.vue            # 首页（打字机动画 + 管理员验证 + 留言板）
        ├── BlogPage.vue                # 博客列表页（筛选 + 分页 + 管理操作）
        ├── PostDetail.vue              # 文章详情页（Markdown 渲染 + 上下篇导航）
        ├── WritePage.vue               # 写文章页（分栏 Markdown 编辑器 + 分类/标签管理）
        ├── AboutPage.vue               # 关于页（站点统计 + 访客统计 + 技术栈展示）
        ├── FavoritesPage.vue           # 收藏夹（分类 + 搜索 + CRUD）
        ├── ProjectsPage.vue            # 项目展示（占位）
        ├── EssaysPage.vue              # 随笔（占位）
        ├── PlansPage.vue               # 计划（占位）
        ├── TimelinePage.vue            # 时间轴（占位）
        ├── HotTopicsPage.vue           # 热门话题（占位）
        └── MarketPage.vue              # 市场数据（占位）
```

### 4.2 模块详细说明

#### 4.2.1 博客系统模块

**涉及文件**: `BlogPage.vue`, `PostDetail.vue`, `WritePage.vue`, `BlogSidebar.vue`, `TagInput.vue`, `store/articles.js`, `api/articles.js`, `utils/marked.js`

**核心功能**:
- **文章列表**: 三维筛选（分类/年份/标签，AND 关系）+ 分页（每页 6 篇）
- **文章详情**: Markdown 渲染 + 代码高亮 + 上下篇导航 + 管理操作
- **文章撰写**: 左右分栏 Markdown 编辑器 + 14 种语法快捷按钮 + Tab 缩进 + 草稿保存
- **文章管理**: 编辑（预填数据跳转 WritePage）+ 删除（二次确认弹窗）

**关键算法 — 三维筛选**:

```js
const filteredPosts = computed(() => {
  let posts = mockPosts

  // 1. 分类筛选（多选，OR 关系）
  if (selectedCategories.value.length > 0) {
    posts = posts.filter(post => selectedCategories.value.includes(post.category))
  }

  // 2. 年份筛选（单选）
  if (selectedYear.value) {
    posts = posts.filter(post => post.date.startsWith(selectedYear.value))
  }

  // 3. 标签筛选（多选，至少含一个匹配标签）
  if (selectedTags.value.length > 0) {
    posts = posts.filter(post =>
      (post.tags || []).some(tag => selectedTags.value.includes(tag))
    )
  }

  return posts
})
```

**关键算法 — Markdown 工具栏光标插入**:

```js
const insertMarkdown = (syntax) => {
  const el = textareaRef.value
  const start = el.selectionStart     // 光标起始位置
  const end = el.selectionEnd         // 光标结束位置（选中文本时 > start）
  const before = content.value.substring(0, start)
  const selected = content.value.substring(start, end)  // 选中的文本
  const after = content.value.substring(end)

  let insertion = ''
  switch (syntax) {
    case 'bold':  insertion = `**${selected || '加粗文字'}**`; break
    case 'link':  insertion = `[${selected || '链接文字'}](url)`; break
    // ... 14 种语法
  }

  content.value = before + insertion + after

  nextTick(() => {
    el.focus()
    el.setSelectionRange(newPos, newPos)  // 恢复光标位置
  })
}
```

#### 4.2.2 分类管理模块

**涉及文件**: `store/categories.js`, `BlogSidebar.vue`, `WritePage.vue`

**核心功能**:
- **分类存储**: `defaultCategories`（文章数据提取）+ `customCategories`（用户创建，localStorage 持久化）
- **分类筛选**: BlogSidebar 中仅显示 `visibleCategories`（用户可通过管理弹窗控制）
- **新建分类**: WritePage 和 BlogSidebar 均可创建，写入共享 store
- **删除分类**: 仅可删除自定义分类（`customCategories`），同时取消该分类的选中状态

**状态流**:

```
WritePage                          BlogSidebar
  │ addCustomCategory('AI')          │
  │ → categoryStore.addCustom('AI')  │
  └───────┬──────────────────────────┘
          │
          ▼
    categoryStore (reactive)
    customCategories: ['AI', '读书']
    ↓ saveCustomCategories()
    localStorage: '["AI","读书"]'
          │
          ▼
    BlogSidebar 自动响应
    visibleCategories: [...allCategories, 'AI', '读书']
```

#### 4.2.3 留言板模块

**涉及文件**: `SplashScreen.vue` (前端), `GuestbookController.java` (后端), `Guestbook.java` (实体)

**核心功能**:
- **提交留言**: 昵称选填（默认显示"匿名"），内容必填（≤500 字）
- **权限分层**: 管理员可见全部留言列表 + 删除按钮；普通用户提交后仅显示"提交成功"
- **时间格式化**: 前端 `formatTime()` 实现"刚刚/N 分钟前/N 小时前/完整日期"

**后端实体设计**:

```java
@Entity
public class Guestbook {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String nickname;      // 可选，默认"匿名"

    @Column(nullable = false, length = 500)
    private String content;       // 必填

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (nickname == null || nickname.isBlank()) {
            nickname = "匿名";    // 后端默认值
        }
        createdAt = LocalDateTime.now();
    }
}
```

#### 4.2.4 音乐播放器模块

**涉及文件**: `MusicPlayer.vue` (前端), `MusicController.java` + `MusicService.java` (后端)

**核心功能**:
- **播放控制**: 播放/暂停、上一曲/下一曲、进度条点击/拖拽
- **可视化**: 均衡器动画（3 根跳动竖条，播放时激活）
- **管理员功能**: 上传音频（`<input type="file" accept="audio/*">` + FormData）、删除歌曲
- **权限轮询**: 每 2 秒检查 `sessionStorage.adminVerified` 以同步管理员状态

**关键防护 — Audio 空指针检查**:

```js
const togglePlay = () => {
  if (!currentSong.value) return
  if (isPlaying.value) {
    if (audio.value) {                          // ★ 先检查 Audio 对象是否存在
      audio.value.pause()
      isPlaying.value = false
    }
  } else {
    if (!audio.value) {                         // ★ 未初始化则先初始化
      play(currentIndex.value)
      return
    }
    audio.value.play().then(() => { ... })
      .catch(() => { playError.value = true })  // ★ 播放失败提示
  }
}
```

#### 4.2.5 访客统计模块

**涉及文件**: `AboutPage.vue` (前端), `VisitorController.java` + `Visitor.java` + `VisitorRepository.java` (后端)

**核心功能**:
- **访客标识**: `localStorage` 存储 `visitorId`（`crypto.randomUUID()` 生成）
- **每日去重**: `existsByVisitorIdAndVisitDate()` 检查当日是否已记录
- **统计查询**: `COUNT(DISTINCT visitorId)` + 日期范围过滤
- **前端展示**: 圆形卡片展示今日/本月独立访客数，30 秒自动刷新

#### 4.2.6 管理员验证模块

**涉及文件**: `SplashScreen.vue` + `utils/isAdmin.js` (前端), `AdminController.java` + `AdminConfig.java` (后端)

**验证流程**:

```
[用户点击头像]
      │
      ▼
[弹出验证弹窗] ← showModal = true
      │
      ▼
[用户输入密码]
      │
      ▼
[点击验证 / 回车]
      │
      ▼
  POST /api/verify { username: "admin", password: "****" }
      │
      ▼
  [Spring Boot AdminConfig.verify(password)]
      │
      ├─── 成功 ──► sessionStorage.setItem('adminVerified', 'true')
      │              isVerified = true
      │              1秒后自动关闭弹窗
      │
      └─── 失败 ──► attempts++
                    剩余次数 = 5 - attempts
                    用完 → 冷却 5 分钟
```

**密码配置** (`application.properties`):

```properties
blog.admin.username=admin
blog.admin.password-hash=793bcee5edd5045b87f5c2dbf55b1bf1e310de44bf3248a6db9e96b1e58e8b13
blog.admin.salt=4a7d1ed8c3f54062b82e9aa13cde650f
```

密码哈希生成方式: `SHA-256(salt + password)` — 使用 `AdminConfig.generateSalt()` 生成随机盐值，然后 `AdminConfig.hash(password, salt)` 计算哈希。

### 4.3 后端完整 API 清单

| 方法 | 路径 | 控制器 | 功能 | 权限 |
|------|------|--------|------|------|
| `GET` | `/api/articles` | ArticleController | 获取全部文章 | 公开 |
| `GET` | `/api/articles/{id}` | ArticleController | 获取单篇文章 | 公开 |
| `POST` | `/api/articles` | ArticleController | 创建文章 | 公开（前端路由守卫控制） |
| `PUT` | `/api/articles/{id}` | ArticleController | 更新文章 | 公开（前端路由守卫控制） |
| `DELETE` | `/api/articles/{id}` | ArticleController | 删除文章 | 公开（前端条件渲染控制） |
| `GET` | `/api/guestbook` | GuestbookController | 获取全部留言（时间倒序） | 公开 |
| `POST` | `/api/guestbook` | GuestbookController | 提交留言 | 公开 |
| `DELETE` | `/api/guestbook/{id}` | GuestbookController | 删除留言 | 公开（前端条件渲染控制） |
| `POST` | `/api/verify` | AdminController | 管理员密码验证 | 公开 |
| `GET` | `/api/stats` | StatsController | 获取站点统计数据 | 公开 |
| `POST` | `/api/visitors/track` | VisitorController | 记录访客 | 公开 |
| `GET` | `/api/visitors/stats` | VisitorController | 获取访客统计 | 公开 |
| `GET` | `/api/favorites` | FavoriteController | 获取收藏列表（支持分类/关键词过滤） | 公开 |
| `POST` | `/api/favorites` | FavoriteController | 添加收藏 | 公开（前端条件渲染控制） |
| `PUT` | `/api/favorites/{id}` | FavoriteController | 更新收藏 | 公开（前端条件渲染控制） |
| `DELETE` | `/api/favorites/{id}` | FavoriteController | 删除收藏 | 公开（前端条件渲染控制） |
| `GET` | `/api/music` | MusicController | 获取音乐列表 | 公开 |
| `POST` | `/api/music` | MusicController | 上传音乐 | 公开（前端条件渲染控制） |
| `DELETE` | `/api/music/{id}` | MusicController | 删除音乐 | 公开（前端条件渲染控制） |
| `GET` | `/api/music/{id}/stream` | MusicController | 音乐流播放（Range 支持） | 公开 |

> **说明**: 写操作的后端端点本身是公开的，权限控制全部在前端实现（路由守卫 + 条件渲染 + 管理员验证弹窗）。这种设计简化了后端实现，适用于个人博客场景。如需增强安全性，可引入 JWT/Session 认证。

### 4.4 数据库表结构

**articles** — 文章表:

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 文章 ID |
| title | VARCHAR | NOT NULL | 标题 |
| summary | VARCHAR(500) | — | 摘要 |
| category | VARCHAR | — | 分类 |
| date | DATE | — | 发布日期 |
| author | VARCHAR | — | 作者 |
| content | TEXT | — | Markdown 正文 |
| tags | TEXT | — | JSON 字符串标签数组 |

**guestbook** — 留言表:

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 留言 ID |
| nickname | VARCHAR(20) | — | 昵称（默认"匿名"） |
| content | VARCHAR(500) | NOT NULL | 留言内容 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

**favorites** — 收藏表:

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 收藏 ID |
| title | VARCHAR | NOT NULL | 标题 |
| url | VARCHAR | NOT NULL | 链接 |
| description | VARCHAR(500) | — | 描述 |
| category | VARCHAR | NOT NULL | 分类（GITHUB/WEBSITE/ARTICLE） |

**visitors** — 访客表:

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录 ID |
| visitor_id | VARCHAR | NOT NULL | 访客唯一标识（UUID） |
| visit_date | DATE | NOT NULL | 访问日期 |

**music** — 音乐表:

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 歌曲 ID |
| title | VARCHAR | — | 歌曲标题 |
| file_name | VARCHAR | NOT NULL | 文件存储名（UUID.扩展名） |
| file_size | BIGINT | — | 文件大小（字节） |
| created_at | TIMESTAMP | NOT NULL | 上传时间 |

---

## 5. 系统架构分析

### 5.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端 (Browser)                                  │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        表现层 (Presentation Layer)                      │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐  │  │
│  │  │ NavBar │ │Splash  │ │ Blog   │ │ Write  │ │ Post   │ │Favorites│  │  │
│  │  │        │ │Screen  │ │ Page   │ │ Page   │ │ Detail │ │  Page  │  │  │
│  │  └────────┘ └────────┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘  │  │
│  │                  │          │          │          │          │         │  │
│  │                  │     ┌────▼────┐     │          │          │         │  │
│  │                  │     │BlogSide │     │          │          │         │  │
│  │                  │     │  bar    │     │          │          │         │  │
│  │                  │     └────────┘     │          │          │         │  │
│  ├──────────────────┼────────────────────┼──────────┼──────────┼─────────┤  │
│  │             状态管理层 (State Layer)    │          │          │         │  │
│  │  ┌────────────────────────┐  ┌──────────────────┐                     │  │
│  │  │  store/articles.js     │  │ store/categories  │                     │  │
│  │  │  (reactive articles)   │  │ (reactive cats)   │                     │  │
│  │  └───────────┬────────────┘  └────────┬─────────┘                     │  │
│  ├──────────────┼────────────────────────┼───────────────────────────────┤  │
│  │         数据访问层 (Data Access Layer)  │                               │  │
│  │  ┌──────────▼────────────────────────▼───────────┐                     │  │
│  │  │  api/articles.js   +   api/auth.js            │                     │  │
│  │  │  (fetch /api/...   +   fetch /api/verify)     │                     │  │
│  │  └──────────────────────┬────────────────────────┘                     │  │
│  │                         │                                              │  │
│  │  ┌──────────────────────▼────────────────────────┐                     │  │
│  │  │   utils/isAdmin.js  +  utils/marked.js        │                     │  │
│  │  │   utils/storage.js  (localStorage 工具)        │                     │  │
│  │  └──────────────────────────────────────────────┘                     │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        路由层 (Router Layer)                            │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │  vue-router (createWebHistory)                                  │  │  │
│  │  │  beforeEach → adminVerified guard for /write                     │  │  │
│  │  │  Lazy loading → () => import() for all routes                    │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                       构建层 (Vite + Rollup)                            │  │
│  │  Proxy: /api → http://localhost:8080  (开发环境)                       │  │
│  │  Tree-shaking + Code-splitting (生产构建)                              │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                          HTTP / HTTPS
                                    │
┌─────────────────────────────────────────────────────────────────────────────┐
│                             后端 (Spring Boot)                               │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      控制层 (Controller Layer)                          │  │
│  │  ArticleController │ GuestbookController │ MusicController            │  │
│  │  VisitorController │ FavoriteController  │ AdminController            │  │
│  │  StatsController   │ TimelineController  │ ...                        │  │
│  │                                                        12 Controllers  │  │
│  └──────────────────────────────┬────────────────────────────────────────┘  │
│                                  │                                            │
│  ┌──────────────────────────────▼────────────────────────────────────────┐  │
│  │                       服务层 (Service Layer)                            │  │
│  │  MusicService │ MarketDataService │ TimelineService │ HotTopicFetcher │  │
│  └──────────────────────────────┬────────────────────────────────────────┘  │
│                                  │                                            │
│  ┌──────────────────────────────▼────────────────────────────────────────┐  │
│  │                   数据访问层 (Repository Layer)                         │  │
│  │  ArticleRepository │ GuestbookRepository │ VisitorRepository           │  │
│  │  FavoriteRepository│ MusicRepository     │ ... (10 Repositories)       │  │
│  └──────────────────────────────┬────────────────────────────────────────┘  │
│                                  │                                            │
│  ┌──────────────────────────────▼────────────────────────────────────────┐  │
│  │                        数据层 (Data Layer)                              │  │
│  │  ┌─────────────────────┐    ┌──────────────────────┐                   │  │
│  │  │  H2 Database (文件)  │    │  uploads/music/ (FS) │                   │  │
│  │  │  ./data/blogdb.mv.db│    │  UUID命名音频文件     │                   │  │
│  │  └─────────────────────┘    └──────────────────────┘                   │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                       横切关注点 (Cross-cutting)                        │  │
│  │  WebConfig (CORS) │ GlobalExceptionHandler │ AdminConfig (密码安全)    │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 分层设计说明

#### 前端分层

| 层级 | 职责 | 实现 |
|------|------|------|
| **表现层** | 页面渲染、用户交互、动画效果 | `views/` + `components/` (Vue SFC) |
| **状态层** | 跨组件共享数据、缓存策略 | `store/` (Vue reactive) |
| **数据访问层** | API 调用封装、数据格式化、本地存储 | `api/` + `utils/` |
| **路由层** | URL 管理、页面导航、权限拦截 | `router/` (Vue Router) |
| **构建层** | 开发代理、代码分割、生产优化 | `vite.config.js` (Vite + Rollup) |

#### 后端分层（经典三层架构）

| 层级 | 职责 | 实现 |
|------|------|------|
| **控制层** | 接收 HTTP 请求、参数校验、响应封装 | `controller/` (12 个 `@RestController`) |
| **服务层** | 业务逻辑、事务管理、文件操作 | `service/` (4 个 `@Service`) |
| **数据访问层** | 数据库 CRUD、声明式查询 | `repository/` (10 个 Spring Data JPA Repository) |
| **实体层** | 数据库表映射 | `entity/` (10 个 `@Entity`) |
| **配置层** | CORS、异常处理、密码安全 | `config/` (3 个 `@Configuration`) |

### 5.3 关键架构决策

**决策 1: 前端权限控制而非后端 Session/JWT**

> **考量**: 个人博客的"管理员"概念本质上只有一位用户，引入 JWT 或 Spring Security 会显著增加复杂度（需要管理 Token 生命周期、刷新机制、存储）。
>
> **选择**: 前端 sessionStorage + 路由守卫 + 条件渲染。后端 `/api/verify` 仅校验密码后返回 `{success: true/false}`，不颁发 Token。
>
> **权衡**: 简单直接但安全性略低（浏览器开发者工具可修改 sessionStorage）。对于个人博客场景，这一风险可接受。

**决策 2: H2 文件数据库替代 MySQL/PostgreSQL**

> **考量**: 个人博客数据量小（< 1000 条记录），不需要分布式/高可用特性。
>
> **选择**: H2 嵌入式文件数据库，数据持久化到 `./data/blogdb.mv.db` 文件。
>
> **优势**: 零安装部署（`mvn spring-boot:run` 即可运行），数据文件可随项目目录一起备份。

**决策 3: Vue reactive() 替代 Pinia**

> **考量**: 项目仅 2 个共享状态（文章列表 + 分类列表），引入 Pinia 的额外开销（依赖体积、学习成本、项目模板）不划算。
>
> **选择**: 使用 Vue 3 原生的 `reactive()` 创建响应式共享对象，ES Module 自然单例。
>
> **权衡**: 缺少 Pinia 的 DevTools 调试支持、插件生态和模块化能力。未来若状态复杂度增长，可平滑迁移到 Pinia。

**决策 4: MusicPlayer 仅在首页渲染**

> **考量**: 音乐播放器为固定定位悬浮组件（`position: fixed; bottom: 24px; right: 24px`），若在 App.vue 中全局渲染，则每个页面都加载其依赖（Audio API、上传逻辑），增加不必要的 JS 体积。
>
> **选择**: 仅在 `SplashScreen.vue`（首页）中渲染 MusicPlayer。
>
> **权衡**: 切换页面时音乐播放中断。如需全局播放，可迁移到 `App.vue`。

### 5.4 扩展性与可维护性设计

- **模块化路由**: 每个页面独立文件 + 懒加载，新增页面仅需添加路由配置
- **共享 Store 模式**: `store/` 目录下的模块可被任意组件直接 import，无需 Context Provider
- **API 封装**: `api/` 层统一管理请求 URL 和参数格式化，切换后端 URL 仅需改一个环境变量
- **组件组合**: `BlogSidebar` 和 `TagInput` 设计为通用组件，可在不同页面复用
- **Docker 就绪**: 后端为 Spring Boot fat JAR，前端为静态文件，天然适合容器化
- **H2 → MySQL 迁移**: Spring Data JPA 抽象了数据库差异，仅需修改 `application.properties` 中的 `spring.datasource.*` 配置即可切换数据库

---

## 6. 运维指南

### 6.1 项目启动步骤

#### 前端启动

```bash
cd my-blog
npm install          # 安装依赖
npm run dev          # 启动 Vite 开发服务器（默认端口 5173）
```

#### 后端启动

```bash
cd backend
mvn spring-boot:run  # 启动 Spring Boot（默认端口 8080）
```

#### 聚合启动（推荐使用）

1. 先启动后端: `cd backend && mvn spring-boot:run`
2. 再启动前端: `cd my-blog && npm run dev`
3. 访问: `http://localhost:5173`

#### 生产构建

```bash
# 前端
cd my-blog && npm run build    # 输出到 dist/ 目录

# 后端
cd backend && mvn package -DskipTests    # 输出到 target/blog-backend-1.0.0.jar
```

### 6.2 端口配置

| 服务 | 默认端口 | 配置文件 |
|------|---------|---------|
| 前端开发服务器 | 5173 | `vite.config.js` → `server.port` |
| 后端 API 服务器 | 8080 | `application.properties` → `server.port` |
| H2 Web Console | 8080 | 访问 `http://localhost:8080/h2-console` |

### 6.3 环境变量

前端 `.env`:

```env
VITE_API_URL=/api       # API 基础路径（开发时通过 Vite 代理到 localhost:8080）
```

后端 `application.properties` 关键配置:

```properties
server.port=8080
spring.datasource.url=jdbc:h2:file:./data/blogdb;AUTO_SERVER=TRUE
spring.jpa.hibernate.ddl-auto=update          # 自动建表/更新表结构
spring.servlet.multipart.max-file-size=50MB   # 单文件上传大小限制
spring.servlet.multipart.max-request-size=100MB
blog.admin.username=admin                     # 管理员用户名
blog.admin.password-hash=...                  # SHA-256(salt+password) 哈希
blog.admin.salt=...                           # 随机盐值
```

### 6.4 常见问题排查

#### 问题 1: 前端 404 — "无法连接到服务器"

**症状**: BlogPage 显示"无法连接到服务器，请确认后端已启动"  
**排查**:
1. 确认后端已启动 — 访问 `http://localhost:8080/api/articles` 应返回 JSON
2. 确认 Vite 代理配置 — `vite.config.js` 中 `proxy['/api'].target = 'http://localhost:8080'`
3. 确认后端端口未被占用 — `netstat -ano | findstr 8080`
4. 确认 CORS 配置 — `WebConfig.java` 应允许所有来源的 `/api/**` 请求

#### 问题 2: 音乐播放失败 / 无法拖动进度条

**症状**: 上传歌曲后点击播放无反应或进度条无法拖动  
**排查**:
1. 检查浏览器 Network 面板，查看 `/api/music/{id}/stream` 请求
2. 应返回 `206 Partial Content`（而非 `200 OK`）— 说明 Range 请求正常
3. 确认 `MusicController.stream()` 方法正确处理了 `Range` 请求头
4. **常见原因**: `Audio` 对象在调用 `play()` 前未初始化 → 已在 [MusicPlayer.vue#L86-L106](file:///d:/blog01/my-blog/src/components/MusicPlayer.vue#L86-L106) 添加 null 检查

#### 问题 3: 非管理员可访问写文章页面

**症状**: 未经验证的用户能直接访问 `/write` 路由  
**排查**:
1. 确认 `router/index.js` 中 `beforeEach` 守卫存在且正确
2. 确认 `sessionStorage.getItem('adminVerified')` 的 key 拼写一致
3. 清除浏览器 `sessionStorage` 后重试

#### 问题 4: 自定义分类消失

**症状**: 新建的分类刷新页面后不见了  
**排查**:
1. 检查 `localStorage.getItem('blog_custom_categories')` 是否有数据
2. 确认 `categoryStore.addCustom()` 调用了 `saveCustomCategories()`
3. 检查浏览器是否禁用了 `localStorage`

#### 问题 5: H2 数据库数据丢失

**症状**: 重启后端后文章/留言/收藏数据消失  
**排查**:
1. 确认 `spring.jpa.hibernate.ddl-auto=update`（而非 `create` 或 `create-drop`）
2. 确认数据文件路径 `./data/blogdb.mv.db` 未被删除
3. 检查启动日志中是否有 "Table not found" 错误

---

## 7. 测试策略与结果

### 7.1 当前测试状态

项目目前处于 **开发阶段**，尚未引入自动化测试框架。以下是已验证的功能状态。

### 7.2 构建验证

| 检查项 | 命令 | 结果 |
|--------|------|------|
| 前端构建 | `npm run build` | ✅ Pass（exit 0），仅 marked 库 chunk 体积警告（正常） |
| 后端编译 | `mvn compile` | ✅ Pass（Spring Boot 3.4.5 + Java 25） |

### 7.3 手工验证清单

| 模块 | 测试场景 | 状态 |
|------|---------|------|
| **博客** | 文章列表加载、分类/年份/标签三维筛选、分页跳转、空状态提示 | ✅ |
| | 文章详情渲染（Markdown → HTML）、代码高亮、上下篇导航 | ✅ |
| | 写文章（Markdown 工具栏 14 种语法、光标插入、Tab 缩进） | ✅ |
| | 编辑文章（预填数据跳转）、删除文章（二次确认弹窗） | ✅ |
| **管理员验证** | 点击头像 → 弹窗 → 输入密码 → 后端校验 → 成功/失败提示 | ✅ |
| | 失败次数限制（5 次 → 冷却 5 分钟） | ✅ |
| | sessionStorage 持久化验证状态 | ✅ |
| | 路由守卫拦截未授权 `/write` 访问 | ✅ |
| **留言板** | 昵称选填提交留言、内容校验（必填 + 500 字上限） | ✅ |
| | 管理员查看/删除留言列表 | ✅ |
| | 非管理员提交后显示"提交成功"不显示列表 | ✅ |
| **收藏夹** | 分类筛选、关键词搜索、添加/编辑/删除收藏 | ✅ |
| **音乐播放器** | 上传音频、播放/暂停、上下曲、进度条拖拽 | ✅ |
| | Audio 对象 null 检查（防止空指针崩溃） | ✅ |
| | 管理员删除歌曲 | ✅ |
| **访客统计** | 自动生成 visitorId、每日去重、30 秒自动刷新 | ✅ |
| **分类管理** | 创建自定义分类、localStorage 持久化、删除自定义分类 | ✅ |
| **响应式** | 移动端布局适配（导航栏 → 侧滑菜单、侧边栏 → 折叠面板） | ✅ |

### 7.4 已知问题

| 编号 | 描述 | 优先级 | 状态 |
|------|------|--------|------|
| — | 暂无已知严重缺陷 | — | — |

---

## 8. 未来优化方向

### 8.1 功能扩展

| 方向 | 描述 | 优先级 |
|------|------|--------|
| **全文搜索** | 引入 Elasticsearch 或 H2 全文索引，支持文章内容关键词搜索 | 中 |
| **评论系统** | 在文章详情页添加评论功能（类似留言板） | 中 |
| **RSS 订阅** | 生成 RSS Feed 供读者订阅 | 低 |
| **图片上传** | 支持在编辑器中拖拽/粘贴上传图片 | 中 |
| **暗色/亮色主题切换** | 增加亮色主题选项（当前仅暗色主题） | 低 |
| **国际化 (i18n)** | 支持中英文切换 | 低 |
| **SEO 优化** | SSR（Nuxt/Vite SSR）或预渲染，提升搜索引擎可见性 | 中 |
| **微信/社交媒体分享** | 文章分享功能 | 低 |
| **文章导入/导出** | 支持 Markdown 文件批量导入导出 | 低 |
| **数据统计仪表盘** | 管理员专属的数据可视化面板（访问趋势、热门文章等） | 中 |

### 8.2 性能改进

| 方向 | 描述 | 优先级 |
|------|------|------|
| **图片懒加载** | 文章中的图片实现 `loading="lazy"` 或 Intersection Observer | 中 |
| **虚拟列表** | 文章列表超过 100 条时引入虚拟滚动 | 低 |
| **Service Worker** | PWA 离线缓存，提升二次访问速度 | 低 |
| **CDN 部署** | 静态资源 CDN 分发（Vercel/Netlify/GitHub Pages） | 中 |
| **Gzip/Brotli 压缩** | 服务器端开启压缩 | 中 |

### 8.3 技术债务处理

| 方向 | 描述 | 优先级 |
|------|------|------|
| **引入 TypeScript** | 为前端代码添加类型安全，减少运行时错误 | 高 |
| **后端认证标准化** | 引入 Spring Security + JWT，替代当前的前端 permission 方案 | 中 |
| **API 文档** | 使用 Swagger/OpenAPI 自动生成 API 文档 | 中 |
| **数据库迁移工具** | 引入 Flyway/Liquibase 管理数据库版本 | 中 |
| **前后端统一错误处理** | 定义标准的 `ApiResponse<T>` 响应格式 | 高 |
| **日志系统完善** | 后端添加结构化日志（ELK/SLF4J + JSON 格式） | 中 |
| **代码规范** | 前端引入 ESLint + Prettier，后端引入 Checkstyle | 高 |
| **自动化测试** | 前端: Vitest + Vue Test Utils；后端: JUnit + MockMvc | 高 |
| **CI/CD** | GitHub Actions 自动化构建、测试、部署 | 中 |

### 8.4 架构演进建议

```
当前架构                          第一阶段演变                      第二阶段演变
┌─────────┐                     ┌─────────┐                      ┌─────────┐
│ Vue 3   │                     │ Vue 3   │                      │ Nuxt 3  │
│ SPA     │                     │ + TS    │                      │ (SSR)   │
└────┬────┘                     │ + ESLint│                      └────┬────┘
     │                          └────┬────┘                           │
┌────▼────┐                     ┌────▼────┐                      ┌────▼────┐
│ Express │  ← (N/A, Vite)      │ Nginx   │                      │ Nginx   │
└────┬────┘                     │ (reverse│                      │ (CDN)   │
     │                          │  proxy) │                      └────┬────┘
┌────▼────┐                     └────┬────┘                           │
│ Spring  │                     ┌────▼────┐                      ┌────▼────┐
│ Boot    │                     │ Spring  │                      │ Spring  │
│ + H2    │                     │ Boot    │                      │ Boot    │
│         │                     │ + MySQL │                      │ + MySQL │
│         │                     │ + Redis │                      │ + Redis │
└─────────┘                     └─────────┘                      │ + ES    │
                                                                  └─────────┘
```

**第一步（短期）**: 引入 TypeScript + ESLint + 统一错误处理格式，解决技术债务中最紧迫的问题  
**第二步（中期）**: H2 → MySQL 迁移、引入 Redis 缓存、添加 JWT 认证  
**第三步（长期）**: Vue SPA → Nuxt SSR（SEO 优化）、引入 Elasticsearch 全文搜索

---

> **文档版本**: v1.0.0 | **生成时间**: 2026-05-24 | **覆盖范围**: 前端 19 个源文件 + 后端 41 个源文件