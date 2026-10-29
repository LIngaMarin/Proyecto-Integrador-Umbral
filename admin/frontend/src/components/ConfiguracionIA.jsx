import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

export default function ConfiguracionIA() {
  const [academias, setAcademias] = useState([])
  const [academia, setAcademia] = useState('')
  const [form, setForm] = useState({ activa: true, limite_por_docente: 100 })
  const [docentes, setDocentes] = useState([])
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  useEffect(() => {
    api
      .get('/academias/')
      .then((r) => {
        const lista = (r.data.results || r.data).filter((a) => a.activa)
        setAcademias(lista)
        if (lista.length > 0) setAcademia(String(lista[0].id))
      })
      .catch(() => setErrores({ error: 'No se pudieron cargar las academias.' }))
  }, [])

  const cargar = useCallback(async (id) => {
    if (!id) return
    try {
      const [c, u] = await Promise.all([
        api.get(`/configuracion-ia/?academia=${id}`),
        api.get(`/ia/uso-docentes/?academia=${id}`),
      ])
      setForm({ activa: c.data.activa, limite_por_docente: c.data.limite_por_docente })
      setDocentes(u.data)
    } catch {
      setErrores({ error: 'No se pudo cargar la configuración de IA.' })
    }
  }, [])

  useEffect(() => {
    cargar(academia)
  }, [academia, cargar])

  const guardar = async (e) => {
    e.preventDefault()
    setErrores(null)
    setMensaje('')
    try {
      await api.put(`/configuracion-ia/?academia=${academia}`, {
        activa: form.activa,
        limite_por_docente: Number(form.limite_por_docente),
      })
      setMensaje('Configuración de IA guardada.')
      await cargar(academia)
    } catch (err) {
      const data = err.response?.data
      setErrores(
        data && typeof data === 'object' ? data : { error: 'No se pudo guardar la configuración.' },
      )
    }
  }

  return (
    <section className="panel">
      <h2>Uso de inteligencia artificial</h2>

      <form className="formulario" onSubmit={guardar}>
        <label>
          <span>Academia</span>
          <select value={academia} onChange={(e) => setAcademia(e.target.value)}>
            {academias.map((a) => (
              <option key={a.id} value={a.id}>
                {a.nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="opcion-hijo">
          <input
            type="checkbox"
            checked={form.activa}
            onChange={(e) => setForm((prev) => ({ ...prev, activa: e.target.checked }))}
          />
          IA activada en esta academia
        </label>
        <label>
          <span>Límite de usos por docente (cada mes)</span>
          <input
            type="number"
            min="0"
            value={form.limite_por_docente}
            onChange={(e) => setForm((prev) => ({ ...prev, limite_por_docente: e.target.value }))}
            required
          />
        </label>
        <div className="acciones">
          <button type="submit" disabled={!academia}>
            Guardar configuración
          </button>
        </div>
      </form>

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

      <h3>Uso de este mes por docente</h3>
      <table className="tabla-claves">
        <thead>
          <tr>
            <th>Docente</th>
            <th>Correo</th>
            <th>Usos</th>
            <th>Límite</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {docentes.length === 0 && (
            <tr>
              <td colSpan="5">No hay docentes asignados a una sede de esta academia.</td>
            </tr>
          )}
          {docentes.map((d) => (
            <tr key={d.id}>
              <td>{d.nombre}</td>
              <td>{d.email}</td>
              <td>{d.usados}</td>
              <td>{d.limite}</td>
              <td>
                {d.usados >= d.limite && (
                  <span className="limite-alcanzado">Límite alcanzado</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
