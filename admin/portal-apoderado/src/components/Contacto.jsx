import { useEffect, useState } from 'react'
import { api } from '../api'

const mensajes = (datos) =>
  Object.values(datos ?? {})
    .flat()
    .join(' ')

export default function Contacto() {
  const [email, setEmail] = useState('')
  const [telefono, setTelefono] = useState('')
  const [original, setOriginal] = useState(null)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    api
      .get('/mis-datos/contacto/')
      .then((r) => {
        setEmail(r.data.email)
        setTelefono(r.data.telefono)
        setOriginal(r.data)
      })
      .catch(() => setError('No se pudieron cargar tus datos de contacto.'))
  }, [])

  const guardar = async (e) => {
    e.preventDefault()
    setError('')
    setAviso('')
    setGuardando(true)
    try {
      const r = await api.put('/mis-datos/contacto/', { email, telefono })
      setEmail(r.data.email)
      setTelefono(r.data.telefono)
      setOriginal(r.data)
      setAviso('Tus datos de contacto se guardaron.')
    } catch (err) {
      setError(mensajes(err.response?.data) || 'No se pudieron guardar tus datos.')
    } finally {
      setGuardando(false)
    }
  }

  const sinCambios = original && original.email === email && original.telefono === telefono

  return (
    <form className="tarjeta contacto" onSubmit={guardar}>
      <h2>Tus datos de contacto</h2>
      <p className="suave">La academia usa estos datos para comunicarse contigo.</p>
      <label>
        <span>Correo</span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
          required
        />
      </label>
      <label>
        <span>Teléfono</span>
        <input
          type="tel"
          value={telefono}
          onChange={(e) => setTelefono(e.target.value)}
          autoComplete="tel"
        />
      </label>
      <p className="suave">Tu correo también es tu usuario para ingresar al portal.</p>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {aviso && (
        <p className="exito" role="status">
          {aviso}
        </p>
      )}
      <button type="submit" disabled={guardando || sinCambios}>
        {guardando ? 'Guardando...' : 'Guardar cambios'}
      </button>
    </form>
  )
}
