import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
/*
 * ======================== 路由注册 ========================
 * 导入 Vue Router 实例
 * app.use(router) → 将路由系统安装到 Vue 应用中
 *
 * 安装后的效果：
 *   - <router-view /> 组件全局可用（用于 App.vue 中的路由出口）
 *   - <router-link /> 组件全局可用（用于 NavBar.vue 中的导航链接）
 *   - $router（路由实例）和 $route（当前路由对象）可在组件中通过 useRouter/useRoute 访问
 */
import router from './router'

/*
 * createApp(App) → 创建 Vue 应用实例，传入根组件 App.vue
 * .use(router)    → 注册路由插件
 * .mount('#app')  → 挂载到 index.html 中的 <div id="app">
 *
 * 执行顺序说明：
 *   必须先 use(router) 再 mount，确保路由系统在首次渲染前已就绪
 *   否则模板中的 <router-view /> 和 <router-link /> 无法识别
 */
createApp(App).use(router).mount('#app')