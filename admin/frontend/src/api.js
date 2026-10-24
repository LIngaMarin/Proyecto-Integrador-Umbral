import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('umbral_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem('umbral_token')) {
      localStorage.removeItem('umbral_token')
      localStorage.removeItem('umbral_nombre')
      localStorage.removeItem('umbral_rol')
      window.location.reload()
    }
    return Promise.reject(error)
  },
)
