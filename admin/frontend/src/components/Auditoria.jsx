import { useEffect, useState } from 'react'
import { api } from '../api'

const ACCIONES = [
  { valor: 'CREAR', etiqueta: 'Creó' },
  { valor: 'EDITAR', etiqueta: 'Editó' },
  { valor: 'ELIMINAR', etiqueta: 'Eliminó' },
]

const MODULOS = {
  academias: 'Academias',
  sedes: 'Sedes',
  aulas: 'Aulas',
  ciclos: 'Ciclos',
  festivos: 'Festivos',
  cursos: 'Cursos',
  secciones: 'Secciones',
  matriculas: 'Matrículas',
  'matricular-nuevo': 'Matrícula de alumno nuevo',
  horarios: 'Horarios',
  simulacros: 'Simulacros',
  resultados: 'Resultados',
  usuarios: 'Usuarios',
  apoderados: 'Apoderados',
  'alumnos-masivo': 'Carga masiva',
  marca: 'Marca',
  'configuracion-ia': 'Inteligencia artificial',
}

const FILTROS_VACIOS = { usuario: '', accion: '', recurso: '', desde: '', hasta: '' }

const formatoFecha = (valor) =>
  new Date(valor).toLocaleString('es-PE', {
    timeZone: 'America/Lima',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

const mensajeDeError = (e) => {
  const datos = e.response?.data
  if (datos && typeof datos === 'object') {
    const primero = Object.values(datos)[0]
    if (typeof primero === 'string') return primero
  }
  return 'No se pudo cargar el historial'
}

export default function Auditoria() {
  const [usuarios, setUsuarios] = useState([])
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [pagina, setPagina] = useState(1)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get('/usuarios/')
      .then((r) => setUsuarios(r.data))
      .catch(() => setUsuarios([]))
  }, [])

  const parametros = new URLSearchParams()
  Object.entries(filtros).forEach(([nombre, valor]) => {
    if (valor) parametros.set(nombre, valor)
  })
  parametros.set('pagina', pagina)
  const consulta = parametros.toString()

  useEffect(() => {
    api
      .get(`/auditoria/?${consulta}`)
      .then((r) => {
        setDatos({ consulta, ...r.data })
        setError('')
      })
      .catch((e) => setError(mensajeDeError(e)))
  }, [consulta])

  const cambiar = (nombre) => (e) => {
    setFiltros({ ...filtros, [nombre]: e.target.value })
    setPagina(1)
  }

  const limpiar = () => {
    setFiltros(FILTROS_VACIOS)
    setPagina(1)
  }

  const hayFiltros = Object.values(filtros).some(Boolean)
  const vigente = datos && datos.consulta === consulta ? datos : null
  const nombreDe = (u) => `${u.first_name} ${u.last_name}`.trim() || u.email

  return (
    <section className="panel">
      <h2>Auditoría</h2>
      <p>Quién creó, editó o eliminó información en el panel, y cuándo lo hizo.</p>

      <div className="formulario">
        <label>
          <span>Usuario</span>
          <select value={filtros.usuario} onChange={cambiar('usuario')}>
            <option value="">Todos</option>
            {usuarios.map((u) => (
              <option key={u.id} value={u.id}>
                {nombreDe(u)}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Acción</span>
          <select value={filtros.accion} onChange={cambiar('accion')}>
            <option value="">Todas</option>
            {ACCIONES.map((a) => (
              <option key={a.valor} value={a.valor}>
                {a.etiqueta}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Módulo</span>
          <select value={filtros.recurso} onChange={cambiar('recurso')}>
            <option value="">Todos</option>
            {Object.entries(MODULOS).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Desde</span>
          <input type="date" value={filtros.desde} max={filtros.hasta} onChange={cambiar('desde')} />
        </label>
        <label>
          <span>Hasta</span>
          <input type="date" value={filtros.hasta} min={filtros.desde} onChange={cambiar('hasta')} />
        </label>
        <div className="acciones">
          <button type="button" className="secundario" onClick={limpiar} disabled={!hayFiltros}>
            Limpiar filtros
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {!vigente && !error && <p>Cargando historial...</p>}

      {vigente && (
        <>
          <p className="subtitulo">
            {vigente.total === 0
              ? 'Sin movimientos'
              : `${vigente.total} ${vigente.total === 1 ? 'movimiento' : 'movimientos'}`}
          </p>

          {vigente.total === 0 && (
            <p>
              {hayFiltros
                ? 'No hay movimientos con esos filtros. Prueba ampliando las fechas.'
                : 'Todavía no hay movimientos. Aparecerán aquí cuando alguien cree, edite o elimine información.'}
            </p>
          )}

          {vigente.total > 0 && (
            <div className="tabla-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Fecha y hora</th>
                    <th>Usuario</th>
                    <th>Acción</th>
                    <th>Módulo</th>
                    <th>Registro</th>
                    <th>IP</th>
                  </tr>
                </thead>
                <tbody>
                  {vigente.resultados.map((r) => (
                    <tr key={r.id}>
                      <td>{formatoFecha(r.fecha)}</td>
                      <td>{r.usuario_nombre}</td>
                      <td>
                        <span className={`accion accion-${r.accion.toLowerCase()}`}>
                          {r.accion_texto}
                        </span>
                      </td>
                      <td>{MODULOS[r.recurso] ?? r.recurso}</td>
                      <td>{r.objeto_id ? `N.º ${r.objeto_id}` : '—'}</td>
                      <td>{r.ip ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {vigente.paginas > 1 && (
            <nav className="paginacion" aria-label="Páginas del historial">
              <button
                type="button"
                className="secundario"
                onClick={() => setPagina(vigente.pagina - 1)}
                disabled={vigente.pagina <= 1}
              >
                Anterior
              </button>
              <span>
                Página {vigente.pagina} de {vigente.paginas}
              </span>
              <button
                type="button"
                className="secundario"
                onClick={() => setPagina(vigente.pagina + 1)}
                disabled={vigente.pagina >= vigente.paginas}
              >
                Siguiente
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
