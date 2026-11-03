import { useEffect, useState } from 'react'
import { api } from '../api'

const mensajeDeError = (e) => {
  const datos = e.response?.data
  if (datos && typeof datos === 'object') {
    const primero = Object.values(datos)[0]
    if (typeof primero === 'string') return primero
  }
  return 'No se pudieron cargar los temas'
}

export default function TemasCriticos() {
  const [simulacros, setSimulacros] = useState([])
  const [cursos, setCursos] = useState([])
  const [seleccion, setSeleccion] = useState('')
  const [datos, setDatos] = useState(null)
  const [analisis, setAnalisis] = useState(null)
  const [analizando, setAnalizando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([api.get('/simulacros/'), api.get('/cursos/')])
      .then(([s, c]) => {
        setSimulacros(s.data.filter((x) => x.activa))
        setCursos(c.data)
      })
      .catch(() => setError('No se pudieron cargar los simulacros'))
  }, [])

  const consulta = seleccion ? `?simulacro=${seleccion}` : ''

  useEffect(() => {
    api
      .get(`/rendimiento/temas/${consulta}`)
      .then((r) => {
        setDatos({ clave: seleccion, ...r.data })
        setError('')
      })
      .catch((e) => {
        setDatos(null)
        setError(mensajeDeError(e))
      })
  }, [seleccion, consulta])

  const analizar = () => {
    setAnalizando(true)
    api
      .get(`/rendimiento/temas/${consulta}${consulta ? '&' : '?'}ia=1`)
      .then((r) => {
        setAnalisis({ clave: seleccion, ...r.data })
        setError('')
      })
      .catch((e) => setError(mensajeDeError(e)))
      .finally(() => setAnalizando(false))
  }

  const nombreCurso = (id) => cursos.find((c) => c.id === id)?.nombre ?? ''
  const vigente =
    analisis && analisis.clave === seleccion
      ? analisis
      : datos && datos.clave === seleccion
        ? datos
        : null
  const hayTemas = vigente?.secciones.some((s) => s.temas.length > 0)

  return (
    <section className="panel">
      <h2>Temas con más errores</h2>
      <p>
        Qué temas fallaron más los alumnos de cada salón, para coordinar un plan de refuerzo con el
        docente.
      </p>

      <div className="formulario">
        <label>
          <span>Simulacro</span>
          <select value={seleccion} onChange={(e) => setSeleccion(e.target.value)}>
            <option value="">El más reciente</option>
            {simulacros.map((s) => (
              <option key={s.id} value={s.id}>
                {nombreCurso(s.curso)} · {s.nombre} ({s.fecha})
              </option>
            ))}
          </select>
        </label>
        <div className="acciones">
          <button onClick={analizar} disabled={analizando || !hayTemas}>
            {analizando ? 'Analizando...' : 'Analizar con IA'}
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {!vigente && !error && <p>Cargando temas...</p>}
      {vigente && !vigente.simulacro && <p>Aún no hay simulacros con resultados.</p>}

      {vigente?.simulacro && (
        <>
          <p className="subtitulo">
            {vigente.simulacro.curso} · {vigente.simulacro.nombre} · {vigente.simulacro.fecha}
          </p>

          {vigente.ia.disponible && (
            <p className="ok">Análisis generado con IA (Gemini). Revísalo antes de compartirlo.</p>
          )}
          {!vigente.ia.disponible && vigente.ia.motivo && (
            <p className="aviso">Se muestra un resumen automático. {vigente.ia.motivo}</p>
          )}

          {vigente.secciones.length === 0 && <p>Este simulacro aún no tiene resultados.</p>}

          <div className="temas-lista">
            {vigente.secciones.map((s) => {
              const maximo = Math.max(1, ...s.temas.map((t) => t.errores))
              return (
                <article className="tema-salon" key={s.seccion}>
                  <header>
                    <h3>{s.nombre}</h3>
                    <span>{s.alumnos_evaluados} alumnos evaluados</span>
                  </header>
                  <p className="tema-resumen">
                    {vigente.ia.disponible && <span className="etiqueta-ia">IA</span>}
                    {s.resumen}
                  </p>
                  {s.temas.map((t) => (
                    <div
                      className="tema-fila"
                      key={t.tema}
                      title={`${t.errores} errores · ${t.alumnos_con_error} alumnos`}
                    >
                      <span className="barra-nombre">{t.tema}</span>
                      <div className="barra-pista">
                        <div
                          className="barra refuerzo"
                          style={{ width: `${(t.errores / maximo) * 100}%` }}
                        />
                      </div>
                      <span className="tema-dato">
                        {t.errores} errores · {t.porcentaje_alumnos}% de alumnos
                      </span>
                    </div>
                  ))}
                </article>
              )
            })}
          </div>
        </>
      )}
    </section>
  )
}
