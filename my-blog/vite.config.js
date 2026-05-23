import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // Vite 开发代理：将 /api 请求转发到 Spring Boot 后端
    // 开发阶段无需 CORS 处理，解决跨域问题
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Spring Boot 默认端口
        changeOrigin: true,
        secure: false
      }
    }
  }
})