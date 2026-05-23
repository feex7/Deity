<!--
  ============================================================
  组件名称: SplashScreen（博客过渡主页）
  功能描述: 博客首页过渡页面，包含头像展示、打字机文字、
            管理员验证弹窗、社交链接、动态副标题等模块
  后端对接: 密码验证完全由 Spring Boot 后端处理
            密码从不暴露在前端代码中，杜绝浏览器 DevTools 泄密风险
            Vite 开发环境通过代理转发 /api 请求到后端
  ============================================================
-->
<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import authAPI from '../api/auth'
import { isAdmin } from '../utils/isAdmin.js'
import MusicPlayer from '../components/MusicPlayer.vue'

/*
 * ======================== 打字机效果配置 ========================
 * fullText     - 要显示的完整文字内容
 * typingSpeed  - 打字速度（毫秒/字），值越小越快
 * deleteSpeed  - 删除速度（毫秒/字），值越小越快
 * pauseDuration - 打完后停顿时间（毫秒），之后开始删除
 */
const fullText = '欢迎来到我的的个人网站'
const typingSpeed = 50
const deleteSpeed = 70
const pauseDuration = 1800

/*
 * ======================== 管理员验证配置 ========================
 * 安全原则：密码绝不存储在前端代码中
 *   验证流程：用户输入密码 → 前端发送到 Spring Boot 后端 →
 *   后端校验（SHA-256 + 盐值 + 时序安全比对）→ 返回结果
 *   前端只接收 { success: true/false, message: "..." }
 *   密码原文从头到尾不经过前端持久化
 */
const ADMIN_USERNAME = 'admin'
const MAX_ATTEMPTS = 5

/*
 * ======================== 响应式状态管理 ========================
 * Vue3 Composition API 使用 ref() 创建响应式数据，
 * 当这些值发生变化时，模板会自动重新渲染
 */
const displayedText = ref('')     // 打字机当前显示的文本内容
const showContent = ref(false)    // 是否显示页面主体内容（控制加载动画）
const showModal = ref(false)      // 是否显示管理员验证弹窗
const password = ref('')          // 密码输入框内容（双向绑定 v-model）
const attempts = ref(0)           // 已失败的验证次数
const isVerified = ref(false)     // 是否已通过管理员验证
const isLoading = ref(false)      // 是否正在请求后端验证接口
const errorMessage = ref('')      // 验证失败时的错误提示文本
const successMessage = ref('')    // 验证成功时的成功提示文本

const messages = ref([])
const guestNickname = ref('')
const guestContent = ref('')
const guestSubmitting = ref(false)
const guestLoading = ref(false)
const guestError = ref('')
const guestFormError = ref('')
const guestSubmitted = ref(false)

/*
 * ======================== 动态副标题配置 ========================
 * tags       - 轮播显示的标签数组
 * currentTag - 当前显示的标签文本
 * tagIndex   - 当前标签索引
 * tagTimer   - 标签轮播定时器
 */
const tags = ['前端开发', '技术分享', '生活记录', '开源项目']
const currentTag = ref(tags[0])
let tagIndex = 0
let typeTimer = null   // 打字机定时器句柄
let tagTimer = null    // 标签轮播定时器句柄

/*
 * ======================== 计算属性 ========================
 * disabled - 判断验证按钮是否应该被禁用
 *            条件：密码为空 || 正在加载 || 超过最大尝试次数
 */
const disabled = computed(() => !password.value || isLoading.value || attempts.value >= MAX_ATTEMPTS)

let countdownTimer = null  // 5分钟冷却计时器句柄

/*
 * ======================== 头像点击事件 ========================
 * 功能：点击头像时触发管理员验证弹窗
 * 流程：
 *   1. 如果已验证通过，直接忽略点击
 *   2. 否则打开弹窗，并重置输入状态
 */
const handleAvatarClick = () => {
  if (isVerified.value) return

  showModal.value = true
  password.value = ''
  errorMessage.value = ''
  successMessage.value = ''
}

/*
 * ======================== 提交验证 ========================
 * 功能：将用户输入的密码发送到 Spring Boot 后端进行验证
 * 安全说明：
 *   - 密码通过 HTTPS POST 请求体发送到后端
 *   - 前端不做任何密码比对，比对逻辑完全在后端
 *   - 后端返回的是 success/message，不包含任何密码信息
 * 流程：
 *   1. 检查按钮禁用状态
 *   2. 设置加载中
 *   3. 调用 authAPI.verify() 发送 POST /api/verify
 *   4. 成功 → verificationSuccess()
 *   5. 失败 → verificationFailed()
 *   6. 网络错误 → 提示"后端服务不可用"
 */
