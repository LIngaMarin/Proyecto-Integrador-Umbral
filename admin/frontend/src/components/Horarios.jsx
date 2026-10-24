import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
const VACIO = { docente: '', aula: '', dia_semana: '0', hora_inicio: '', hora_fin: '' }

export default function Horarios() {
  const [secciones, setSecciones] = useState([])
  const [cursos, setCursos] = useState([])
  const [docentes, setDocentes] = useState([])
  const [aulas, setAulas] = useState([])
  const [horarios, setHorarios] = useState([])
  const [seccionId, setSeccionId] = useState('')
  const [form, setForm] = useState(VACIO)
  const [editandoId, setEditandoId] = useState(null)
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  const cargarBase = useCallback(async () => {
    try {
      const [s, c, d, a] = await Promise.all([
        api.get('/secciones/'),
        api.get('/cursos/'),
        api.get('/docentes/'),
        api.get('/aulas/'),
      ])
      setSecciones(s.data.filter((x) => x.activa))
      setCursos(c.data)
      setDocentes(d.data)
      setAulas(a.data.filter((x) => x.activa))
    } catch {
      setErrores({ error: 'No se pudo cargar la información del servidor' })
    }
  }, [])

  const cargarHorarios = useCallback(async () => {
    if (!seccionId) {
      setHorarios([])
      return
    }
    try {
      const { data } = await api.get(`/horarios/?seccion=${seccionId}`)
      setHorarios(data.filter((h) => h.activa))
    } catch {
      setErrores({ error: 'No se pudieron cargar los horarios' })
    }
  }, [seccionId])

  useEffect(() => {
    cargarBase()
  }, [cargarBase])

  useEffect(() => {
    cargarHorarios()
  }, [cargarHorarios])

  const nombreCurso = (id) => cursos.find((c) => c.id === id)?.nombre ?? ''
  const nombreDocente = (id) => {
    const d = docentes.find((x) => x.id === id)
    return d ? `${d.first_name} ${d.last_name}` : id
  }
  const nombreAula = (id) => aulas.find((a) => a.id === id)?.nombre ?? id
  const corta = (hora) => String(hora).slice(0, 5)

  const mostrarError = (err) => {
    const data = err.response?.data
    setErrores(
      data && typeof data === 'object'
        ? data
        : { error: 'No se pudo completar la operación.' },
    )
  }

  const cambiar = (campo, e) => setForm((prev) => ({ ...prev, [campo]: e.target.value }))

  const limpiar = () => {
    setForm(VACIO)
    setEditandoId(null)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setErrores(null)
    setMensaje('')
    const datos = {
      seccion: Number(seccionId),
      docente: Number(form.docente),
      aula: Number(form.aula),
      dia_semana: Number(form.dia_semana),
      hora_inicio: form.hora_inicio,
      hora_fin: form.hora_fin,
    }
    try {
      if (editandoId) {
        await api.put(`/horarios/${editandoId}/`, datos)
      } else {
        await api.post('/horarios/', datos)
      }
      limpiar()
      setMensaje('Horario guardado correctamente')
      await cargarHorarios()
    } catch (err) {
      mostrarError(err)
    }
  }

  const editar = (h) => {
    setEditandoId(h.id)
    setForm({
      docente: String(h.docente),
      aula: String(h.aula),
      dia_semana: String(h.dia_semana),
      hora_inicio: corta(h.hora_inicio),
      hora_fin: corta(h.hora_fin),
    })
    setErrores(null)
    setMensaje('')
  }

  const quitar = async (h) => {
    setErrores(null)
    setMensaje('')
    try {
      await api.delete(`/horarios/${h.id}/`)
      await cargarHorarios()
    } catch (err) {
      mostrarError(err)
    }
  }

  return (
    <section className="panel">
      <h2>Horarios</h2>

      <div className="formulario">
        <label>
          <span>Sección</span>
          <select
            value={seccionId}
            onChange={(e) => {
              setSeccionId(e.target.value)
              limpiar()
            }}
          >
            <option value="">Selecciona...</option>
            {secciones.map((s) => (
              <option key={s.id} value={s.id}>
                {nombreCurso(s.curso)} - {s.nombre}
              </option>
            ))}
          </select>
        </label>
      </div>

      {seccionId && (
        <form className="formulario" onSubmit={guardar}>
          <label>
            <span>Día</span>
            <select value={form.dia_semana} onChange={(e) => cambiar('dia_semana', e)}>
              {DIAS.map((d, i) => (
                <option key={d} value={i}>
                  {d}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Inicio</span>
            <input
              type="time"
              value={form.hora_inicio}
              onChange={(e) => cambiar('hora_inicio', e)}
              required
            />
          </label>
          <label>
            <span>Fin</span>
            <input
              type="time"
              value={form.hora_fin}
              onChange={(e) => cambiar('hora_fin', e)}
              required
            />
          </label>
          <label>
            <span>Docente</span>
            <select value={form.docente} onChange={(e) => cambiar('docente', e)} required>
              <option value="">Selecciona...</option>
              {docentes.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.first_name} {d.last_name}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Aula</span>
            <select value={form.aula} onChange={(e) => cambiar('aula', e)} required>
              <option value="">Selecciona...</option>
              {aulas.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.nombre}
                </option>
              ))}
            </select>
          </label>
          <div className="acciones">
            <button type="submit">{editandoId ? 'Actualizar' : 'Agregar'}</button>
            {editandoId && (
              <button type="button" className="secundario" onClick={limpiar}>
                Cancelar
              </button>
            )}
          </div>
        </form>
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
        <div className="semana">
          {DIAS.map((dia, indice) => (
            <div key={dia} className="dia">
              <h3>{dia}</h3>
              {horarios
                .filter((h) => h.dia_semana === indice)
                .map((h) => (
                  <div key={h.id} className="bloque">
                    <strong>
                      {corta(h.hora_inicio)} - {corta(h.hora_fin)}
                    </strong>
                    <span>{nombreDocente(h.docente)}</span>
                    <span>Aula {nombreAula(h.aula)}</span>
                    <div className="celda-acciones">
                      <button type="button" className="secundario" onClick={() => editar(h)}>
                        Editar
                      </button>
                      <button type="button" className="peligro" onClick={() => quitar(h)}>
                        Quitar
                      </button>
                    </div>
                  </div>
                ))}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
