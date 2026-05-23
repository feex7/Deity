import { createRouter, createWebHistory } from 'vue-router'
import AboutPage    from '../views/AboutPage.vue'

/*
 * ======================== 路由配置 ========================
 * createWebHistory()
 *   使用 HTML5 History 模式（URL 中不带 # 号）
 *   需要服务器配置回退规则（Vite 开发服务器已内置支持）
 *
 *   对比 createWebHashHistory()：
 *     - History 模式：URL 干净美观 (example.com/posts)
 *     - Hash 模式：URL 带 # 号 (example.com/#/posts)
 *
 * linkActiveClass: 'active'
 *   当 <router-link> 的 to 属性与当前路由匹配时，
 *   自动添加 'active' CSS 类，无需在组件中手动判断
 *
 * routes 数组：
 *   每个路由包含：
 *     - path：URL 路径
 *     - name：路由名称（用于编程式导航 router.push({ name: 'xxx' })）
 *     - component：懒加载的页面组件
 *
 *   component 使用动态 import() 实现路由级代码分割：
 *     只有访问该路由时才会加载对应的 JS 文件
 *     减少首屏加载体积，提升页面性能
 */
const router = createRouter({
  history: createWebHistory(),
  linkActiveClass: 'active',
  routes: [
    { path: '/', name: 'home', component: () => import('../views/SplashScreen.vue') },
    { path: '/about', name: 'about', component: AboutPage },
    { path: '/favorites', name: 'favorites', component: () => import('../views/FavoritesPage.vue') },
    { path: '/posts', name: 'blog', component: () => import('../views/BlogPage.vue') },
    { path: '/posts/:id', name: 'postDetail', component: () => import('../views/PostDetail.vue') },
    { path: '/write', name: 'write', component: () => import('../views/WritePage.vue') },
    { path: '/projects', name: 'projects', component: () => import('../views/ProjectsPage.vue') },
    { path: '/essays', name: 'essays', component: () => import('../views/EssaysPage.vue') },
    { path: '/plans', name: 'plans', component: () => import('../views/PlansPage.vue') },
    { path: '/timeline', name: 'timeline', component: () => import('../views/TimelinePage.vue') }
  ]
})

router.beforeEach((to, from, next) => {
  if (to.path === '/write') {
    const verified = sessionStorage.getItem('adminVerified') === 'true'
    if (!verified) {
      alert('请先点击网站首页头像进行管理员验证')
      return next({ path: '/' })
    }
  }
  next()
})

export default router