const submitVerification = async () => {
  if (disabled.value) return

  isLoading.value = true
  errorMessage.value = ''

  try {
    // 调用 Spring Boot 后端 POST /api/verify 接口
    // 请求体：{ username: "admin", password: "用户输入" }
    // 响应体：{ success: true/false, message: "..." }
    const result = await authAPI.verify(ADMIN_USERNAME, password.value)

    if (result.success) {
      verificationSuccess()
    } else {
      verificationFailed(result.message || '用户名或密码错误')
    }
  } catch (error) {
    verificationFailed('后端服务暂不可用，请确认后端已启动')
  }

  isLoading.value = false
}

/*
 * ======================== 验证成功处理 ========================
 * 功能：管理员验证成功后的状态更新
 * 流程：
 *   1. 更新验证状态为 true
 *   2. 显示成功提示信息
 *   3. 将验证状态持久化到 sessionStorage（会话期间有效）
 *   4. 1 秒后自动关闭弹窗
 * 安全说明：
 *   sessionStorage 仅记录验证通过标志（"true"），不记录任何密码
 *   浏览器标签页关闭后自动清除，避免永久状态残留
 */
const verificationSuccess = () => {
  isVerified.value = true
  successMessage.value = '验证成功！欢迎回来，管理员'
  sessionStorage.setItem('adminVerified', 'true')
  fetchMessages()

  setTimeout(() => {
    showModal.value = false
    successMessage.value = ''
  }, 1000)
}

/*
 * ======================== 验证失败处理 ========================
 * 功能：处理验证失败逻辑，包含重试次数限制
 * 流程：
 *   1. 累加失败次数
 *   2. 计算剩余次数
 *   3. 如果达到上限 → 显示冷却提示，启动 5 分钟倒计时
 *   4. 如果还有机会 → 显示剩余次数和错误信息
 * 参数：
 *   msg - 后端返回的错误信息
 */
const verificationFailed = (msg) => {
  attempts.value++
  const remaining = MAX_ATTEMPTS - attempts.value

  if (remaining <= 0) {
    errorMessage.value = '验证次数已用完，请 5 分钟后再试'
    password.value = ''

    countdownTimer = setTimeout(() => {
      attempts.value = 0
      errorMessage.value = ''
    }, 5 * 60 * 1000)
  } else {
    errorMessage.value = `${msg}，还剩 ${remaining} 次机会`
    password.value = ''
  }
}

/*
 * ======================== 关闭弹窗 ========================
 * 功能：关闭管理员验证弹窗
 * 触发方式：点击取消按钮 / 点击遮罩层 / 点击关闭按钮
 */
const closeModal = () => {
  showModal.value = false
}

/*
 * ======================== 打字机效果 - 打字阶段 ========================
 * 功能：将文字逐字显示，模拟打字机效果
 * 原理：
 *   1. 使用 setInterval 定时器，每隔 typingSpeed 毫秒执行一次
 *   2. 每次执行取出 fullText 的下一个字符追加到 displayedText
 *   3. 所有字符打完 → 清除定时器 → 延迟后进入删除阶段
 */
const typeText = () => {
  let index = 0
  displayedText.value = ''

  typeTimer = setInterval(() => {
    if (index < fullText.length) {
      displayedText.value += fullText[index]
      index++
    } else {
      clearInterval(typeTimer)
      typeTimer = null
      setTimeout(deleteText, pauseDuration)
    }
  }, typingSpeed)
}

/*
 * ======================== 打字机效果 - 删除阶段 ========================
 * 功能：将文字逐字删除，形成完整的打字/删除循环
 * 原理：
 *   1. 从最后一个字符开始截断（substring 方法）
 *   2. 每次删除一个字符
 *   3. 全部删除后 → 停顿 500ms → 重新开始打字阶段
 */
