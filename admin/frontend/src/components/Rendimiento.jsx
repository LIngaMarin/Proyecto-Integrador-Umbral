import { useEffect, useState } from 'react'
import { api } from '../api'

const MAXIMO = 20

export default function Rendimiento() {
  const [simulacros, setSimulacros] = useState([])
  const [seleccion, setSeleccion] = useState('')
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get('/simulacros/')
      .then((r) => setSimulacros(r.data.filter((s) => s.activa)))
      .catch(() => setError('No se pudieron cargar los simulacros'))
  }, [])

  useEffect(() => {
    const consulta = seleccion ? `?simulacro=${seleccion}` : ''
    api
      .get(`/rendimiento/${consulta}`)
      .then((r) => {
        setDatos(r.data)
        setError('')
      })
      .catch(() => setError('No se pudo cargar el rendimiento'))
  }, [seleccion])

  return (
    <section className="panel">
      <h2>Rendimiento por salón</h2>

      <div className="formulario">
        <label>
          <span>Simulacro</span>
          <select value={seleccion} onChange={(e) => setSeleccion(e.target.value)}>
            <option value="">El más reciente</option>
            {simulacros.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre} ({s.fecha})
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <p className="error">{error}</p>}
      {!datos && !error && <p>Cargando rendimiento...</p>}

      {datos && !datos.simulacro && <p>Aún no hay simulacros con resultados.</p>}

      {datos && datos.simulacro && (
        <>
          <p className="subtitulo">
            {datos.simulacro.nombre} · {datos.simulacro.fecha}
          </p>

          <div className="grafico" role="img" aria-label="Promedio por sección">
            {datos.secciones.map((s) => (
              <div
                className="fila-barra"
                key={s.seccion}
                title={`${s.nombre}: promedio ${s.promedio} (${s.evaluados} alumnos, máximo ${s.maximo}, mínimo ${s.minimo})`}
              >
                <span className="barra-nombre">{s.nombre}</span>
                <div className="barra-pista">
                  <div
                    className={`barra ${s.necesita_refuerzo ? 'refuerzo' : ''}`}
                    style={{ width: `${(s.promedio / MAXIMO) * 100}%` }}
                  />
                  <div
                    className="barra-umbral"
                    style={{ left: `${(datos.umbral / MAXIMO) * 100}%` }}
                  />
                </div>
                <span className="barra-valor">{s.promedio.toFixed(2)}</span>
              </div>
            ))}
            <div className="leyenda">
              <span>
                <i className="punto normal" /> Sección al día
              </span>
              <span>
                <i className="punto refuerzo" /> Necesita refuerzo
              </span>
              <span>Línea punteada: mínimo aprobatorio ({datos.umbral})</span>
            </div>
          </div>

          <table>
            <thead>
              <tr>
                <th>Sección</th>
                <th>Alumnos evaluados</th>
                <th>Promedio</th>
                <th>Máximo</th>
                <th>Mínimo</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {datos.secciones.length === 0 && (
                <tr>
                  <td colSpan={6}>Este simulacro aún no tiene resultados.</td>
                </tr>
              )}
              {datos.secciones.map((s) => (
                <tr key={s.seccion}>
                  <td>{s.nombre}</td>
                  <td>{s.evaluados}</td>
                  <td>{s.promedio.toFixed(2)}</td>
                  <td>{s.maximo}</td>
                  <td>{s.minimo}</td>
                  <td>{s.necesita_refuerzo ? '⚠ Necesita refuerzo' : 'Al día'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  )
}
