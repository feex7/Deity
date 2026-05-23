<!--
============================================================
组件名称: NavBar（网站头部导航栏）
功能描述: 固定顶部导航栏，通过 Vue Router 的 router-link 实现页面跳转
         当前导航项：首页/博客/关于/项目/随笔/计划/时间轴
导航模式: 接入 Vue Router 后，使用 <router-link> 替代手动 emit
          active 状态由 Vue Router 自动管理，无需手动 props 传递
============================================================
-->
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const navItems = [
  { name: 'home',      label: '首页',   href: '/' },
  { name: 'blog',      label: '博客',   href: '/posts' },
  { name: 'about',     label: '关于',   href: '/about' },
  { name: 'favorites', label: '收藏夹', href: '/favorites' },
  { name: 'projects',  label: '项目',   href: '/projects' },
  { name: 'essay',     label: '随笔',   href: '/essays' },
  { name: 'plan',      label: '计划',   href: '/plans' },
  { name: 'timeline',  label: '时间轴', href: '/timeline' },
  { name: 'write',     label: '写文章', href: '/write' }
]

const isScrolled = ref(false)
const menuOpen = ref(false)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 10
}

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value
  document.body.style.overflow = menuOpen.value ? 'hidden' : ''
}

const closeMenu = () => {
  menuOpen.value = false
  document.body.style.overflow = ''
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.body.style.overflow = ''
})
</script>

<template>
  <header class="navbar" :class="{ scrolled: isScrolled }">
    <div class="navbar-inner">

      <nav class="nav-desktop">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="item.href"
          class="nav-link"
          exact-active-class="active"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="navbar-right">
        <button
          class="hamburger"
          :class="{ active: menuOpen }"
          @click="toggleMenu"
          aria-label="切换菜单"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </div>

    <!--
    ======================== 移动端遮罩层 ========================
    v-if="menuOpen" → 只在菜单展开时渲染（条件渲染，节省性能）
    @click="closeMenu" → 点击遮罩层关闭菜单
    position: fixed; inset: 0 → 覆盖整个视口
    z-index: 104 → 高于导航栏（100）但低于侧滑菜单（105）
    -->
    <div v-if="menuOpen" class="mobile-overlay" @click="closeMenu"></div>

    <!--
    ======================== 移动端侧滑菜单 ========================
    v-if="menuOpen" → 只在菜单展开时渲染
    :class="{ open: menuOpen }" → CSS transition 控制滑入/滑出动画
    position: fixed; right: 0 → 固定在屏幕右侧
    width: 280px → 菜单宽度
    transform: translateX(100%) → 初始隐藏在屏幕右侧外部
    .open 时 transform: translateX(0) → 滑入到正常位置
    overflow-y: auto → 导航项过多时允许垂直滚动
    -->
    <div class="mobile-menu" :class="{ open: menuOpen }">
      <!-- 菜单头部：标题 + 关闭按钮 -->
      <div class="mobile-menu-header">
        <span class="mobile-menu-title">导航菜单</span>
        <button class="mobile-close-btn" @click="closeMenu">&times;</button>
      </div>

      <!-- 移动端导航列表 -->
      <nav class="mobile-nav">
        <!--
          移动端导航项同样使用 <router-link>，
          区别在于点击后需要关闭侧滑菜单 → @click="closeMenu"
          这样用户点击跳转时菜单自动收起，不会停留在新页面上
        -->
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="item.href"
          class="mobile-nav-link"
          exact-active-class="active"
          @click="closeMenu"
        >
          <!-- 激活项左侧的绿色小圆点指示器 -->
          <span class="mobile-nav-dot"></span>
          {{ item.label }}
        </router-link>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 60px;
  transition: background-color 0.3s ease, box-shadow 0.3s ease;
}

.navbar.scrolled {
  background-color: rgba(10, 10, 10, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.05),
    0 4px 20px rgba(0, 0, 0, 0.3);
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  position: relative;
}

.navbar-right {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
}

.nav-desktop {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  text-decoration: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  letter-spacing: 0.5px;
  padding: 8px 0;
  position: relative;
  transition: color 0.3s ease;
}

.nav-link:hover {
  color: #00ff80;
  text-shadow: 0 0 12px rgba(0, 255, 128, 0.4);
  transform: translateY(-1px);
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #00ff80;
  border-radius: 1px;
  box-shadow: 0 0 6px rgba(0, 255, 128, 0.5);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.3s ease;
}

.nav-link:hover::after {
  transform: scaleX(1);
}

.nav-link.active {
  color: #f9ca24;
  text-shadow: 0 0 8px rgba(249, 202, 36, 0.3);
}

.hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 28px;
  height: 28px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  z-index: 110;
}

.hamburger span {
  display: block;
  width: 100%;
  height: 2px;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 1px;
  transition: all 0.3s ease;
  transform-origin: center;
}

.hamburger.active span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.hamburger.active span:nth-child(2) {
  opacity: 0;
  transform: scaleX(0);
}

.hamburger.active span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.6);
  z-index: 104;
  animation: fadeIn 0.3s ease;
}

.mobile-menu {
  position: fixed;
  top: 0;
  right: 0;
  width: 280px;
  height: 100vh;
  background-color: #111;
  z-index: 105;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: -4px 0 30px rgba(0, 0, 0, 0.5);
  overflow-y: auto;
}

.mobile-menu.open {
  transform: translateX(0);
}

.mobile-menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.mobile-menu-title {
  color: #00ff80;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
}

.mobile-close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 24px;
  cursor: pointer;
  line-height: 1;
  padding: 0;
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  padding: 12px 0;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 15px;
  letter-spacing: 0.5px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.mobile-nav-link:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: #fff;
}

.mobile-nav-link.active {
  color: #00ff80;
  background-color: rgba(0, 255, 128, 0.05);
}

.mobile-nav-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: transparent;
  transition: background-color 0.3s ease;
}

.mobile-nav-link.active .mobile-nav-dot {
  background-color: #00ff80;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .nav-desktop {
    display: none;
  }

  .hamburger {
    display: flex;
  }

  .navbar-inner {
    padding: 0 16px;
    justify-content: flex-start;
  }

  .navbar-right {
    right: 16px;
  }
}
</style>