import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

const vacio = (campos) =>
  Object.fromEntries(
    campos.map((c) => [c.nombre, c.tipo === 'checkbox' ? true : '']),
  )

export default function GestionRecurso({ titulo, endpoint, campos }) {
  const [items, setItems] = useState([])
  const [opciones, setOpciones] = useState({})
  const [form, setForm] = useState(() => vacio(campos))
  const [editandoId, setEditandoId] = useState(null)
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  const cargar = useCallback(async () => {
    try {
      const { data } = await api.get(`/${endpoint}/`)
      setItems(data)
    } catch {
      setErrores({ error: 'No se pudo conectar con el servidor' })
    }
  }, [endpoint])

  useEffect(() => {
    cargar()
    campos
      .filter((c) => c.relacion)
      .forEach(async (c) => {
        try {
          const { data } = await api.get(`/${c.relacion.endpoint}/`)
          setOpciones((prev) => ({ ...prev, [c.nombre]: data }))
        } catch {
          setErrores({ error: 'No se pudo cargar las opciones' })
        }
      })
  }, [cargar, campos])

  const cambiar = (campo, e) => {
    const valor = campo.tipo === 'checkbox' ? e.target.checked : e.target.value
    setForm((prev) => ({ ...prev, [campo.nombre]: valor }))
  }

  const limpiar = () => {
    setForm(vacio(campos))
    setEditandoId(null)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setErrores(null)
    setMensaje('')
    const datos = { ...form }
    campos.forEach((c) => {
      if (datos[c.nombre] === '' && !c.requerido) delete datos[c.nombre]
    })
    try {
      if (editandoId) {
        await api.put(`/${endpoint}/${editandoId}/`, datos)
      } else {
        await api.post(`/${endpoint}/`, datos)
      }
      limpiar()
      setMensaje('Guardado correctamente')
      cargar()
    } catch (err) {
      const data = err.response?.data
      setErrores(
        data && typeof data === 'object'
          ? data
          : { error: 'No se pudo guardar. Revisa la conexión con el servidor.' },
      )
    }
  }

  const editar = (item) => {
    setEditandoId(item.id)
    setForm(Object.fromEntries(campos.map((c) => [c.nombre, item[c.nombre] ?? ''])))
    setErrores(null)
    setMensaje('')
  }

  const desactivar = async (item) => {
    await api.delete(`/${endpoint}/${item.id}/`)
    cargar()
  }

  const celda = (campo, item) => {
    const valor = item[campo.nombre]
    if (campo.tipo === 'checkbox') return valor ? 'Sí' : 'No'
    if (campo.relacion) {
      const opcion = (opciones[campo.nombre] || []).find((o) => o.id === valor)
      return opcion ? opcion[campo.relacion.mostrar] : valor
    }
    return valor
  }

  return (
    <section className="panel">
      <h2>{titulo}</h2>

      <form className="formulario" onSubmit={guardar}>
        {campos.map((c) => (
          <label key={c.nombre} className={c.tipo === 'checkbox' ? 'check' : ''}>
            <span>{c.etiqueta}</span>
            {c.tipo === 'select' ? (
              <select
                value={form[c.nombre]}
                onChange={(e) => cambiar(c, e)}
                required={c.requerido}
              >
                <option value="">Selecciona...</option>
                {(opciones[c.nombre] || []).map((o) => (
                  <option key={o.id} value={o.id}>
                    {o[c.relacion.mostrar]}
                  </option>
                ))}
              </select>
            ) : c.tipo === 'checkbox' ? (
              <input
                type="checkbox"
                checked={Boolean(form[c.nombre])}
                onChange={(e) => cambiar(c, e)}
              />
            ) : (
              <input
                type={c.tipo}
                value={form[c.nombre]}
                onChange={(e) => cambiar(c, e)}
                required={c.requerido}
              />
            )}
          </label>
        ))}
        <div className="acciones">
          <button type="submit">{editandoId ? 'Actualizar' : 'Crear'}</button>
          {editandoId && (
            <button type="button" className="secundario" onClick={limpiar}>
              Cancelar
            </button>
          )}
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

      <table>
        <thead>
          <tr>
            {campos.map((c) => (
              <th key={c.nombre}>{c.etiqueta}</th>
            ))}
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {items.length === 0 && (
            <tr>
              <td colSpan={campos.length + 1}>Aún no hay registros.</td>
            </tr>
          )}
          {items.map((item) => (
            <tr key={item.id}>
              {campos.map((c) => (
                <td key={c.nombre}>{celda(c, item)}</td>
              ))}
              <td className="celda-acciones">
                <button type="button" className="secundario" onClick={() => editar(item)}>
                  Editar
                </button>
                <button type="button" className="peligro" onClick={() => desactivar(item)}>
                  Desactivar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
