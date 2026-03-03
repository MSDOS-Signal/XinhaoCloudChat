import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') !== 'light')

  function toggleTheme() {
    isDark.value = !isDark.value
  }

  function applyTheme() {
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      document.documentElement.classList.remove('light')
    } else {
      document.documentElement.classList.remove('dark')
      document.documentElement.classList.add('light')
    }
  }

  // Watch for changes
  watch(isDark, () => {
    applyTheme()
  })

  // Initialize
  applyTheme()

  return { isDark, toggleTheme }
})