const deleteText = () => {
  let index = fullText.length

  typeTimer = setInterval(() => {
    if (index > 0) {
      index--
      displayedText.value = fullText.substring(0, index)
    } else {
      clearInterval(typeTimer)
      typeTimer = null
      setTimeout(typeText, 500)
    }
  }, deleteSpeed)
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const fetchMessages = async () => {
  guestLoading.value = true
  guestError.value = ''
  try {
    const res = await fetch('/api/guestbook')
    if (!res.ok) throw new Error('加载失败')
    messages.value = await res.json()
  } catch (e) {
    guestError.value = '留言加载失败，请确认后端已启动'
  } finally {
    guestLoading.value = false
  }
}

const submitMessage = async () => {
  guestFormError.value = ''
  const nickname = guestNickname.value.trim()
  const content = guestContent.value.trim()
  if (nickname.length > 20) { guestFormError.value = '昵称不能超过20个字符'; return }
  if (!content) { guestFormError.value = '请输入留言内容'; return }
  if (content.length > 500) { guestFormError.value = '留言不能超过500个字符'; return }
  guestSubmitting.value = true
  try {
    const res = await fetch('/api/guestbook', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nickname, content })
    })
    if (!res.ok) {
      const err = await res.json()
      throw new Error(err.error || '提交失败')
    }
    const saved = await res.json()
    messages.value.unshift(saved)
    guestContent.value = ''
    guestFormError.value = ''
    guestSubmitted.value = true
  } catch (e) {
    guestFormError.value = e.message || '提交失败，请重试'
  } finally {
    guestSubmitting.value = false
  }
}

const deleteMessage = async (id) => {
  try {
    const res = await fetch(`/api/guestbook/${id}`, { method: 'DELETE' })
    if (res.ok) {
      messages.value = messages.value.filter(m => m.id !== id)
    }
  } catch (e) {
    console.error('删除留言失败:', e)
  }
}

/*
 * ======================== 生命周期钩子 ========================
 * onMounted：组件挂载到DOM后执行
 *   1. 恢复验证状态（从 sessionStorage 读取）
 *   2. 延迟 300ms 后触发页面加载动画（内容淡入）
 *   3. 启动打字机效果
 *   4. 启动副标题轮播定时器
 *
 * onUnmounted：组件从DOM卸载前执行
 *   1. 清除所有定时器，防止内存泄漏
 */
onMounted(() => {
  if (sessionStorage.getItem('adminVerified') === 'true') {
    isVerified.value = true
    fetchMessages()
  }

  setTimeout(() => {
    showContent.value = true
    typeText()
  }, 300)

  tagTimer = setInterval(() => {
    tagIndex = (tagIndex + 1) % tags.length
    currentTag.value = tags[tagIndex]
  }, 3000)
})

onUnmounted(() => {
  if (typeTimer) clearInterval(typeTimer)
  if (tagTimer) clearInterval(tagTimer)
  if (countdownTimer) clearTimeout(countdownTimer)
})
</script>

