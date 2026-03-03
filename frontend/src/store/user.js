import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user')) || null)
  const token = ref(localStorage.getItem('token') || null)

  const isAuthenticated = computed(() => !!user.value && !!token.value)

  function login(data) {
    // Check if data has user and token (from LoginResponse) or just user
    if (data.token) {
        token.value = data.token
        localStorage.setItem('token', data.token)
        user.value = data.user
        localStorage.setItem('user', JSON.stringify(data.user))
    } else {
        // Fallback for direct user object updates
        user.value = data
        localStorage.setItem('user', JSON.stringify(data))
    }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  return { user, token, isAuthenticated, login, logout }
})
