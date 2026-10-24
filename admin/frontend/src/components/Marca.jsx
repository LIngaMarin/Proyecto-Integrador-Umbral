import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import { aplicarMarca } from '../marca'

export default function Marca() {
  const [form, setForm] = useState({
    color_primario: '#2563EB',
    color_secundario: '#1F2937',
  })
  const [publicado, setPublicado] = useState(null)
  const [hayBorrador, setHayBorrador] = useState(false)
  const [errores, setErrores] = useState(null)
  const [mensaje, setMensaje] = useState('')

  const cargar = useCallback(async () => {
    try {
      const [b, p] = await Promise.all([api.get('/marca/borrador/'), api.get('/marca/')])
      setPublicado(p.data)
      setHayBorrador(b.data.hay_borrador)
      setForm({
        color_primario: b.data.color_primario,
        color_secundario: b.data.color_secundario,
      })
    } catch {
      setErrores({ error: 'No se pudo cargar la marca' })
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  // Vista previa en vivo: el panel muestra los colores que se están eligiendo.
  useEffect(() => {
    aplicarMarca(form)
  }, [form])

  // Al salir de la pestaña, el panel vuelve a los colores publicados.
  useEffect(
    () => () => {
      api
        .get('/marca/')
        .then((r) => aplicarMarca(r.data))
        .catch(() => {})
    },
    [],
  )

  const cambiar = (campo, e) =>
    setForm((prev) => ({ ...prev, [campo]: e.target.value.toUpperCase() }))

  const mostrarError = (err) => {
    const data = err.response?.data
    setErrores(
      data && typeof data === 'object'
        ? data
        : { error: 'No se pudo completar la operación.' },
    )
  }

  const accion = async (hacer, texto) => {
    setErrores(null)
    setMensaje('')
    try {
      await hacer()
      await cargar()
      setMensaje(texto)
    } catch (err) {
      mostrarError(err)
    }
  }

  const guardarBorrador = (e) => {
    e.preventDefault()
    accion(
      () => api.put('/marca/borrador/', form),
      'Borrador guardado. Aún no está publicado.',
    )
  }

  const publicar = () =>
    accion(async () => {
      await api.put('/marca/borrador/', form)
      await api.post('/marca/publicar/')
    }, 'Colores publicados correctamente.')

  const descartar = () =>
    accion(() => api.delete('/marca/borrador/'), 'Borrador descartado.')

  const cambiosSinPublicar =
    publicado &&
    (form.color_primario !== publicado.color_primario ||
      form.color_secundario !== publicado.color_secundario)

  return (
    <section className="panel">
      <h2>Marca de la academia</h2>

      {cambiosSinPublicar && (
        <p className="aviso">
          Estás viendo una vista previa. Los alumnos y docentes seguirán viendo los
          colores publicados hasta que pulses Publicar.
        </p>
      )}

      {publicado && (
        <p className="publicado">
          Colores publicados:
          <i className="swatch" style={{ background: publicado.color_primario }} />
          {publicado.color_primario}
          <i className="swatch" style={{ background: publicado.color_secundario }} />
          {publicado.color_secundario}
          {hayBorrador && ' · hay un borrador guardado'}
        </p>
      )}

      <form className="formulario" onSubmit={guardarBorrador}>
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
          <button type="submit" className="secundario">
            Guardar borrador
          </button>
          <button type="button" onClick={publicar}>
            Publicar
          </button>
          <button type="button" className="peligro" onClick={descartar}>
            Descartar borrador
          </button>
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
