import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

const VACIO = {
  id: null,
  first_name: '',
  last_name: '',
  email: '',
  password: '',
  telefono: '',
  hijos: [],
}

export default function Apoderados() {
  const [apoderados, setApoderados] = useState([])
  const [alumnos, setAlumnos] = useState([])
  const [form, setForm] = useState(VACIO)
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  const cargar = useCallback(async () => {
    try {
      const [a, h] = await Promise.all([api.get('/apoderados/'), api.get('/alumnos/')])
      setApoderados(a.data.results || a.data)
      setAlumnos(h.data.results || h.data)
    } catch {
      setErrores({ error: 'No se pudo cargar la información.' })
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  const nombreAlumno = (id) => {
    const a = alumnos.find((x) => x.id === id)
    return a ? `${a.first_name} ${a.last_name}`.trim() || a.email : `#${id}`
  }

  const cambiar = (campo) => (e) => setForm((prev) => ({ ...prev, [campo]: e.target.value }))

  const alternarHijo = (id) =>
    setForm((prev) => ({
      ...prev,
      hijos: prev.hijos.includes(id)
        ? prev.hijos.filter((h) => h !== id)
        : [...prev.hijos, id],
    }))

  const editar = (a) => {
    setErrores(null)
    setMensaje('')
    setForm({
      id: a.id,
      first_name: a.first_name,
      last_name: a.last_name,
      email: a.email,
      password: '',
      telefono: a.telefono || '',
      hijos: a.hijos,
    })
  }

  const guardar = async (e) => {
    e.preventDefault()
    setErrores(null)
    setMensaje('')
    const datos = {
      first_name: form.first_name,
      last_name: form.last_name,
      email: form.email,
      telefono: form.telefono,
      hijos: form.hijos,
    }
    if (form.password) datos.password = form.password
    try {
      if (form.id) {
        await api.patch(`/apoderados/${form.id}/`, datos)
        setMensaje('Apoderado actualizado.')
      } else {
        await api.post('/apoderados/', datos)
        setMensaje('Apoderado registrado.')
      }
      setForm(VACIO)
      await cargar()
    } catch (err) {
      const data = err.response?.data
      setErrores(
        data && typeof data === 'object' ? data : { error: 'No se pudo guardar el apoderado.' },
      )
    }
  }

  const desactivar = async (a) => {
    if (!window.confirm(`¿Desactivar al apoderado ${a.email}?`)) return
    try {
      await api.delete(`/apoderados/${a.id}/`)
      setMensaje('Apoderado desactivado.')
      await cargar()
    } catch {
      setErrores({ error: 'No se pudo desactivar el apoderado.' })
    }
  }

  return (
    <section className="panel">
      <h2>Apoderados</h2>

      <form className="formulario" onSubmit={guardar}>
        <label>
          <span>Nombres</span>
          <input value={form.first_name} onChange={cambiar('first_name')} required />
        </label>
        <label>
          <span>Apellidos</span>
          <input value={form.last_name} onChange={cambiar('last_name')} required />
        </label>
        <label>
          <span>Correo</span>
          <input type="email" value={form.email} onChange={cambiar('email')} required />
        </label>
        <label>
          <span>Teléfono</span>
          <input value={form.telefono} onChange={cambiar('telefono')} />
        </label>
        <label>
          <span>{form.id ? 'Contraseña nueva (opcional)' : 'Contraseña'}</span>
          <input
            type="password"
            value={form.password}
            onChange={cambiar('password')}
            required={!form.id}
            minLength={8}
          />
        </label>

        <fieldset className="lista-hijos">
          <legend>Hijos vinculados</legend>
          {alumnos.length === 0 && <p>No hay alumnos registrados.</p>}
          {alumnos.map((al) => (
            <label key={al.id} className="opcion-hijo">
              <input
                type="checkbox"
                checked={form.hijos.includes(al.id)}
                onChange={() => alternarHijo(al.id)}
              />
              {`${al.first_name} ${al.last_name}`.trim()} · {al.email}
            </label>
          ))}
        </fieldset>

        <div className="acciones">
          <button type="submit">{form.id ? 'Guardar cambios' : 'Registrar apoderado'}</button>
          {form.id && (
            <button type="button" className="secundario" onClick={() => setForm(VACIO)}>
              Cancelar edición
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

      <table className="tabla-claves">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Teléfono</th>
            <th>Hijos</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {apoderados.map((a) => (
            <tr key={a.id}>
              <td>{`${a.first_name} ${a.last_name}`}</td>
              <td>{a.email}</td>
              <td>{a.telefono}</td>
              <td>{a.hijos.map(nombreAlumno).join(', ') || '—'}</td>
              <td className="acciones">
                <button type="button" onClick={() => editar(a)}>
                  Editar
                </button>
                <button type="button" className="peligro" onClick={() => desactivar(a)}>
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