<template>
  <div class="splash-container">
    <div class="hero-section">
      <div class="gradient-bg"></div>
      <div class="grid-bg"></div>

      <div class="content-wrapper" :class="{ show: showContent }">

        <div class="avatar-wrapper">
          <div class="avatar-glow"></div>
          <img src="/touxiang.jpg" class="avatar" alt="头像" @click="handleAvatarClick" />
          <div v-if="isVerified" class="verified-badge">✓ 管理员已验证</div>
          <div v-if="!isVerified" class="click-hint">点击头像进行验证</div>
        </div>

        <div class="nickname">Deity</div>

        <h1 class="typewriter-text">
          <span>{{ displayedText }}</span>
          <span class="cursor"></span>
        </h1>

        <p class="subtitle">
          <span class="static-text">专注于</span>
          <span class="dynamic-text">{{ currentTag }}</span>
        </p>

        <div class="social-links">
          <a href="#" class="social-link" title="GitHub">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
          </a>
          <a href="#" class="social-link" title="抖音">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19.5 8.5a6.5 6.5 0 0 1-4-1.5v5.5A6.5 6.5 0 0 1 9 19a6.5 6.5 0 0 1-6.5-6.5A6.5 6.5 0 0 1 9 6c.2 0 .3 0 .5.1v3.8c-.2 0-.3-.1-.5-.1a2.5 2.5 0 1 0 2.5 2.5V0h3.5c0 1.4 1.1 2.5 2.5 2.5v3.5c1 0 1.9-.5 2.5-1.2v3.7z"/>
            </svg>
          </a>
          <a href="#" class="social-link" title="QQ">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C8 2 4.5 4.5 4.5 8c0 1.5.5 2.8 1.5 3.8-.6 1-1 2-1 3.2 0 2.5 3 4 7 4s7-1.5 7-4c0-1.2-.4-2.2-1-3.2 1-1 1.5-2.3 1.5-3.8C19.5 4.5 16 2 12 2zm-3 6.5a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm6 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm-3 3c-2 0-3.5 1-3.5 2s1.5 2 3.5 2 3.5-1 3.5-2-1.5-2-3.5-2z"/>
            </svg>
          </a>
        </div>

        <div v-if="isAdmin()" class="scroll-hint">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 3v8M4 8l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>向下滚动查看留言</span>
        </div>
      </div>
    </div>

    <section class="guestbook-section" :class="{ show: showContent }">
      <div class="guestbook-inner">
        <h2 class="guestbook-title">
          <span class="title-icon">💬</span>
          留言板
        </h2>
        <p class="guestbook-desc">留下你的足迹，分享你的想法</p>

        <div class="guestbook-form">
          <div v-if="guestSubmitted && !isAdmin()" class="submit-success">
            <span class="success-icon">✅</span>
            <p>留言已提交，感谢你的反馈</p>
            <button class="another-msg" @click="guestSubmitted = false; guestContent = ''">再写一条</button>
          </div>
          <template v-else>
          <div class="form-row">
            <input
              v-model="guestNickname"
              type="text"
              placeholder="你的昵称（选填）"
              class="form-input"
              maxlength="20"
              :disabled="guestSubmitting"
            />
            <span class="char-count">{{ guestNickname.length }}/20</span>
          </div>
          <div class="form-row textarea-row">
            <textarea
              v-model="guestContent"
              placeholder="写点什么吧..."
              class="form-textarea"
              rows="3"
              maxlength="500"
              :disabled="guestSubmitting"
            ></textarea>
            <span class="char-count">{{ guestContent.length }}/500</span>
          </div>
          <p v-if="guestFormError" class="form-error">{{ guestFormError }}</p>
          <button
            class="submit-btn-guest"
            @click="submitMessage"
            :disabled="guestSubmitting || !guestContent.trim()"
          >
            <span v-if="guestSubmitting" class="spinner-guest"></span>
            <span v-else>发布留言</span>
          </button>
          </template>
        </div>

        <div v-if="isAdmin()">
          <div v-if="guestLoading" class="guest-loading">
            <div class="spinner-guest"></div>
            <span>加载留言中...</span>
          </div>

          <div v-else-if="guestError" class="guest-error-state">
            <p>{{ guestError }}</p>
            <button class="retry-btn" @click="fetchMessages">重新加载</button>
          </div>

          <div v-else-if="!messages.length" class="guest-empty">
            <span class="empty-icon">📝</span>
            <p>还没有留言</p>
          </div>

          <div v-else class="guest-list">
            <div v-for="msg in messages" :key="msg.id" class="guest-card">
              <div class="guest-card-header">
                <div class="guest-avatar">{{ (msg.nickname || '匿').charAt(0) }}</div>
                <div class="guest-meta">
                  <span class="guest-name">{{ msg.nickname || '匿名' }}</span>
                  <span class="guest-time">{{ formatTime(msg.createdAt) }}</span>
                </div>
                <button
                  v-if="isAdmin()"
                  class="guest-delete"
                  @click="deleteMessage(msg.id)"
                  title="删除留言"
                >✕</button>
              </div>
              <p class="guest-text">{{ msg.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!--
      ======================== 管理员验证弹窗 ========================
      显示条件：v-if="showModal"（showModal 为 true 时渲染）
      结构：
        .modal-overlay  →  半透明黑色遮罩层，点击遮罩可关闭弹窗
        .modal-content  →  弹窗主体（深色圆角卡片）
          .modal-header →  标题 + 关闭按钮
          .modal-body   →  描述文字 + 密码输入框 + 提示信息
          .modal-footer →  取消按钮 + 验证按钮
      安全说明：
        密码通过 HTTPS POST 发送到 Spring Boot 后端 /api/verify
        前端不做任何密码比对或本地验证
        密码绝不出现在前端的 JS Bundle 中
    -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>管理员验证</h2>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>

        <div class="modal-body">
          <p class="modal-desc">请输入管理员密码以继续</p>

          <div class="input-group">
            <!--
              type="password" → 密码掩码显示
              v-model="password" → 双向绑定到 password 响应式变量
              @keyup.enter → 按回车直接提交验证
              :disabled → 加载中或超过次数时禁用输入
              autofocus → 弹窗打开后自动聚焦
            -->
            <input
              v-model="password"
              type="password"
              placeholder="请输入密码"
              class="password-input"
              @keyup.enter="submitVerification"
              :disabled="isLoading || attempts >= MAX_ATTEMPTS"
              autofocus
            />
          </div>

          <p v-if="successMessage" class="success-msg">{{ successMessage }}</p>
          <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
          <p v-if="attempts > 0 && attempts < MAX_ATTEMPTS" class="attempts-msg">
            剩余尝试次数：{{ MAX_ATTEMPTS - attempts }}
          </p>
        </div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" @click="submitVerification" :disabled="disabled">
            <span v-if="isLoading" class="spinner"></span>
            <span v-else>验证</span>
          </button>
        </div>
      </div>
    </div>

    <MusicPlayer />
  </div>
</template>

<style scoped>
/*
 * ================================================================
 *                        样式模块说明
 * ================================================================
 * 1. 布局层：splash-container（全屏容器）
 * 2. 背景层：gradient-bg（渐变）+ grid-bg（网格）
 * 3. 内容层：content-wrapper（页面加载动画）
 * 4. 头像模块：avatar-wrapper / avatar-glow / avatar / 徽章
 * 5. 文字模块：typewriter-text / cursor（打字机效果）
 * 6. 副标题模块：subtitle
 * 7. 社交链接模块：social-links / social-link
 * 8. 弹窗模块：modal-overlay / modal-content（验证弹窗）
 * 9. 动画模块：blink / fadeIn / scaleIn / spin（关键帧动画）
 * 10. 响应式模块：媒体查询适配不同屏幕尺寸
 * ================================================================
 */

/*
 * ======================== 1. 全屏容器 ========================
 * position: relative   → 为内部绝对定位的子元素提供定位参考
 * flex-direction: column → 子元素垂直排列
 * justify-content: center → 垂直居中
 * align-items: center → 水平居中
 * min-height: 100vh   → 最小高度等于视口高度，确保全屏覆盖
 * background-color: #0a0a0a → 稍浅的黑色背景（不是纯黑）
 * overflow: hidden    → 隐藏超出容器范围的内容（防止光晕溢出）
 */
/*
 * padding-top: 60px → 为固定的顶部导航栏（NavBar）预留空间
 *                      防止导航栏遮挡页面顶部内容
 *                      导航栏高度约 60px，此处与之匹配
 */
.splash-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #0a0a0a;
  min-height: 100vh;
}

.hero-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
  padding-top: 60px;
}

