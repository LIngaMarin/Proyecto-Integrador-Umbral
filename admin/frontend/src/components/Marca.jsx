import { useEffect, useState } from 'react'
import { api } from '../api'
import { aplicarMarca } from '../marca'

export default function Marca() {
  const [form, setForm] = useState({
    color_primario: '#2563EB',
    color_secundario: '#1F2937',
  })
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  useEffect(() => {
    api
      .get('/marca/')
      .then((r) =>
        setForm({
          color_primario: r.data.color_primario,
          color_secundario: r.data.color_secundario,
        }),
      )
      .catch(() => setErrores({ error: 'No se pudo cargar la marca' }))
  }, [])

  const cambiar = (campo, e) =>
    setForm((prev) => ({ ...prev, [campo]: e.target.value.toUpperCase() }))

  const guardar = async (e) => {
    e.preventDefault()
    setErrores(null)
    setMensaje('')
    try {
      const { data } = await api.put('/marca/', form)
      aplicarMarca(data)
      setMensaje('Colores guardados correctamente')
    } catch (err) {
      const data = err.response?.data
      setErrores(
        data && typeof data === 'object'
          ? data
          : { error: 'No se pudo guardar. Revisa la conexión con el servidor.' },
      )
    }
  }

  return (
    <section className="panel">
      <h2>Marca de la academia</h2>

      <form className="formulario" onSubmit={guardar}>
        <label>
          <span>Color primario</span>
          <input
            type="color"
            value={form.color_primario.toLowerCase()}
            onChange={(e) => cambiar('color_primario', e)}
          />
        </label>
        <label>
          <span>Color secundario</span>
          <input
            type="color"
            value={form.color_secundario.toLowerCase()}
            onChange={(e) => cambiar('color_secundario', e)}
          />
        </label>
        <div className="acciones">
          <button type="submit">Guardar colores</button>
        </div>
      </form>

      <div className="muestra" style={{ borderColor: form.color_primario }}>
        <strong style={{ color: form.color_primario }}>Así se verá el título</strong>
        <span style={{ color: form.color_secundario }}>Y así el texto secundario</span>
        <button type="button" style={{ background: form.color_primario, color: '#ffffff' }}>
          Botón de ejemplo
        </button>
      </div>

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
    </section>
  )
}
