import { useEffect, useState } from 'react'
import { api } from '../api'

const ZONA = 'America/Lima'
const DIAS_SEMANA = ['L', 'M', 'M', 'J', 'V', 'S', 'D']

const hoyLima = () => new Date().toLocaleDateString('en-CA', { timeZone: ZONA })
const dos = (n) => String(n).padStart(2, '0')
const clave = (anio, mes, dia) => `${anio}-${dos(mes)}-${dos(dia)}`

const nombreMes = (anio, mes) =>
  new Date(Date.UTC(anio, mes - 1, 1)).toLocaleDateString('es-PE', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })

const fechaLarga = (texto) =>
  new Date(`${texto}T12:00:00`).toLocaleDateString('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  })

const diasHasta = (texto, hoy) =>
  Math.round((new Date(`${texto}T12:00:00`) - new Date(`${hoy}T12:00:00`)) / 86400000)

const textoFaltan = (dias) => {
  if (dias === 0) return 'Es hoy'
  if (dias === 1) return 'Es mañana'
  if (dias > 1) return `Faltan ${dias} días`
  return 'Ya pasó'
}

export default function CalendarioSimulacros({ hijo }) {
  const hoy = hoyLima()
  const [anio, setAnio] = useState(() => Number(hoy.slice(0, 4)))
  const [mes, setMes] = useState(() => Number(hoy.slice(5, 7)))
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')
  const [elegido, setElegido] = useState(null)

  const ultimoDia = new Date(Date.UTC(anio, mes, 0)).getUTCDate()
  const desde = clave(anio, mes, 1)
  const hasta = clave(anio, mes, ultimoDia)

  useEffect(() => {
    let vigente = true
    api
      .get(`/mis-hijos/${hijo.id}/simulacros-programados/?desde=${desde}&hasta=${hasta}`)
      .then((r) => {
        if (!vigente) return
        setError('')
        setDatos({ desde, simulacros: r.data.simulacros })
      })
      .catch(() => vigente && setError('No se pudo cargar el calendario de simulacros.'))
    return () => {
      vigente = false
    }
  }, [hijo.id, desde, hasta])

  const irA = (cambio) => {
    const fecha = new Date(Date.UTC(anio, mes - 1 + cambio, 1))
    setAnio(fecha.getUTCFullYear())
    setMes(fecha.getUTCMonth() + 1)
    setElegido(null)
  }

  // Los datos del mes anterior no se muestran mientras llegan los del nuevo.
  const vigente = datos?.desde === desde ? datos : null
  const porDia = {}
  vigente?.simulacros.forEach((s) => {
    ;(porDia[s.fecha] ??= []).push(s)
  })

  const huecos = (new Date(Date.UTC(anio, mes - 1, 1)).getUTCDay() + 6) % 7
  const celdas = [
    ...Array.from({ length: huecos }, () => null),
    ...Array.from({ length: ultimoDia }, (_, i) => i + 1),
  ]
  const detalle = elegido ? (porDia[elegido] ?? []) : []

  return (
    <section className="tarjeta calendario">
      <h2>Calendario de simulacros</h2>
      <p className="suave">Los días marcados tienen un simulacro programado para {hijo.first_name}.</p>

      <div className="calendario-mes">
        <button type="button" className="secundario" onClick={() => irA(-1)} aria-label="Mes anterior">
          ‹
        </button>
        <strong>{nombreMes(anio, mes)}</strong>
        <button type="button" className="secundario" onClick={() => irA(1)} aria-label="Mes siguiente">
          ›
        </button>
      </div>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!vigente && !error && <p className="suave">Cargando calendario...</p>}

      <div className="calendario-rejilla">
        {DIAS_SEMANA.map((d, i) => (
          <span key={i} className="calendario-semana">
            {d}
          </span>
        ))}
        {celdas.map((dia, i) => {
          if (dia === null) return <span key={`h${i}`} />
          const fecha = clave(anio, mes, dia)
          const lista = porDia[fecha]
          const clases = ['calendario-dia']
          if (fecha === hoy) clases.push('hoy')
          if (lista) clases.push('con-simulacro')
          if (fecha === elegido) clases.push('elegido')
          return lista ? (
            <button
              key={fecha}
              type="button"
              className={clases.join(' ')}
              aria-pressed={fecha === elegido}
              aria-label={`${fechaLarga(fecha)}: ${lista.length} simulacro${lista.length > 1 ? 's' : ''}`}
              onClick={() => setElegido(fecha === elegido ? null : fecha)}
            >
              {dia}
            </button>
          ) : (
            <span key={fecha} className={clases.join(' ')}>
              {dia}
            </span>
          )
        })}
      </div>

      {vigente && vigente.simulacros.length === 0 && (
        <p className="vacio">No hay simulacros programados en este mes.</p>
      )}

      {detalle.length > 0 && (
        <ul className="calendario-detalle" aria-live="polite">
          {detalle.map((s) => (
            <li key={s.id}>
              <strong>{s.nombre}</strong>
              <span>
                {s.curso} · Sección {s.seccion}
              </span>
              <span className="suave">
                {fechaLarga(s.fecha)} · {textoFaltan(diasHasta(s.fecha, hoy))}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
