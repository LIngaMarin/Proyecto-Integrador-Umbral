import { useEffect, useState } from 'react'
import { api } from '../api'

const fechaCorta = (texto) =>
  new Date(`${texto}T12:00:00`).toLocaleDateString('es-PE', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })

const TENDENCIAS = {
  sube: { clase: 'sube', texto: '▲ El último simulacro superó al anterior.' },
  baja: { clase: 'baja', texto: '▼ El último simulacro quedó por debajo del anterior.' },
  estable: { clase: 'igual', texto: 'Igual que el simulacro anterior.' },
  sin_datos: {
    clase: 'igual',
    texto: 'Hacen falta al menos dos simulacros en el período para ver la tendencia.',
  },
}

const mensajeDeError = (e) => {
  if (e.response?.status === 404) return 'No encontramos a ese alumno entre tus hijos.'
  return 'No se pudo cargar el reporte.'
}

const TarjetaResumen = ({ etiqueta, valor }) => (
  <div className="tarjeta" style={{ padding: '14px 16px' }}>
    <span className="suave" style={{ fontSize: '0.85rem' }}>
      {etiqueta}
    </span>
    <strong style={{ display: 'block', fontSize: '1.5rem' }}>{valor}</strong>
  </div>
)

export default function Reporte({ hijos }) {
  const [elegido, setElegido] = useState(hijos[0].id)
  const hijo = hijos.find((h) => h.id === elegido) ?? hijos[0]
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let vigente = true
    api
      .get(`/mis-hijos/${hijo.id}/reporte/`)
      .then((r) => {
        if (!vigente) return
        setDatos(r.data)
        setError('')
      })
      .catch((e) => vigente && setError(mensajeDeError(e)))
    return () => {
      vigente = false
    }
  }, [hijo.id])

  const vigente = datos && datos.alumno.id === hijo.id ? datos : null
  const tendencia = vigente ? TENDENCIAS[vigente.tendencia] ?? TENDENCIAS.sin_datos : null

  return (
    <section className="tarjeta resultados" aria-label={`Reporte de ${hijo.first_name}`}>
      <h2>Reporte de {hijo.first_name}</h2>

      {hijos.length > 1 && (
        <div className="hijos" role="tablist" aria-label="Tus hijos">
          {hijos.map((h) => (
            <button
              key={h.id}
              type="button"
              role="tab"
              aria-selected={h.id === hijo.id}
              className={h.id === hijo.id ? 'chip activo' : 'chip'}
              onClick={() => setElegido(h.id)}
            >
              {h.first_name} {h.last_name}
            </button>
          ))}
        </div>
      )}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!vigente && !error && <p className="suave">Cargando reporte...</p>}

      {vigente && (
        <>
          <p className="suave">
            Período del {fechaCorta(vigente.periodo.desde)} al {fechaCorta(vigente.periodo.hasta)}.
          </p>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
              gap: 10,
            }}
          >
            <TarjetaResumen
              etiqueta="Asistencia"
              valor={vigente.asistencia.porcentaje !== null ? `${vigente.asistencia.porcentaje} %` : '—'}
            />
            <TarjetaResumen etiqueta="Tardanzas" valor={vigente.asistencia.tardanzas} />
            <TarjetaResumen etiqueta="Faltas" valor={vigente.asistencia.faltas} />
            <TarjetaResumen
              etiqueta="Promedio de simulacros"
              valor={vigente.promedio !== null ? vigente.promedio : '—'}
            />
          </div>

          <p className={`variacion ${tendencia.clase}`}>
            <strong>Tendencia:</strong> {tendencia.texto}
          </p>

          {vigente.simulacros.length === 0 ? (
            <p className="vacio">No hubo simulacros en este período.</p>
          ) : (
            <ul className="lista-simulacros">
              {vigente.simulacros.map((s) => (
                <li key={`${s.fecha}-${s.nombre}`}>
                  <span>
                    {s.nombre}
                    <small>{fechaCorta(s.fecha)}</small>
                  </span>
                  <strong className="nota">{s.puntaje}</strong>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
