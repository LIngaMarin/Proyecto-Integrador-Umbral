import { useState } from 'react'
import { api } from '../api'

export default function CambiarClave({ onListo, onSalir }) {
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e) => {
    e.preventDefault()
    setError('')
    if (nueva.length < 8) {
      setError('La contraseña nueva debe tener al menos 8 caracteres.')
      return
    }
    if (nueva !== confirmar) {
      setError('La confirmación no coincide con la contraseña nueva.')
      return
    }
    setGuardando(true)
    try {
      await api.post('/auth/cambiar-clave/', { clave_actual: actual, clave_nueva: nueva })
      onListo()
    } catch (err) {
      const datos = err.response?.data
      setError(
        datos?.clave_actual
          ? 'La contraseña temporal es incorrecta.'
          : datos?.clave_nueva
            ? 'La contraseña nueva no es válida.'
            : 'No se pudo cambiar la contraseña.',
      )
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="acceso">
      <form className="tarjeta acceso-caja" onSubmit={guardar}>
        <strong className="acceso-marca">Umbral</strong>
        <h1>Crea tu contraseña</h1>
        <p>Ingresaste con una contraseña temporal. Elige una nueva para continuar.</p>
        <label>
          <span>Contraseña temporal</span>
          <input
            type="password"
            value={actual}
            onChange={(e) => setActual(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        <label>
          <span>Contraseña nueva</span>
          <input
            type="password"
            value={nueva}
            onChange={(e) => setNueva(e.target.value)}
            autoComplete="new-password"
            required
          />
        </label>
        <label>
          <span>Repite la contraseña nueva</span>
          <input
            type="password"
            value={confirmar}
            onChange={(e) => setConfirmar(e.target.value)}
            autoComplete="new-password"
            required
          />
        </label>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={guardando}>
          {guardando ? 'Guardando...' : 'Guardar contraseña'}
        </button>
        <button type="button" className="secundario" onClick={onSalir}>
          Salir
        </button>
      </form>
    </div>
  )
}
