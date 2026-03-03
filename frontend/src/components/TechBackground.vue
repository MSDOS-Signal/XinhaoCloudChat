<template>
  <canvas ref="canvas" class="tech-background"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useThemeStore } from '../store/theme'

const canvas = ref(null)
const themeStore = useThemeStore()
let ctx = null
let animationFrameId = null
let width = 0
let height = 0

// Configuration
const particleCount = 80
const connectionDistance = 150
const mouseDistance = 250

let mouse = { x: null, y: null }
let particles = []
let packets = [] 

class Particle {
  constructor() {
    this.x = Math.random() * width
    this.y = Math.random() * height
    this.vx = (Math.random() - 0.5) * 0.5 
    this.vy = (Math.random() - 0.5) * 0.5
    this.size = Math.random() * 2 + 1.5
    this.pulse = Math.random() * Math.PI
  }

  update() {
    this.x += this.vx
    this.y += this.vy

    if (this.x < 0 || this.x > width) this.vx *= -1
    if (this.y < 0 || this.y > height) this.vy *= -1

    if (mouse.x != null) {
      let dx = mouse.x - this.x
      let dy = mouse.y - this.y
      let distance = Math.sqrt(dx * dx + dy * dy)
      if (distance < mouseDistance) {
        const forceDirectionX = dx / distance
        const forceDirectionY = dy / distance
        const force = (mouseDistance - distance) / mouseDistance
        const directionX = forceDirectionX * force * 0.5
        const directionY = forceDirectionY * force * 0.5
        this.vx -= directionX * 0.05 
        this.vy -= directionY * 0.05
      }
    }
    
    this.pulse += 0.05
    
    if (Math.abs(this.vx) > 1) this.vx *= 0.95
    if (Math.abs(this.vy) > 1) this.vy *= 0.95
  }

  draw() {
    // Brighter green for dark mode
    const baseColor = themeStore.isDark ? '0, 255, 128' : '0, 0, 0' 
    const alpha = 0.6 + Math.sin(this.pulse) * 0.3
    
    ctx.fillStyle = `rgba(${baseColor}, ${alpha})`
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

class Packet {
  constructor(startNode, endNode) {
    this.startNode = startNode
    this.endNode = endNode
    this.progress = 0
    this.speed = 0.02 + Math.random() * 0.03
    this.active = true
  }

  update() {
    this.progress += this.speed
    if (this.progress >= 1) {
      this.active = false
    }
  }

  draw() {
    if (!this.active) return
    const x = this.startNode.x + (this.endNode.x - this.startNode.x) * this.progress
    const y = this.startNode.y + (this.endNode.y - this.startNode.y) * this.progress
    
    // Bright green packets
    const color = themeStore.isDark ? '#00ff80' : '#10a37f' 
    
    ctx.fillStyle = color
    ctx.shadowBlur = 8
    ctx.shadowColor = color
    ctx.beginPath()
    ctx.arc(x, y, 2.5, 0, Math.PI * 2)
    ctx.fill()
    ctx.shadowBlur = 0
  }
}

const init = () => {
  if (!canvas.value) return
  resize()
  particles = []
  packets = []
  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle())
  }
}

const resize = () => {
  if (!canvas.value) return
  width = window.innerWidth
  height = window.innerHeight
  canvas.value.width = width
  canvas.value.height = height
}

const animate = () => {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)
  
  // Brighter green for connections
  const baseColor = themeStore.isDark ? '0, 255, 128' : '0, 0, 0'

  for (let i = 0; i < particles.length; i++) {
    particles[i].update()
    particles[i].draw()
  }

  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      let dx = particles[i].x - particles[j].x
      let dy = particles[i].y - particles[j].y
      let distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < connectionDistance) {
        const opacity = 1 - (distance / connectionDistance)
        // Increased opacity multiplier from 0.15 to 0.4 for better visibility
        ctx.strokeStyle = `rgba(${baseColor}, ${opacity * 0.4})` 
        ctx.lineWidth = 1.2
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()

        if (Math.random() < 0.002) {
          packets.push(new Packet(particles[i], particles[j]))
        }
        if (Math.random() < 0.002) {
          packets.push(new Packet(particles[j], particles[i]))
        }
      }
    }
  }

  for (let i = packets.length - 1; i >= 0; i--) {
    packets[i].update()
    packets[i].draw()
    if (!packets[i].active) {
      packets.splice(i, 1)
    }
  }
  
  animationFrameId = requestAnimationFrame(animate)
}

const handleMouseMove = (e) => {
  mouse.x = e.x
  mouse.y = e.y
}

const handleMouseLeave = () => {
  mouse.x = null
  mouse.y = null
}

onMounted(() => {
  if (canvas.value) {
    ctx = canvas.value.getContext('2d')
    init()
    animate()
    window.addEventListener('resize', resize)
    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseleave', handleMouseLeave)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseleave', handleMouseLeave)
  cancelAnimationFrame(animationFrameId)
})

watch(() => themeStore.isDark, () => {
  // Animation loop picks up the change
})
</script>

<style scoped>
.tech-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  background: transparent;
}
</style>