/*
 * ======================== 2a. 渐变背景装饰 ========================
 * position: absolute  → 脱离文档流，覆盖整个容器
 * inset: 0           → 等同于 top:0; right:0; bottom:0; left:0
 * radial-gradient    → 径向渐变，中心在水平居中、垂直28%处
 * ellipse            → 椭圆形渐变（横向扩散较宽）
 * pointer-events: none → 不接收鼠标事件，点击穿透到下方元素
 */
.gradient-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center 28%,
    rgba(0, 255, 128, 0.06) 0%,
    transparent 60%
  );
  pointer-events: none;
}

/*
 * ======================== 2b. 网格背景装饰 ========================
 * 使用两条 linear-gradient 组合绘制网格：
 *   第一条：水平方向的线条（从上到下排列）
 *   第二条：垂直方向的线条（从左到右排列）
 * background-size: 40px 40px → 每个格子的宽高
 * 颜色透明度仅 0.03，非常低调
 */
.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 255, 128, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 128, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

/*
 * ======================== 3. 内容主容器（页面加载动画） ========================
 * z-index: 1 → 确保内容在背景层之上
 *
 * 初始状态（隐藏）：
 *   opacity: 0 → 完全透明
 *   transform: translateY(30px) → 向下偏移30px
 *
 * 显示状态（添加 .show 类后）：
 *   opacity: 1 → 完全不透明
 *   transform: translateY(0) → 归位
 *
 * transition 参数说明：
 *   0.8s → 动画持续时间
 *   cubic-bezier(0.16, 1, 0.3, 1) → 自定义缓动曲线（先快后慢，有回弹感）
 */
.content-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.content-wrapper.show {
  opacity: 1;
  transform: translateY(0);
}

.scroll-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 40px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 1.5px;
  animation: scrollPulse 2s ease-in-out infinite;
}

.scroll-hint svg {
  width: 20px;
  height: 20px;
  animation: scrollBounce 1.5s ease-in-out infinite;
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.45; }
  50% { opacity: 0.85; }
}

@keyframes scrollBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(8px); }
}

/*
 * ======================== 4a. 头像容器 ========================
 * position: relative → 为内部的光晕（absolute）提供定位参考
 * flex-direction: column → 头像在上，徽章/提示在下
 */
