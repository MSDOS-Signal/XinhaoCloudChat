<template>
  <canvas ref="canvas" class="matrix-rain"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvas = ref(null)
let ctx = null
let width = 0
let height = 0
let columns = 0
let drops = []
let intervalId = null

const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%^&*()_+'

const resize = () => {
  if (!canvas.value) return
  width = window.innerWidth
  height = window.innerHeight
  canvas.value.width = width
  canvas.value.height = height
  
  columns = Math.floor(width / 20)
  drops = []
  for (let i = 0; i < columns; i++) {
    drops[i] = Math.random() * -100 // Random start delay
  }
}

const draw = () => {
  if (!ctx) return
  
  // Semi-transparent black to create trail effect
  ctx.fillStyle = 'rgba(0, 0, 0, 0.05)'
  ctx.fillRect(0, 0, width, height)
  
  ctx.fillStyle = '#0F0' // Green text
  ctx.font = '15px monospace'
  
  for (let i = 0; i < drops.length; i++) {
    const text = characters.charAt(Math.floor(Math.random() * characters.length))
    ctx.fillText(text, i * 20, drops[i] * 20)
    
    if (drops[i] * 20 > height && Math.random() > 0.975) {
      drops[i] = 0
    }
    drops[i]++
  }
}

onMounted(() => {
  if (canvas.value) {
    ctx = canvas.value.getContext('2d')
    resize()
    window.addEventListener('resize', resize)
    intervalId = setInterval(draw, 50)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  if (intervalId) clearInterval(intervalId)
})
</script>

<style scoped>
.matrix-rain {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  background-color: black;
  pointer-events: none;
}
</style>
