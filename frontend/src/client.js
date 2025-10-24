import axios from 'axios'

const api = axios.create({
  baseURL: '/api' // proxied by Vite to http://localhost:8080
})

export default api
