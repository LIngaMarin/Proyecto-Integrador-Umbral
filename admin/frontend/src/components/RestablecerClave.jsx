import { useEffect, useState } from 'react'
import { api } from '../api'

export default function RestablecerClave() {
  const [usuarios, setUsuarios] = useState([])
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(null)

  useEffect(() => {
    api
      .get('/usuarios/')
      .then((r) => setUsuarios(r.data.results || r.data))
      .catch(() => setError('No se pudo cargar la lista de usuarios.'))
  }, [])

  const restablecer = async (u) => {
    if (!window.confirm(`¿Restablecer la contraseña de ${u.email}? Se le enviará una clave temporal por correo.`)) {
      return
    }
    setMensaje('')
    setError('')
    setEnviando(u.id)
    try {
      await api.post(`/usuarios/${u.id}/restablecer-clave/`)
      setMensaje(`Se envió una contraseña temporal a ${u.email}.`)
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo restablecer la contraseña.')
    } finally {
      setEnviando(null)
    }
  }

  return (
    <section className="panel">
      <h2>Contraseñas de usuarios</h2>
      <p>
        Genera una contraseña temporal y la envía por correo. El usuario deberá
        cambiarla al ingresar.
      </p>
      {mensaje && <p className="ok">{mensaje}</p>}
      {error && <p className="aviso">{error}</p>}
      <table className="tabla-claves">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Rol</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id}>
              <td>{`${u.first_name} ${u.last_name}`}</td>
              <td>{u.email}</td>
              <td>{u.rol}</td>
              <td>
                <button disabled={enviando === u.id} onClick={() => restablecer(u)}>
                  {enviando === u.id ? 'Enviando...' : 'Restablecer contraseña'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
