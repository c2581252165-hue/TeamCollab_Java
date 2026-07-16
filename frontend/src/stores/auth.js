import { defineStore } from 'pinia'
import axios from 'axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user')) || null,
  }),
  getters: {
    rpgLevel: (state) => {
      if (!state.user) return 1
      const p = state.user.points || 0
      if (p < 100) return 1
      if (p < 300) return 2
      if (p < 600) return 3
      if (p < 1000) return 4
      return 5
    },
    rpgTitle: (state) => {
      if (!state.user) return '访客'
      const p = state.user.points || 0
      if (p < 100) return '搬砖实习生'
      if (p < 300) return '初级开发'
      if (p < 600) return '高级开发'
      if (p < 1000) return '资深专家'
      return '首席架构师'
    }
  },
  actions: {
    async login(username, password) {
      const formData = new FormData()
      formData.append('username', username)
      formData.append('password', password)
      
      const response = await axios.post('http://127.0.0.1:8000/api/auth/login', formData)
      
      this.token = response.data.access_token
      this.user = response.data.user
      
      localStorage.setItem('token', this.token)
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
