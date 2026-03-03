<template>
  <div class="auth-container">
    <TechBackground />
    <div class="theme-toggle" @click="themeStore.toggleTheme">
        <el-icon v-if="themeStore.isDark"><Moon /></el-icon>
        <el-icon v-else><Sunny /></el-icon>
    </div>
    
    <div class="auth-box">
      <div class="header">
        <h1 class="app-title">炘灏云聊</h1>
        <h2 class="app-subtitle">Xinhao Chat</h2>
        <p class="subtitle">登录到您的账户</p>
      </div>
      <el-form :model="form" :rules="rules" ref="loginForm" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="w-100 auth-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        <div class="footer">
          <p>还没有账号? <router-link to="/register">立即注册</router-link></p>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useThemeStore } from '../store/theme'
import { login } from '../api/auth'
import { ElMessage } from 'element-plus'
import { Moon, Sunny } from '@element-plus/icons-vue'
import TechBackground from '../components/TechBackground.vue'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const loginForm = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginForm.value) return
  await loginForm.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const { data } = await login(form)
        userStore.login(data)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        ElMessage.error(error.response?.data || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: var(--bg-primary);
  transition: background-color 0.3s;
  position: relative;
  overflow: hidden;
}

.theme-toggle {
    position: absolute;
    top: 20px;
    right: 20px;
    cursor: pointer;
    font-size: 24px;
    color: var(--text-primary);
    padding: 8px;
    border-radius: 50%;
    transition: background-color 0.2s;
    z-index: 20;
}

.theme-toggle:hover {
    background-color: var(--bg-secondary);
}

.auth-box {
  /* background: rgba(var(--bg-secondary-rgb), 0.8);  Removed for new effect */
  background: var(--bg-secondary);
  padding: 40px;
  border-radius: 12px;
  /* border: 1px solid var(--border-color); Removed for new border effect */
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 20px var(--shadow-color);
  z-index: 10;
  position: relative;
  overflow: hidden;
}

/* Rotating Border Effect */
.auth-box::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: conic-gradient(transparent, transparent, transparent, #00ff80);
    animation: rotate 4s linear infinite;
    z-index: -2;
}

.auth-box::after {
    content: '';
    position: absolute;
    inset: 3px; 
    background: var(--bg-secondary);
    border-radius: 10px;
    z-index: -1;
}

@keyframes rotate {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

/* Dark mode specific override */
:root.dark .auth-box::after {
    background-color: rgba(9, 9, 11, 0.9);
}

:root:not(.dark) .auth-box::after {
    background-color: rgba(255, 255, 255, 0.9);
}

.header {
  text-align: center;
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.app-title {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    color: var(--accent-color);
    text-shadow: 0 0 10px rgba(16, 163, 127, 0.5);
    letter-spacing: 2px;
}

.app-subtitle {
  margin: 5px 0 0;
  color: var(--text-primary);
  font-weight: 600;
  font-size: 20px;
  letter-spacing: 1px;
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.2);
}

.subtitle {
  color: var(--text-secondary);
  margin-top: 15px;
  font-size: 14px;
}

.w-100 {
  width: 100%;
  margin-top: 20px;
}

.auth-btn {
    background-color: var(--accent-color);
    border: none;
    font-weight: 500;
    padding: 20px 0;
}

.auth-btn:hover {
    background-color: var(--accent-hover);
}

.footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-secondary);
}

.footer a {
  color: var(--accent-color);
  text-decoration: none;
  font-weight: 500;
}

.footer a:hover {
  text-decoration: underline;
}

/* Custom Element Plus Overrides */
:deep(.el-form-item__label) {
  color: var(--text-primary) !important;
}

:deep(.el-input__wrapper) {
  background-color: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--accent-color) inset !important;
}

:deep(.el-input__inner) {
  color: var(--text-primary) !important;
}
</style>
