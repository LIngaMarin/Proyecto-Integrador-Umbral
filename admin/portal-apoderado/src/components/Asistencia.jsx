import { useEffect, useState } from 'react'
import { api } from '../api'

const ZONA = 'America/Lima'

// Fecha de hoy en Lima como AAAA-MM-DD, sin depender de la zona del navegador.
const hoyLima = () => new Date().toLocaleDateString('en-CA', { timeZone: ZONA })

const sumarDias = (texto, dias) => {
  const fecha = new Date(`${texto}T12:00:00`)
  fecha.setDate(fecha.getDate() + dias)
  return fecha.toLocaleDateString('en-CA')
}

const PERIODOS = [
  { clave: '7', etiqueta: 'Últimos 7 días', rango: (hoy) => [sumarDias(hoy, -6), hoy] },
  { clave: '30', etiqueta: 'Últimos 30 días', rango: (hoy) => [sumarDias(hoy, -29), hoy] },
  { clave: 'mes', etiqueta: 'Este mes', rango: (hoy) => [`${hoy.slice(0, 8)}01`, hoy] },
]

const nombreDia = (texto) =>
  new Date(`${texto}T12:00:00`).toLocaleDateString('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  })

const mensajeDeError = (e) => {
  if (e.response?.status === 404) return 'No encontramos a ese alumno entre tus hijos.'
  const datos = e.response?.data
  if (datos && typeof datos === 'object') {
    const primero = Object.values(datos)[0]
    if (typeof primero === 'string') return primero
  }
  return 'No se pudo cargar la asistencia.'
}

export default function Asistencia({ hijo }) {
  const hoy = hoyLima()
  const [periodo, setPeriodo] = useState('30')
  const [desde, setDesde] = useState(() => PERIODOS[1].rango(hoy)[0])
  const [hasta, setHasta] = useState(hoy)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  const consulta = `/mis-hijos/${hijo.id}/asistencia/?desde=${desde}&hasta=${hasta}`

  useEffect(() => {
    api
      .get(consulta)
      .then((r) => {
        setDatos({ consulta, ...r.data })
        setError('')
      })
      .catch((e) => setError(mensajeDeError(e)))
  }, [consulta])

  const elegirPeriodo = (p) => {
    const [d, h] = p.rango(hoyLima())
    setPeriodo(p.clave)
    setDesde(d)
    setHasta(h)
  }

  const cambiarFecha = (poner) => (e) => {
    poner(e.target.value)
    setPeriodo('')
  }

  const rangoValido = desde && hasta && desde <= hasta
  const vigente = datos && datos.consulta === consulta ? datos : null
  const resumen = vigente?.resumen

  return (
    <section className="tarjeta asistencia" aria-label={`Asistencia de ${hijo.first_name}`}>
      <h2>Asistencia de {hijo.first_name}</h2>

      <div className="periodos" role="group" aria-label="Período">
        {PERIODOS.map((p) => (
          <button
            key={p.clave}
            type="button"
            className={p.clave === periodo ? 'chip activo' : 'chip'}
            aria-pressed={p.clave === periodo}
            onClick={() => elegirPeriodo(p)}
          >
            {p.etiqueta}
          </button>
        ))}
      </div>

      <div className="fechas">
        <label>
          <span>Desde</span>
          <input type="date" value={desde} max={hasta || undefined} onChange={cambiarFecha(setDesde)} />
        </label>
        <label>
          <span>Hasta</span>
          <input type="date" value={hasta} min={desde || undefined} onChange={cambiarFecha(setHasta)} />
        </label>
      </div>

      {!rangoValido && (
        <p className="aviso">Elige una fecha inicial y una final, y que la inicial no sea posterior.</p>
      )}
      {rangoValido && error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {rangoValido && !vigente && !error && <p className="suave">Cargando asistencia...</p>}

      {rangoValido && vigente && resumen.total === 0 && (
        <p className="vacio">
          No hay asistencias registradas en este período. Prueba con otro rango de fechas.
        </p>
      )}

      {rangoValido && vigente && resumen.total > 0 && (
        <>
          <div className="resumen">
            <p className="resumen-titulo">
              <strong>{resumen.porcentaje} %</strong> de asistencia
            </p>
            <div
              className="reparto"
              role="img"
              aria-label={`Asistió ${resumen.presentes}, tardanza ${resumen.tardanzas}, faltó ${resumen.faltas}`}
            >
              <span className="tramo presente" style={{ flexGrow: resumen.presentes }} />
              <span className="tramo tardanza" style={{ flexGrow: resumen.tardanzas }} />
              <span className="tramo falta" style={{ flexGrow: resumen.faltas }} />
            </div>
            <ul className="leyenda">
              <li>
                <span className="punto presente" />
                Asistió <strong>{resumen.presentes}</strong>
              </li>
              <li>
                <span className="punto tardanza" />
                Tardanza <strong>{resumen.tardanzas}</strong>
              </li>
              <li>
                <span className="punto falta" />
                Faltó <strong>{resumen.faltas}</strong>
              </li>
            </ul>
          </div>

          <ul className="dias">
            {vigente.registros.map((r) => (
              <li key={r.fecha} className={`dia ${r.estado.toLowerCase()}`}>
                <span className="dia-fecha">{nombreDia(r.fecha)}</span>
                <span className="dia-hora">{r.hora_registro ? `${r.hora_registro} h` : ''}</span>
                <span className={`estado ${r.estado.toLowerCase()}`}>{r.estado_texto}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
