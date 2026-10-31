import { defineStore } from 'pinia'
import request from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),
  actions: {
    async login(username, password) {
      const data = await request.post('/user/login', { username, password })
      this.token = data.token
      this.userInfo = data.user
      localStorage.setItem('token', data.token)
    },
    async fetchInfo() {
      if (!this.token) return
      try {
        this.userInfo = await request.get('/user/info')
      } catch (e) {
        // token失效的情况
        this.logout()
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