.avatar-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 25px;
}

/*
 * ======================== 4b. 头像光晕 ========================
 * 核心：radial-gradient 径向渐变 + filter: blur 模糊
 * width/height: 250px → 光晕范围
 * 渐变层次（从内到外）：
 *   0%    → 最高亮度 0.25
 *   30%   → 中等亮度 0.15
 *   60%   → 低亮度 0.05
 *   80%   → 完全透明
 * border-radius: 50% → 圆形光晕
 * translate(-50%, -50%) → 将光晕精确居中于头像
 */
.avatar-glow {
  position: absolute;
  width: 250px;
  height: 250px;
  background: radial-gradient(
    circle,
    rgba(0, 255, 128, 0.25) 0%,
    rgba(0, 255, 128, 0.15) 30%,
    rgba(0, 255, 128, 0.05) 60%,
    transparent 80%
  );
  border-radius: 50%;
  pointer-events: none;
  filter: blur(15px);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/*
 * ======================== 4c. 头像图片 ========================
 * border-radius: 50% → 将正方形图片裁剪为圆形
 * object-fit: cover  → 图片按比例填充，裁剪超出部分（防止拉伸）
 * transition         → 三个属性同时过渡，形成平滑动画
 * border             → 默认半透明白色边框
 * cursor: pointer    → 鼠标悬停时显示手型光标（提示可点击）
 */
.avatar {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  object-fit: cover;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  border: 3px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  position: relative;
  z-index: 1;
}

/*
 * ======================== 4d. 头像悬停效果 ========================
 * box-shadow 三层阴影组合：
 *   第一层：0 0 20px → 紧贴头像的亮绿色光晕（0.5透明度）
 *   第二层：0 0 40px → 稍远的外层光晕（0.3透明度，更柔和）
 *   第三层：0 12px 24px → 模拟光源在上的阴影（向下偏移12px）
 * transform: translateY(-8px) → 向上浮动8px
 * scale(1.05) → 放大5%
 * border-color → 边框变亮变绿
 */
.avatar:hover {
  box-shadow:
    0 0 20px rgba(0, 255, 128, 0.5),
    0 0 40px rgba(0, 255, 128, 0.3),
    0 12px 24px rgba(0, 0, 0, 0.3);
  transform: translateY(-8px) scale(1.05);
  border-color: rgba(0, 255, 128, 0.8);
}

/*
 * ======================== 4e. 验证状态徽章 ========================
 * 验证通过后显示在头像下方的绿色胶囊形标记
 * border-radius: 20px → 胶囊形状（大于高度的一半即为胶囊）
 * rgba(0, 255, 128, 0.15) → 半透明绿色背景
 */
.verified-badge {
  margin-top: 12px;
  padding: 4px 14px;
  background-color: rgba(0, 255, 128, 0.15);
  border: 1px solid rgba(0, 255, 128, 0.4);
  border-radius: 20px;
  font-size: 12px;
  color: #00ff80;
  letter-spacing: 0.5px;
}

/*
 * ======================== 4f. 点击提示文字 ========================
 * 未验证时显示在头像下方的提示
 * 悬停在头像区域时文字亮度增加（引导用户点击）
 */
.click-hint {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1px;
  transition: color 0.3s ease;
}

.avatar-wrapper:hover .click-hint {
  color: rgba(255, 255, 255, 0.6);
}

.nickname {
  font-size: 24px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 3px;
  margin-top: 8px;
  margin-bottom: 16px;
}

.typewriter-text {
  color: #FFFFFF;
  font-family: Arial, Helvetica, sans-serif;
  font-size: 22px;
  letter-spacing: 1.5px;
  line-height: 1.2;
  margin: 0;
}

/*
 * ======================== 5b. 光标样式 ========================
 * display: inline-block → 让 span 可以设置宽高
 * width: 2px → 光标宽度（细竖线效果）
 * height: 1em → 光标高度等于字号大小
 * background-color: #00ff80 → 绿色光标
 * animation: blink 0.8s infinite → 闪烁动画（0.8秒周期，无限循环）
 * vertical-align: middle → 垂直居中对齐文字
 */
.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: #00ff80;
  animation: blink 0.8s infinite;
  margin-left: 3px;
  border-radius: 1px;
  vertical-align: middle;
}

/*
 * ======================== 6. 副标题样式 ========================
 * 使用 flex 布局让"专注于"和动态标签并排显示
 */
.subtitle {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  font-size: 15px;
  align-items: center;
}

