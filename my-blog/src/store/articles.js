/*
 * ============================================================
 * 模块名称: articles.js（文章共享数据）
 * 功能描述: 跨组件共享的文章数据存储
 *           BlogPage（列表页）和 PostDetail（详情页）共用同一份数据
 *
 * 数据结构:
 *   id       - 文章唯一标识
 *   title    - 文章标题
 *   summary  - 文章摘要（列表页展示）
 *   category - 文章分类（必选，从分类列表中选择）
 *   date     - 发布日期（YYYY-MM-DD）
 *   author   - 作者
 *   content  - 文章正文（Markdown 格式，详情页渲染）
 *   tags     - 文章标签（撰写时手动添加，当前为空）
 *
 * 使用方式:
 *   import { mockPosts } from '../store/articles.js'
 *   // 按 id 查找: mockPosts.find(p => p.id === id)
 *   // 列表展示: mockPosts.map(...)
 * ============================================================
 */

import { reactive, ref } from 'vue'
import { articlesAPI } from '../api/articles.js'

const defaultArticles = [
  {
    id: 1,
    title: 'Vue3 Composition API 实战指南',
    summary: '深入探索 Vue3 的 Composition API，通过实际项目案例讲解 ref、reactive、computed、watch 等核心 API 的使用场景和最佳实践。',
    category: '前端',
    date: '2026-05-20',
    author: 'Admin',
    tags: [],
    content: `## 为什么需要 Composition API？

在 Vue2 中，我们使用 Options API（data、methods、computed、watch）组织组件逻辑。这种方式在小项目中足够清晰，但当组件变得复杂时，同一功能的逻辑被迫分散在不同选项中，阅读和维护成本急剧上升。

Composition API 的核心思想是**按逻辑关注点组织代码**，而非按选项类型。

## ref 与 reactive

ref 用于包装基本类型值，通过 .value 访问和修改：

\`\`\`js
import { ref } from 'vue'

const count = ref(0)
const increment = () => {
  count.value++
}
\`\`\`

reactive 用于包装对象，直接访问属性无需 .value：

\`\`\`js
import { reactive } from 'vue'

const state = reactive({
  user: { name: 'Alice' },
  items: []
})
\`\`\`

## computed 与 watch

computed 自动追踪依赖并缓存结果，非常适合派生状态：

\`\`\`js
const doubled = computed(() => count.value * 2)
\`\`\`

watch 用于在响应式数据变化时执行副作用：

\`\`\`js
watch(count, (newVal, oldVal) => {
  console.log(\`count 从 \${oldVal} 变为 \${newVal}\`)
})
\`\`\`

## 实战建议

1. **优先使用 ref**：ref 明确标识了响应式数据，类型推导更好
2. **computed 代替模板表达式**：保持模板简洁
3. **watchEffect 简化初次执行**：需要立即执行的 watch 用 watchEffect
4. **defineExpose 暴露方法**：父组件通过 ref 调用子组件方法时使用

Composition API 不只是一个新写法，它代表了 Vue 向更灵活、更函数式的方向演进。掌握它是现代 Vue 开发者的必修课。`
  },
  {
    id: 2,
    title: 'Spring Boot 微服务架构设计',
    summary: '从单体应用到微服务架构的演进之路，详解 Spring Boot 中的服务拆分、注册发现、配置中心等关键技术选型。',
    category: '后端',
    date: '2026-05-15',
    author: 'Admin',
    tags: [],
    content: `## 单体架构的困境

当项目初期功能简单时，单体架构是最佳选择——开发快、部署简单。但随着业务增长，单体的缺点逐渐暴露：代码耦合严重、部署周期长、扩展困难、技术栈锁定。

## 微服务拆分原则

拆分微服务没有银弹，但有几条原则值得遵循：

> 按业务边界拆分，而非按技术层拆分。

一个订单服务应该包含完整的订单业务逻辑，而不是把 Controller、Service、Repository 分别拆成三个服务。

## 服务注册与发现

在微服务架构中，服务实例会动态上下线。Spring Cloud Netflix Eureka 是最经典的实现：

\`\`\`yaml
# application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
\`\`\`

## 配置中心

统一管理配置是微服务的基本需求。Nacos 和 Spring Cloud Config 是主流选择：

| 特性 | Nacos | Config Server |
|------|-------|--------------|
| 实时推送 | ✅ | 需配合 Bus |
| 管理界面 | ✅ | 无内置 |
| 服务发现 | ✅ | 需单独部署 |

## 最后的话

微服务不是银弹。如果你的团队只有 3 个人、业务逻辑还很简单，单体 + 良好的模块划分可能更适合你。**架构选择的本质是权衡，而非追求潮流。**`
  },
  {
    id: 3,
    title: 'CSS Grid 与 Flexbox 布局对比',
    summary: '一文搞懂 CSS 两种现代布局方案的区别与适用场景，包含大量实际布局案例和响应式设计技巧。',
    category: '前端',
    date: '2026-05-10',
    author: 'Admin',
    tags: [],
    content: `## 两种布局，两种思路

**Flexbox**：一维布局。在主轴（横或竖）上排列元素，适合组件内部的对齐。
**Grid**：二维布局。同时控制行和列，适合页面级的大框架。

## Flexbox 最佳场景

- 导航栏水平排列
- 卡片内元素垂直居中
- 表单标签与输入框对齐

\`\`\`css
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
\`\`\`

## Grid 最佳场景

- 整体页面布局（header / sidebar / main / footer）
- 照片墙或卡片网格
- 复杂的仪表盘布局

\`\`\`css
.page-layout {
  display: grid;
  grid-template-columns: 250px 1fr;
  grid-template-rows: auto 1fr auto;
  grid-template-areas:
    "header  header"
    "sidebar main"
    "footer  footer";
  min-height: 100vh;
}
\`\`\`

## 组合使用

实际项目中，两者经常配合使用：

> 外层 Grid 搭框架，内层 Flex 调细节。

\`\`\`css
/* Grid 定义整体结构 */
.dashboard { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }

/* Flex 处理卡片内部 */
.card { display: flex; flex-direction: column; justify-content: space-between; }
\`\`\`

关键在于理解它们各自的一维/二维属性，根据实际需求灵活选择。`
  },
  {
    id: 4,
    title: 'Git 工作流最佳实践',
    summary: '团队协作中如何规范使用 Git？本文介绍 Git Flow、GitHub Flow 和 Trunk Based Development 的优劣对比。',
    category: '工具',
    date: '2026-05-05',
    author: 'Admin',
    tags: [],
    content: `## 为什么需要 Git 工作流？

多人协作时，如果没有统一的 Git 使用规范，很快就会出现混乱的提交历史、冲突频发、发布流程失控等问题。一个好的工作流能解决三个核心问题：**代码如何合并、何时发布、怎样追溯**。

## Git Flow

最经典的模型，由 Vincent Driessen 在 2010 年提出：

- \`main\` —— 生产分支，每次提交都是一个发布版本
- \`develop\` —— 开发主线
- \`feature/*\` —— 功能分支，从 develop 拉出，完成后合并回 develop
- \`release/*\` —— 发布分支，从 develop 拉出，完成后合并到 main 和 develop
- \`hotfix/*\` —— 紧急修复分支，从 main 拉出

**适用场景**：有固定发布周期的项目，如企业软件、硬件固件。

**缺点**：分支类型太多，对小团队来说是过度设计。

## GitHub Flow

更简洁的模型，被 GitHub 和众多开源项目采用：

- \`main\` —— 始终可部署
- \`feature/*\` —— 功能分支，通过 Pull Request 合并

**适用场景**：持续部署的 Web 应用、开源项目。

## Trunk Based Development

极致简洁：所有人直接向主干（main/trunk）提交小改动。

> 每次提交都应该可以通过所有测试，随时可以部署。

配合 Feature Flag 来控制未完成功能的可见性。

## 我的建议

- **小团队（<10人）** → GitHub Flow
- **大团队/有固定发布** → Git Flow
- **CI/CD 成熟** → Trunk Based

最重要的是：**选择了就坚持，比反复切换更重要。**`
  },
  {
    id: 5,
    title: 'TypeScript 类型体操入门',
    summary: '从基础的泛型使用到高级类型工具，逐步拆解 TypeScript 类型系统的精髓，让你写出更安全的代码。',
    category: '前端',
    date: '2026-04-28',
    author: 'Admin',
    tags: [],
    content: `## 类型即文档

JavaScript 最大的痛点是运行时才知道类型错误。TypeScript 的类型系统让代码自己说话：

\`\`\`typescript
interface User {
  id: number
  name: string
  email: string
  role: 'admin' | 'editor' | 'viewer'
}

function getUser(id: number): Promise<User> { /* ... */ }
\`\`\`

## 泛型：类型的函数

泛型让类型参数化，大幅提升复用性：

\`\`\`typescript
function first<T>(arr: T[]): T | undefined {
  return arr[0]
}

const a = first([1, 2, 3])       // type: number | undefined
const b = first(['a', 'b'])       // type: string | undefined
\`\`\`

## 实用工具类型

TypeScript 内置了许多强大的工具类型：

\`\`\`typescript
interface Config {
  host: string
  port: number
  debug: boolean
}

// Partial: 所有属性变可选
type PartialConfig = Partial<Config>

// Pick: 选取指定属性
type ConnectionInfo = Pick<Config, 'host' | 'port'>

// Omit: 排除指定属性
type WithoutDebug = Omit<Config, 'debug'>

// Record: 构造对象类型
type PageRoutes = Record<'home' | 'about' | 'blog', string>
\`\`\`

## 进阶：条件类型与 infer

\`\`\`typescript
type UnwrapPromise<T> = T extends Promise<infer U> ? U : T
type Result = UnwrapPromise<Promise<string>> // string
\`\`\`

类型体操不是炫技，而是精确描述你的数据结构和 API 契约。好的类型设计能让你在编码时就避免大量运行时错误。`
  },
  {
    id: 6,
    title: '数据库索引优化策略',
    summary: '索引不是越多越好！本文分析 B+Tree 索引原理，通过 EXPLAIN 分析慢查询，分享实际优化案例。',
    category: '后端',
    date: '2026-04-20',
    author: 'Admin',
    tags: [],
    content: `## B+Tree 索引原理

MySQL InnoDB 引擎使用 B+Tree 作为索引结构。理解它的工作机制是优化的前提：

- **非叶子节点**只存键值和子节点指针
- **叶子节点**存完整数据（聚簇索引）或主键值（二级索引）
- 叶子节点通过双向链表连接，支持范围查询

## EXPLAIN 分析

遇到慢查询的第一步永远是 EXPLAIN：

\`\`\`sql
EXPLAIN SELECT * FROM orders WHERE user_id = 100 AND status = 'paid';
\`\`\`

重点关注这些字段：

| 字段 | 含义 | 理想值 |
|------|------|--------|
| type | 访问类型 | const > ref > range > index > ALL |
| key | 使用的索引 | 非 NULL |
| rows | 扫描行数 | 越小越好 |
| Extra | 额外信息 | 避免 Using filesort |

## 常见优化策略

### 1. 覆盖索引

查询列全部被索引覆盖时，无需回表：

\`\`\`sql
CREATE INDEX idx_user_status ON orders(user_id, status);
SELECT user_id, status FROM orders WHERE user_id = 100;
\`\`\`

### 2. 最左前缀原则

复合索引 \`(a, b, c)\` 只有在查询条件包含 a 时才会被使用。

### 3. 避免索引失效

- WHERE 子句中对索引列使用函数 → 失效
- 使用 OR 连接不同列 → 可能失效
- LIKE '%keyword' 前导模糊 → 失效

> 索引优化的本质是减少磁盘 I/O。每次优化都应该以实际的 EXPLAIN 结果为依据，而非凭感觉。`
  },
  {
    id: 7,
    title: 'Vite 构建工具深度解析',
    summary: '为什么 Vite 比 Webpack 快？从 ES Module、esbuild、Rollup 三个核心层面拆解 Vite 的构建原理。',
    category: '前端',
    date: '2026-04-15',
    author: 'Admin',
    tags: [],
    content: `## Vite 为什么这么快？

Vite 的名字来源于法语"快"，它的快源于三个核心决策：

### 1. 开发时原生 ES Module

Webpack 需要将所有模块打包成一个 bundle，而 Vite 利用浏览器原生支持的 ES Module：

\`\`\`html
<script type="module" src="/src/main.js"></script>
\`\`\`

浏览器按需请求每个模块，Vite 只需转换当前被请求的文件。这意味着无论项目多大，**冷启动时间几乎恒定**。

### 2. esbuild 预构建

依赖预构建使用 Go 写的 esbuild，比 JavaScript 写的打包器快 10-100 倍：

\`\`\`js
// vite.config.js
export default {
  optimizeDeps: {
    include: ['lodash', 'axios']
  }
}
\`\`\`

### 3. Rollup 生产构建

生产环境使用 Rollup 打包，充分利用它的 Tree Shaking 和代码分割能力：

\`\`\`js
// 自动代码分割
// 每个路由的组件会被打包为独立的 chunk
const routes = [
  { path: '/posts', component: () => import('./views/BlogPage.vue') }
]
\`\`\`

## 与 Webpack 对比

| 特性 | Vite | Webpack |
|------|------|---------|
| 冷启动 | <1s | 10-30s |
| HMR | 即时 | 随规模变慢 |
| 配置复杂度 | 低 | 高 |
| 生态成熟度 | 快速增长 | 最成熟 |

## 迁移建议

如果你的项目使用 Webpack 且开发体验尚可，不必急于迁移。但新项目强烈推荐 Vite——它不是"又一个构建工具"，而是代表了前端工具链的下一代范式。`
  },
  {
    id: 8,
    title: 'Redis 缓存实战技巧',
    summary: '缓存穿透、缓存击穿、缓存雪崩——三大经典问题的成因与解决方案，附生产环境配置建议。',
    category: '后端',
    date: '2026-04-08',
    author: 'Admin',
    tags: [],
    content: `## 缓存的三大陷阱

### 缓存穿透

**现象**：查询一个数据库中不存在的 key，每次请求都绕过缓存直接打到数据库。
**解决**：布隆过滤器或缓存空值（设置短过期时间）。

\`\`\`java
public String getData(String key) {
    String value = redis.get(key);
    if (value != null) return value;
    
    value = db.query(key);
    if (value == null) {
        redis.setex(key, 60, ""); // 缓存空值 60 秒
        return null;
    }
    redis.set(key, value);
    return value;
}
\`\`\`

### 缓存击穿

**现象**：热点 key 在过期的瞬间，大量请求同时查询数据库。
**解决**：互斥锁或"永不过期"（后台异步刷新）。

\`\`\`java
// 互斥锁方案
String value = redis.get(key);
if (value == null) {
    if (redis.setnx(lockKey, "1")) {
        value = db.query(key);
        redis.set(key, value);
        redis.del(lockKey);
    } else {
        Thread.sleep(50);
        return getData(key);
    }
}
\`\`\`

### 缓存雪崩

**现象**：大量 key 同时过期，导致数据库压力骤增。
**解决**：过期时间加随机值、多级缓存、限流降级。

## 生产环境配置清单

- **最大内存**：\`maxmemory 2gb\`
- **淘汰策略**：\`maxmemory-policy allkeys-lru\`
- **持久化**：RDB + AOF 混合模式
- **连接池**：合理设置 maxTotal（推荐连接数的 2 倍）

> 缓存是门实践艺术，理论只能指引方向，真正靠谱的方案来自压测和监控。`
  },
  {
    id: 9,
    title: '前端性能优化全攻略',
    summary: '从网络层、渲染层、构建层三个维度，全面梳理前端性能优化的实用技巧和度量指标。',
    category: '前端',
    date: '2026-03-30',
    author: 'Admin',
    tags: [],
    content: `## 性能优化的金字塔

前端性能优化可以分为三个层次：

### 网络层

**关键指标**：FCP（首次内容绘制）、LCP（最大内容绘制）

优化手段：
- **代码分割**：路由级 + 组件级懒加载
- **资源压缩**：Gzip/Brotli，图片转 WebP
- **CDN 加速**：静态资源就近分发
- **HTTP/2**：多路复用减少连接开销

### 渲染层

**关键指标**：FID（首次输入延迟）、CLS（累积布局偏移）

优化手段：
- 避免长任务（超过 50ms），拆分为多个小任务
- 使用 \`will-change\` 和 \`transform\` 启动 GPU 加速
- 图片设置 width/height 防止布局抖动
- 虚拟列表处理长列表（如 react-window）

\`\`\`css
/* GPU 加速的动画 */
.slide-in {
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  will-change: transform;
}
\`\`\`

### 构建层

优化手段：
- Tree Shaking：消除死代码
- 合理设置分包策略（vendor、common、runtime）

\`\`\`js
// Vite 分包配置
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        vendor: ['vue', 'vue-router'],
        ui: ['marked']
      }
    }
  }
}
\`\`\`

## 性能预算

设定明确的性能预算并持续监控：LCP < 2.5s、FID < 100ms、CLS < 0.1。

> 优化永无止境，但没有度量的优化是盲目的。`
  },
  {
    id: 10,
    title: 'Docker 容器化部署入门',
    summary: '从 Dockerfile 编写到 docker-compose 编排，手把手教你将 Spring Boot 应用容器化部署。',
    category: '运维',
    date: '2026-03-22',
    author: 'Admin',
    tags: [],
    content: `## 为什么需要 Docker？

"在我机器上能跑"是开发与运维之间最经典的矛盾。Docker 通过容器化技术解决了环境一致性问题，让应用及其依赖一并打包、随处运行。

## 编写 Dockerfile

一个典型的 Spring Boot 应用 Dockerfile：

\`\`\`dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
\`\`\`

### 优化技巧

- **多阶段构建**：编译和运行分离，减小镜像体积
- **使用 alpine 基础镜像**：比 slim 更小
- **.dockerignore**：排除 node_modules、.git 等无关文件

\`\`\`dockerfile
# 构建阶段
FROM maven:3.8-openjdk-17 AS build
WORKDIR /src
COPY . .
RUN mvn package -DskipTests

# 运行阶段
FROM openjdk:17-alpine
COPY --from=build /src/target/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
\`\`\`

## Docker Compose 编排

多服务应用使用 docker-compose：

\`\`\`yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
  redis:
    image: redis:7-alpine
\`\`\`

## 生产环境注意事项

1. **不要以 root 运行**：Dockerfile 中使用 USER 指令
2. **资源限制**：设置 CPU 和内存上限
3. **健康检查**：HEALTHCHECK 指令监控服务状态

> 容器化不是银弹，但它让部署变得可预测、可复现、可规模化。`
  },
  {
    id: 11,
    title: 'RESTful API 设计规范',
    summary: '如何设计出优雅的 REST API？从 URL 命名、状态码使用、版本管理到错误处理，统一规范。',
    category: '后端',
    date: '2026-03-15',
    author: 'Admin',
    tags: [],
    content: `## 资源导向的 URL 设计

REST 的核心是将一切视为资源。URL 应该描述资源，而不是动作：

\`\`\`
✅ GET    /api/users/123      查询用户
✅ POST   /api/users          创建用户
✅ PUT    /api/users/123      更新用户
✅ DELETE /api/users/123      删除用户

❌ GET    /api/getUser?id=123
❌ POST   /api/createUser
\`\`\`

## HTTP 状态码使用规范

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 204 | No Content | 删除成功 |
| 400 | Bad Request | 参数校验失败 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务端异常 |

## 统一响应格式

\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "name": "Alice",
    "email": "alice@example.com"
  }
}
\`\`\`

错误响应也使用相同结构：

\`\`\`json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
\`\`\`

## 分页与过滤

\`\`\`
GET /api/posts?page=1&size=10&sort=date,desc&category=前端
\`\`\`

响应中包含分页元数据：

\`\`\`json
{
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "size": 10,
    "total": 150,
    "totalPages": 15
  }
}
\`\`\`

> 好的 API 设计让调用者感觉自然，不需要频繁查阅文档。一致性是最好的文档。`
  },
  {
    id: 12,
    title: '2026 前端技术趋势展望',
    summary: '回顾过去一年的前端技术发展，展望 SSR、边缘计算、WebAssembly 等前沿技术的未来方向。',
    category: '前端',
    date: '2026-03-01',
    author: 'Admin',
    tags: [],
    content: `## SSR 的回归与进化

服务端渲染并非新概念，但近年以 Next.js、Nuxt 为代表的框架让 SSR 重新成为焦点。关键在于**SSR + 水合（Hydration）**的结合，让首屏渲染在服务端完成，后续交互在客户端接管。

React Server Components（RSC）更是将组件的服务端/客户端边界精细化到组件级别。

## AI 驱动的开发体验

GitHub Copilot 的普及已经改变了写代码的方式。2026 年的趋势是：

- **AI 生成完整页面**而非仅补全代码
- **智能重构建议**而非简单的代码补全
- **自动化测试生成**降低测试编写成本

## WebAssembly：从 Demo 到生产

Wasm 已经不再是玩具，真正落地的场景包括：

- Figma 的设计编辑器（C++ 编译为 Wasm）
- Photoshop Web 版的图像处理引擎
- 数据库（如 DuckDB-Wasm）在浏览器端运行

## 边缘计算与 Serverless

Vercel Edge Functions、Cloudflare Workers 让前端开发者可以直接在边缘节点运行代码，全球延迟 < 50ms。

## 不变的真理

无论技术如何变化，这些原则始终成立：

1. **用户体验优先**：技术选型服务于体验，而非反之
2. **渐进增强**：核心功能在最低环境下可用
3. **性能即功能**：加载速度本身就是一种功能
4. **可维护性**：代码是写给人看的，顺便给机器执行

> 预测未来最好的方式，就是亲手创造它。`
  }
]

export const mockPosts = reactive([])
export const articlesLoading = ref(false)
export const articlesError = ref('')

export async function initArticles() {
  articlesLoading.value = true
  articlesError.value = ''
  try {
    const data = await articlesAPI.list()
    mockPosts.splice(0, mockPosts.length, ...data)
  } catch (e) {
    console.error('加载文章列表失败:', e)
    articlesError.value = '无法连接到服务器，请确认后端已启动'
  } finally {
    articlesLoading.value = false
  }
}

export function getPostById(id) {
  return mockPosts.find(post => post.id === id) || null
}

export async function removePost(id) {
  await articlesAPI.remove(id)
  const index = mockPosts.findIndex(post => post.id === id)
  if (index !== -1) {
    mockPosts.splice(index, 1)
    return true
  }
  return false
}

export async function updatePost(id, newData) {
  const saved = await articlesAPI.update(id, newData)
  const post = mockPosts.find(post => post.id === id)
  if (post) Object.assign(post, saved)
  return true
}

export async function addPost(article) {
  const saved = await articlesAPI.create(article)
  mockPosts.push(saved)
  return saved
}