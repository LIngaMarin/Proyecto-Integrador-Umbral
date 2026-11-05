import { useState } from 'react'
import { api, CLAVES } from '../api'

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
      const { data } = await api.post('/auth/login/', { username: correo, password: clave })
      if (data.rol !== 'APODERADO') {
        setError('Esta cuenta no es de apoderado. Si administras la academia, usa el panel de administración.')
        return
      }
      localStorage.setItem(CLAVES.token, data.access)
      localStorage.setItem(CLAVES.nombre, data.nombre)
      localStorage.setItem(CLAVES.cambiar, data.debe_cambiar_clave ? '1' : '')
      onLogin({ nombre: data.nombre, cambiar: !!data.debe_cambiar_clave })
    } catch (err) {
      setError(
        err.response?.status === 401
          ? 'Correo o contraseña incorrectos.'
          : 'No se pudo conectar con el servidor. Inténtalo de nuevo en unos minutos.',
      )
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="acceso">
      <form className="tarjeta acceso-caja" onSubmit={entrar}>
        <strong className="acceso-marca">Umbral</strong>
        <h1>Portal del apoderado</h1>
        <p>Consulta la asistencia y el avance de tus hijos.</p>
        <label>
          <span>Correo</span>
          <input
            type="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            autoComplete="username"
            required
          />
        </label>
        <label>
          <span>Contraseña</span>
          <input
            type="password"
            value={clave}
            onChange={(e) => setClave(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={cargando}>
          {cargando ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </div>
  )
}