/* 静态文字：低透明度，灰白色 */
.static-text {
  color: rgba(255, 255, 255, 0.45);
}

/* 动态标签：绿色高亮，模拟高亮文本 */
.dynamic-text {
  color: #00ff80;
  font-weight: 500;
  transition: opacity 0.3s ease;
}

/*
 * ======================== 7. 社交链接样式 ========================
 * gap: 18px → 链接之间的间距
 */
.social-links {
  display: flex;
  gap: 18px;
  margin-top: 30px;
}

/* 每个社交链接按钮 */
.social-link {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
  text-decoration: none;
}

/* 悬停效果：边框和图标变绿，上浮，添加发光 */
.social-link:hover {
  border-color: #00ff80;
  color: #00ff80;
  box-shadow: 0 0 15px rgba(0, 255, 128, 0.3);
  transform: translateY(-3px);
}

.social-link svg {
  width: 16px;
  height: 16px;
}

.guestbook-section {
  width: 100%;
  max-width: 680px;
  padding: 0 24px 80px;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.guestbook-section.show {
  opacity: 1;
  transform: translateY(0);
}

.guestbook-inner {
  width: 100%;
}

.guestbook-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
}

.title-icon {
  font-size: 24px;
}

.guestbook-desc {
  color: rgba(255, 255, 255, 0.35);
  font-size: 13px;
  margin: 0 0 28px;
}

.guestbook-form {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 32px;
}

.form-row {
  position: relative;
  margin-bottom: 14px;
}

.textarea-row {
  margin-bottom: 8px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  border-color: rgba(0, 255, 128, 0.4);
}

.form-input:disabled,
.form-textarea:disabled {
  opacity: 0.5;
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: rgba(255, 255, 255, 0.25);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  line-height: 1.6;
}

.char-count {
  position: absolute;
  right: 10px;
  bottom: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.2);
  pointer-events: none;
}

.form-error {
  color: #ff4444;
  font-size: 12px;
  margin: 8px 0 12px;
}

.submit-btn-guest {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 11px;
  background: rgba(0, 255, 128, 0.1);
  border: 1px solid rgba(0, 255, 128, 0.25);
  color: #00ff80;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}

.submit-btn-guest:hover:not(:disabled) {
  background: rgba(0, 255, 128, 0.18);
  border-color: rgba(0, 255, 128, 0.4);
}

.submit-btn-guest:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.spinner-guest {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: #00ff80;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.guest-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
  gap: 12px;
}

.guest-error-state {
  text-align: center;
  padding: 60px 0;
}

.guest-error-state p {
  color: rgba(255, 255, 255, 0.35);
  font-size: 13px;
  margin-bottom: 14px;
}

.retry-btn {
  padding: 8px 20px;
  border: 1px solid rgba(0, 255, 128, 0.3);
  border-radius: 8px;
  background: transparent;
  color: #00ff80;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-btn:hover {
  background: rgba(0, 255, 128, 0.1);
}

.guest-empty {
  text-align: center;
  padding: 60px 0;
  color: rgba(255, 255, 255, 0.25);
  font-size: 14px;
}

.guest-empty .empty-icon {
  font-size: 36px;
  display: block;
  margin-bottom: 12px;
}

.guest-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.guest-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.25s ease;
}

.guest-card:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.16);
}

.guest-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.guest-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 255, 128, 0.2), rgba(78, 205, 196, 0.2));
  border: 1px solid rgba(0, 255, 128, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #00ff80;
  flex-shrink: 0;
}

.guest-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
  min-width: 0;
}

.guest-name {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.guest-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.guest-delete {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.2);
  font-size: 16px;
  cursor: pointer;
  padding: 4px 10px;
  transition: color 0.2s;
}

.guest-delete:hover {
  color: #ff4444;
}

.guest-text {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.85;
  margin: 0;
  word-break: break-word;
  white-space: pre-wrap;
}

.submit-success {
  text-align: center;
  padding: 24px 0;
}

.submit-success .success-icon {
  font-size: 36px;
  display: block;
  margin-bottom: 12px;
}

.submit-success p {
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
  margin: 0 0 16px;
}

.another-msg {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.6);
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.another-msg:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

