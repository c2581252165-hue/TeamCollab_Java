import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const api = axios.create({
  baseURL: '/api'
})

api.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

api.interceptors.response.use((response) => {
  if (response.data && response.data.code === 200 && response.data.data !== undefined) {
    response.data = response.data.data;
  }
  return response;
}, (error) => {
  return Promise.reject(error);
})

export default api
