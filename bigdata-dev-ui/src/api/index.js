import axios from 'axios'

const api = axios.create({
  // baseURL: 'http://localhost:12408/api',
      baseURL: 'http://backend:12408/api',

  timeout: 30000
})

api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    return Promise.reject(error)
  }
)

export default api