/*
 * ======================== 8a. 弹窗遮罩层 ========================
 * position: fixed → 固定在视口，不随页面滚动
 * inset: 0 → 覆盖整个视口
 * background-color: rgba(0, 0, 0, 0.75) → 半透明黑色遮罩
 * backdrop-filter: blur(4px) → 背景模糊效果（毛玻璃）
 * animation: fadeIn 0.2s → 淡入动画
 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

/*
 * ======================== 8b. 弹窗内容卡片 ========================
 * border: 1px solid rgba(0, 255, 128, 0.25) → 绿色半透明边框
 * border-radius: 14px → 大圆角卡片
 * box-shadow: 0 0 40px → 绿色发光效果
 * animation: scaleIn 0.25s → 缩放入场动画
 */
.modal-content {
  background-color: #141414;
  border: 1px solid rgba(0, 255, 128, 0.25);
  border-radius: 14px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  box-shadow: 0 0 40px rgba(0, 255, 128, 0.15);
  animation: scaleIn 0.25s ease;
}

/* 弹窗头部：标题 + 关闭按钮 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.modal-header h2 {
  color: #00ff80;
  font-size: 17px;
  margin: 0;
  font-weight: 500;
  letter-spacing: 1px;
}

/* 关闭按钮：白色半透明，悬停变红 */
.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
  padding: 0;
}

.close-btn:hover {
  color: #ff4444;
}

.modal-body {
  padding: 24px;
}

.modal-desc {
  color: rgba(255, 255, 255, 0.55);
  font-size: 14px;
  margin: 0 0 18px 0;
}

.input-group {
  margin-bottom: 12px;
}

/*
 * ======================== 8c. 密码输入框 ========================
 * 深色输入框 + 白色文字
 * :focus 时边框变为绿色，提供视觉反馈
 * :disabled 时降低透明度
 * 密码通过 HTTPS 加密传输到 Spring Boot 后端
 */
.password-input {
  width: 100%;
  padding: 12px 16px;
  background-color: #0f0f0f;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.password-input:focus {
  border-color: #00ff80;
}

.password-input:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.password-input::placeholder {
  color: rgba(255, 255, 255, 0.25);
}

/* 成功/错误/次数提示 */
.success-msg {
  color: #00ff80;
  font-size: 13px;
  margin: 10px 0 0 0;
}

.error-msg {
  color: #ff4444;
  font-size: 13px;
  margin: 10px 0 0 0;
}

.attempts-msg {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  margin: 10px 0 0 0;
}

/* 弹窗底部按钮容器 */
.modal-footer {
  display: flex;
  gap: 12px;
  padding: 18px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

/* 取消按钮：透明背景 + 白色边框 */
.cancel-btn,
.submit-btn {
  flex: 1;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}

.cancel-btn {
  background-color: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.6);
}

.cancel-btn:hover {
  border-color: rgba(255, 255, 255, 0.4);
  color: #fff;
}

/* 验证按钮：绿色背景 + 黑色文字 */
.submit-btn {
  background-color: #00ff80;
  border: none;
  color: #0a0a0a;
  font-weight: 600;
}

.submit-btn:hover:not(:disabled) {
  background-color: #00e070;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/*
 * ======================== 加载动画（旋转小圆圈） ========================
 * border: 2px solid transparent → 透明边框
 * border-top-color: #0a0a0a → 仅顶部有颜色（深色，与绿色按钮底色对比）
 * animation: spin 0.6s linear infinite → 匀速旋转
 */
.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: #0a0a0a;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/*
 * ======================== 9. 关键帧动画 ========================
 */

/* 光标闪烁动画：前40%显示，后60%隐藏 */
@keyframes blink {
  0%, 40% { opacity: 1; }
  41%, 100% { opacity: 0; }
}

/* 弹窗遮罩淡入 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 弹窗卡片缩放 + 淡入 */
@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 加载按钮的旋转动画 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/*
 * ======================== 10. 响应式适配 ========================
 */

/* 手机/平板适配（屏幕宽度 ≤ 768px） */
@media (max-width: 768px) {
  .content-wrapper.show { transform: translateY(0); }
  .avatar { width: 120px; height: 120px; }
  .avatar-glow { width: 200px; height: 200px; }
  .nickname { font-size: 20px; }
  .typewriter-text { font-size: 20px; }
  .subtitle { font-size: 14px; }
  .modal-content { max-width: 90%; }
  .guestbook-section { padding: 0 16px 60px; }
  .guestbook-title { font-size: 20px; }
}

/* 大屏适配（屏幕宽度 ≥ 1200px） */
@media (min-width: 1200px) {
  .avatar { width: 150px; height: 150px; }
  .avatar-glow { width: 280px; height: 280px; }
  .typewriter-text { font-size: 24px; }
  .subtitle { font-size: 16px; }
}
</style>