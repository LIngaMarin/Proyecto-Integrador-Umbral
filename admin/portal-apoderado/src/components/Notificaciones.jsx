import { useEffect, useState } from 'react'
import { api } from '../api'

const fechaHora = (texto) =>
  new Date(texto).toLocaleString('es-PE', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })

const nombreHijo = (alumno) =>
  `${alumno.first_name} ${alumno.last_name}`.trim() || alumno.email

export default function Notificaciones() {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let vigente = true
    api
      .get('/mis-notificaciones/')
      .then((r) => vigente && setDatos(r.data))
      .catch(() => vigente && setError('No se pudieron cargar tus notificaciones.'))
    return () => {
      vigente = false
    }
  }, [])

  const abrir = (notificacion) => {
    if (notificacion.leida) return
    api
      .post(`/mis-notificaciones/${notificacion.id}/leer/`)
      .then((r) =>
        setDatos((previo) => ({
          sin_leer: Math.max(0, previo.sin_leer - 1),
          notificaciones: previo.notificaciones.map((n) => (n.id === r.data.id ? r.data : n)),
        })),
      )
      .catch(() => setError('No se pudo marcar la notificación como leída.'))
  }

  return (
    <section className="tarjeta resultados" aria-label="Notificaciones">
      <h2>Notificaciones</h2>
      {datos && datos.sin_leer > 0 && (
        <p className="suave">Tienes {datos.sin_leer} sin leer.</p>
      )}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!datos && !error && <p className="suave">Cargando notificaciones...</p>}
      {datos && datos.notificaciones.length === 0 && (
        <p className="vacio">
          No tienes notificaciones. Cuando un hijo falte a clase, te avisaremos aquí.
        </p>
      )}

      {datos && datos.notificaciones.length > 0 && (
        <ul className="notif-lista">
          {datos.notificaciones.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                className={n.leida ? 'notif-item' : 'notif-item nueva'}
                onClick={() => abrir(n)}
                aria-label={n.leida ? n.mensaje : `Sin leer: ${n.mensaje}`}
              >
                <span className="notif-cabecera">
                  <strong>{nombreHijo(n.alumno)}</strong>
                  {!n.leida && <span className="notif-etiqueta">Nueva</span>}
                </span>
                <span>{n.mensaje}</span>
                <small>
                  {n.tipo_texto} · {fechaHora(n.fecha_creacion)}
                </small>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
