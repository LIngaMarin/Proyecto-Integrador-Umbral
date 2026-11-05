import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://127.0.0.1:8000/api',
})

export const CLAVES = {
  token: 'portal_token',
  nombre: 'portal_nombre',
  cambiar: 'portal_cambiar',
}

export const cerrarSesionLocal = () => {
  Object.values(CLAVES).forEach((clave) => localStorage.removeItem(clave))
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(CLAVES.token)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Si el token venció, se limpia la sesión y se vuelve al ingreso.
api.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem(CLAVES.token)) {
      cerrarSesionLocal()
      window.location.reload()
    }
    return Promise.reject(error)
  },
)
