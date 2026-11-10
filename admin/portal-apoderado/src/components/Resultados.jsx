import { useEffect, useState } from 'react'
import { api } from '../api'
import GraficoAvance from './GraficoAvance'

const fechaCorta = (texto) =>
  new Date(`${texto}T12:00:00`).toLocaleDateString('es-PE', { day: 'numeric', month: 'short' })

const textoVariacion = (variacion) => {
  if (variacion === null) return 'Primer simulacro'
  if (variacion > 0) return `▲ +${variacion} respecto al anterior`
  if (variacion < 0) return `▼ ${variacion} respecto al anterior`
  return 'Igual que el anterior'
}

const claseVariacion = (variacion) =>
  variacion === null || variacion === 0 ? 'igual' : variacion > 0 ? 'sube' : 'baja'

export default function Resultados({ hijo }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let vigente = true
    api
      .get(`/mis-hijos/${hijo.id}/simulacros/`)
      .then((r) => vigente && setDatos(r.data))
      .catch(() => vigente && setError('No se pudieron cargar los resultados de simulacros.'))
    return () => {
      vigente = false
    }
  }, [hijo.id])

  return (
    <section className="tarjeta resultados">
      <h2>Resultados de simulacros</h2>
      <p className="suave">Notas de {hijo.first_name} sobre 20 y su avance entre simulacros.</p>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!datos && !error && <p className="suave">Cargando resultados...</p>}
      {datos && datos.cursos.length === 0 && (
        <p className="vacio">Todavía no hay resultados de simulacros registrados.</p>
      )}

      {datos?.cursos.map((curso) => (
        <article key={curso.curso_id} className="curso-resultados">
          <header>
            <h3>{curso.curso}</h3>
            <span className="promedio">
              Promedio <strong>{curso.promedio}</strong>
            </span>
          </header>
          <GraficoAvance resultados={curso.resultados} />
          <ul className="lista-simulacros">
            {curso.resultados.map((r) => (
              <li key={r.simulacro_id}>
                <span>
                  {r.simulacro}
                  <small>{fechaCorta(r.fecha)}</small>
                </span>
                <span className={`variacion ${claseVariacion(r.variacion)}`}>
                  {textoVariacion(r.variacion)}
                </span>
                <strong className="nota">{r.puntaje}</strong>
              </li>
            ))}
          </ul>
        </article>
      ))}
    </section>
  )
}
