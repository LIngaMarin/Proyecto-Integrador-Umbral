import { useEffect, useState } from 'react'
import { api } from '../api'

const MAXIMO = 20

const mensajeDeError = (e) => {
  const datos = e.response?.data
  if (datos && typeof datos === 'object') {
    const primero = Object.values(datos)[0]
    if (typeof primero === 'string') return primero
  }
  return 'No se pudo cargar la comparación'
}

const formato = (n) => (n === null || n === undefined ? '—' : n.toFixed(2))

function Cambio({ seccion }) {
  if (seccion.tendencia === 'sin_datos') {
    return <span className="cambio sin-datos">Sin datos para comparar</span>
  }
  const signo = seccion.diferencia > 0 ? '+' : seccion.diferencia < 0 ? '−' : ''
  const valor = Math.abs(seccion.diferencia).toFixed(2)
  const icono = { mejora: '▲', baja: '▼', igual: '=' }[seccion.tendencia]
  return (
    <span className={`cambio ${seccion.tendencia}`}>
      {icono} {signo}
      {valor}
    </span>
  )
}

export default function Comparacion() {
  const [cursos, setCursos] = useState([])
  const [simulacros, setSimulacros] = useState([])
  const [cursoElegido, setCursoElegido] = useState('')
  const [anteriorElegido, setAnteriorElegido] = useState('')
  const [actualElegido, setActualElegido] = useState('')
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')
  const [cargado, setCargado] = useState(false)

  useEffect(() => {
    Promise.all([api.get('/cursos/'), api.get('/simulacros/')])
      .then(([c, s]) => {
        setCursos(c.data.filter((x) => x.activo))
        setSimulacros(s.data.filter((x) => x.activa))
      })
      .catch(() => setError('No se pudieron cargar los cursos y simulacros'))
      .finally(() => setCargado(true))
  }, [])

  const delCurso = (id) =>
    simulacros
      .filter((s) => String(s.curso) === String(id))
      .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.id - b.id)

  const comparables = cursos.filter((c) => delCurso(c.id).length >= 2)
  const cursoId = cursoElegido || (comparables[0] ? String(comparables[0].id) : '')
  const lista = delCurso(cursoId)
  const anteriorId = anteriorElegido || (lista.length >= 2 ? String(lista[lista.length - 2].id) : '')
  const actualId = actualElegido || (lista.length >= 2 ? String(lista[lista.length - 1].id) : '')

  useEffect(() => {
    if (!anteriorId || !actualId) return
    api
      .get(`/rendimiento/comparar/?anterior=${anteriorId}&actual=${actualId}`)
      .then((r) => {
        setDatos(r.data)
        setError('')
      })
      .catch((e) => {
        setDatos(null)
        setError(mensajeDeError(e))
      })
  }, [anteriorId, actualId])

  const vigente =
    datos && String(datos.anterior.id) === anteriorId && String(datos.actual.id) === actualId
      ? datos
      : null

  const elegirCurso = (valor) => {
    setCursoElegido(valor)
    setAnteriorElegido('')
    setActualElegido('')
    setError('')
  }

  return (
    <section className="panel">
      <h2>Comparar simulacros</h2>
      <p>Mide si el refuerzo funcionó: compara el promedio de cada salón entre dos simulacros.</p>

      {error && <p className="error">{error}</p>}
      {cargado && comparables.length === 0 && !error && (
        <p className="aviso">
          Ningún curso tiene todavía dos simulacros para comparar. Crea otro simulacro y registra sus
          resultados.
        </p>
      )}

      {comparables.length > 0 && (
        <div className="formulario">
          <label>
            <span>Curso</span>
            <select value={cursoId} onChange={(e) => elegirCurso(e.target.value)}>
              {comparables.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Simulacro anterior</span>
            <select value={anteriorId} onChange={(e) => setAnteriorElegido(e.target.value)}>
              {lista.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nombre} ({s.fecha})
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Simulacro actual</span>
            <select value={actualId} onChange={(e) => setActualElegido(e.target.value)}>
              {lista.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nombre} ({s.fecha})
                </option>
              ))}
            </select>
          </label>
        </div>
      )}

      {vigente && (
        <>
          <div className="tarjetas">
            <div className="tarjeta">
              <span className="tarjeta-valor">{vigente.resumen.mejoraron}</span>
              <span className="tarjeta-etiqueta">Salones que mejoraron</span>
            </div>
            <div className="tarjeta">
              <span className="tarjeta-valor">{vigente.resumen.bajaron}</span>
              <span className="tarjeta-etiqueta">Salones que bajaron</span>
            </div>
            <div className="tarjeta">
              <span className="tarjeta-valor">
                {vigente.secciones.filter((s) => s.refuerzo_resuelto).length}
              </span>
              <span className="tarjeta-etiqueta">Salieron de refuerzo</span>
            </div>
          </div>

          <div className="comparacion" role="img" aria-label="Promedio por salón en cada simulacro">
            {vigente.secciones.map((s) => (
              <div className="cmp-fila" key={s.seccion}>
                <span className="barra-nombre">{s.nombre}</span>
                <div className="cmp-pistas">
                  {[
                    ['antes', vigente.anterior.nombre, s.promedio_anterior],
                    ['ahora', vigente.actual.nombre, s.promedio_actual],
                  ].map(([tipo, nombre, valor]) => (
                    <div className="cmp-pista" key={tipo} title={`${nombre}: ${formato(valor)}`}>
                      {valor !== null && (
                        <div
                          className={`cmp-relleno ${tipo} ${
                            tipo === 'ahora' && s.necesita_refuerzo ? 'refuerzo' : ''
                          }`}
                          style={{ width: `${(valor / MAXIMO) * 100}%` }}
                        />
                      )}
                      <i
                        className="cmp-umbral"
                        style={{ left: `${(vigente.umbral / MAXIMO) * 100}%` }}
                      />
                      <span className="cmp-valor">{formato(valor)}</span>
                    </div>
                  ))}
                </div>
                <div className="cmp-cambio">
                  <Cambio seccion={s} />
                  {s.refuerzo_resuelto && <span className="etiqueta-ok">Salió de refuerzo</span>}
                  {s.necesita_refuerzo && <span className="etiqueta-alerta">Sigue en refuerzo</span>}
                </div>
              </div>
            ))}
            <div className="leyenda">
              <span>
                <i className="punto antes" /> {vigente.anterior.nombre} ({vigente.anterior.fecha})
              </span>
              <span>
                <i className="punto normal" /> {vigente.actual.nombre} ({vigente.actual.fecha})
              </span>
              <span>
                <i className="punto refuerzo" /> Bajo el mínimo ({vigente.umbral})
              </span>
            </div>
          </div>

          <table>
            <thead>
              <tr>
                <th>Salón</th>
                <th>{vigente.anterior.nombre}</th>
                <th>{vigente.actual.nombre}</th>
                <th>Cambio</th>
                <th>Alumnos evaluados</th>
              </tr>
            </thead>
            <tbody>
              {vigente.secciones.map((s) => (
                <tr key={s.seccion}>
                  <td>{s.nombre}</td>
                  <td>{formato(s.promedio_anterior)}</td>
                  <td>{formato(s.promedio_actual)}</td>
                  <td>
                    <Cambio seccion={s} />
                  </td>
                  <td>
                    {s.evaluados_anterior} → {s.evaluados_actual}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  )
}
