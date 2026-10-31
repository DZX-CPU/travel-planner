import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000 // AI生成行程比较慢，超时给长一点
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  res => {
    // 后端返回的是 {code, message, data} 结构
    if (res.data.code !== 0) {
      if (res.data.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      ElMessage.error(res.data.message || '请求出错')
      return Promise.reject(new Error(res.data.message))
    }
    return res.data.data
  },
  err => {
    ElMessage.error(err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
