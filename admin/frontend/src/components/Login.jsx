import { useState } from 'react'
import { api } from '../api'
import './Login.css'

const ROLES_PERMITIDOS = ['ADMIN', 'COORDINADOR']

export default function Login({ onLogin }) {
  const [correo, setCorreo] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const entrar = async (e) => {
    e.preventDefault()
    setError('')
    setCargando(true)
    try {
      const { data } = await api.post('/auth/login/', {
        username: correo,
        password: clave,
      })
      if (!ROLES_PERMITIDOS.includes(data.rol)) {
        setError('Este panel es solo para administradores y coordinadores.')
        return
      }
      localStorage.setItem('umbral_token', data.access)
      localStorage.setItem('umbral_nombre', data.nombre)
      localStorage.setItem('umbral_rol', data.rol)
      localStorage.setItem('umbral_cambiar', data.debe_cambiar_clave ? '1' : '')
      onLogin({ nombre: data.nombre, rol: data.rol, cambiar: !!data.debe_cambiar_clave })
    } catch (err) {
      setError(
        err.response?.status === 401
          ? 'Correo o contraseña incorrectos.'
          : 'No se pudo conectar con el servidor.',
      )
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="login">
      <form className="panel login-caja" onSubmit={entrar}>
        <div className="login-marca">
          <strong>Umbral</strong>
        </div>
        <h1>Ingresa a tu cuenta</h1>
        <p>Panel de administración de tu academia</p>
        <label>
          <span>Correo</span>
          <input
            type="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            required
          />
        </label>
        <label>
          <span>Contraseña</span>
          <input
            type="password"
            value={clave}
            onChange={(e) => setClave(e.target.value)}
            required
          />
        </label>
        {error && <p className="errores-simple">{error}</p>}
        <button type="submit" disabled={cargando}>
          {cargando ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </div>
  )
}
