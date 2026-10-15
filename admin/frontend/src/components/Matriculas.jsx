import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

const NUEVO_VACIO = { first_name: '', last_name: '', email: '' }

export default function Matriculas() {
  const [secciones, setSecciones] = useState([])
  const [cursos, setCursos] = useState([])
  const [alumnos, setAlumnos] = useState([])
  const [matriculas, setMatriculas] = useState([])
  const [seccionId, setSeccionId] = useState('')
  const [modo, setModo] = useState('existente')
  const [alumnoId, setAlumnoId] = useState('')
  const [nuevo, setNuevo] = useState(NUEVO_VACIO)
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  const cargarBase = useCallback(async () => {
    try {
      const [s, c, a] = await Promise.all([
        api.get('/secciones/'),
        api.get('/cursos/'),
        api.get('/alumnos/'),
      ])
      setSecciones(s.data.filter((x) => x.activa))
      setCursos(c.data)
      setAlumnos(a.data)
    } catch {
      setErrores({ error: 'No se pudo cargar la información del servidor' })
    }
  }, [])

  const cargarMatriculas = useCallback(async () => {
    if (!seccionId) {
      setMatriculas([])
      return
    }
    try {
      const { data } = await api.get(`/matriculas/?seccion=${seccionId}`)
      setMatriculas(data.filter((m) => m.activa))
    } catch {
      setErrores({ error: 'No se pudieron cargar las matrículas' })
    }
  }, [seccionId])

  useEffect(() => {
    cargarBase()
  }, [cargarBase])

  useEffect(() => {
    cargarMatriculas()
  }, [cargarMatriculas])

  const nombreCurso = (id) => cursos.find((c) => c.id === id)?.nombre ?? ''
  const nombreAlumno = (id) => {
    const a = alumnos.find((x) => x.id === id)
    return a ? `${a.first_name} ${a.last_name} (${a.email})` : id
  }
  const disponibles = alumnos.filter(
    (a) => !matriculas.some((m) => m.alumno === a.id),
  )

  const mostrarError = (err) => {
    const data = err.response?.data
    setErrores(
      data && typeof data === 'object'
        ? data
        : { error: 'No se pudo completar la operación.' },
    )
  }

  const matricular = async (e) => {
    e.preventDefault()
    setErrores(null)
    setMensaje('')
    try {
      if (modo === 'existente') {
        await api.post('/matriculas/', {
          alumno: Number(alumnoId),
          seccion: Number(seccionId),
        })
        setMensaje('Alumno matriculado correctamente')
      } else {
        const { data } = await api.post('/matricular-nuevo/', {
          ...nuevo,
          seccion: Number(seccionId),
        })
        setMensaje(
          data.correo_enviado
            ? 'Alumno creado y matriculado. Se envió un correo con sus credenciales.'
            : 'Alumno creado y matriculado, pero no se pudo enviar el correo.',
        )
        setNuevo(NUEVO_VACIO)
      }
      setAlumnoId('')
      await Promise.all([cargarBase(), cargarMatriculas()])
    } catch (err) {
      mostrarError(err)
    }
  }

  const desactivar = async (matricula) => {
    setErrores(null)
    setMensaje('')
    try {
      await api.delete(`/matriculas/${matricula.id}/`)
      await Promise.all([cargarBase(), cargarMatriculas()])
    } catch (err) {
      mostrarError(err)
    }
  }

  const cambiarNuevo = (campo, e) =>
    setNuevo((prev) => ({ ...prev, [campo]: e.target.value }))

  return (
    <section className="panel">
      <h2>Matrículas</h2>

      <div className="formulario">
        <label>
          <span>Sección</span>
          <select value={seccionId} onChange={(e) => setSeccionId(e.target.value)}>
            <option value="">Selecciona...</option>
            {secciones.map((s) => (
              <option key={s.id} value={s.id}>
                {nombreCurso(s.curso)} - {s.nombre} ({s.matriculados}/{s.cupo})
              </option>
            ))}
          </select>
        </label>
      </div>

      {seccionId && (
        <>
          <div className="acciones">
            <button
              type="button"
              className={modo === 'existente' ? '' : 'secundario'}
              onClick={() => setModo('existente')}
            >
              Alumno existente
            </button>
            <button
              type="button"
              className={modo === 'nuevo' ? '' : 'secundario'}
              onClick={() => setModo('nuevo')}
            >
              Alumno nuevo
            </button>
          </div>

          <form className="formulario" onSubmit={matricular}>
            {modo === 'existente' ? (
              <label>
                <span>Alumno</span>
                <select
                  value={alumnoId}
                  onChange={(e) => setAlumnoId(e.target.value)}
                  required
                >
                  <option value="">Selecciona...</option>
                  {disponibles.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.first_name} {a.last_name} ({a.email})
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <>
                <label>
                  <span>Nombres</span>
                  <input
                    value={nuevo.first_name}
                    onChange={(e) => cambiarNuevo('first_name', e)}
                    required
                  />
                </label>
                <label>
                  <span>Apellidos</span>
                  <input
                    value={nuevo.last_name}
                    onChange={(e) => cambiarNuevo('last_name', e)}
                    required
                  />
                </label>
                <label>
                  <span>Correo</span>
                  <input
                    type="email"
                    value={nuevo.email}
                    onChange={(e) => cambiarNuevo('email', e)}
                    required
                  />
                </label>
              </>
            )}
            <div className="acciones">
              <button type="submit">Matricular</button>
            </div>
          </form>
        </>
      )}

      {mensaje && <p className="ok">{mensaje}</p>}
      {errores && (
        <ul className="errores">
          {Object.entries(errores).map(([campo, msgs]) => (
            <li key={campo}>
              {campo}: {[].concat(msgs).join(' ')}
            </li>
          ))}
        </ul>
      )}

      {seccionId && (
        <table>
          <thead>
            <tr>
              <th>Alumno</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {matriculas.length === 0 && (
              <tr>
                <td colSpan={2}>Aún no hay alumnos matriculados en esta sección.</td>
              </tr>
            )}
            {matriculas.map((m) => (
              <tr key={m.id}>
                <td>{nombreAlumno(m.alumno)}</td>
                <td className="celda-acciones">
                  <button type="button" className="peligro" onClick={() => desactivar(m)}>
                    Quitar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
