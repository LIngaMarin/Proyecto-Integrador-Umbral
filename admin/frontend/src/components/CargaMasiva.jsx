import { useState } from 'react'
import { api } from '../api'

export default function CargaMasiva() {
  const [archivo, setArchivo] = useState(null)
  const [clave, setClave] = useState(0)
  const [cargando, setCargando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState('')

  const bajarPlantilla = async () => {
    setError('')
    try {
      const r = await api.get('/alumnos-masivo/plantilla/', { responseType: 'blob' })
      const url = URL.createObjectURL(r.data)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = 'plantilla_alumnos.xlsx'
      enlace.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('No se pudo descargar la plantilla.')
    }
  }

  const cargar = async (e) => {
    e.preventDefault()
    if (!archivo) return
    setError('')
    setResultado(null)
    setCargando(true)
    try {
      const datos = new FormData()
      datos.append('archivo', archivo)
      const r = await api.post('/alumnos-masivo/cargar/', datos)
      setResultado(r.data)
      setArchivo(null)
      setClave((k) => k + 1)
    } catch (err) {
      const detalle = err.response?.data?.archivo
      setError(detalle ? [].concat(detalle).join(' ') : 'No se pudo cargar el archivo.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="panel">
      <h2>Carga masiva de alumnos</h2>
      <p>
        Descarga la plantilla, complétala con un alumno por fila y súbela. Cada alumno se
        matricula en su sección y recibe una contraseña temporal por correo.
      </p>

      <div className="acciones">
        <button type="button" className="secundario" onClick={bajarPlantilla}>
          Descargar plantilla
        </button>
      </div>

      <form className="formulario" onSubmit={cargar}>
        <label>
          <span>Archivo Excel (.xlsx, hasta 2 MB y 500 alumnos)</span>
          <input
            key={clave}
            type="file"
            accept=".xlsx"
            onChange={(e) => setArchivo(e.target.files[0] || null)}
          />
        </label>
        <div className="acciones">
          <button type="submit" disabled={!archivo || cargando}>
            {cargando ? 'Cargando...' : 'Cargar alumnos'}
          </button>
        </div>
      </form>

      {error && <p className="aviso">{error}</p>}

      {resultado && (
        <>
          <div className="resumen-carga">
            <div>
              <strong>{resultado.total}</strong>
              <span>Filas leídas</span>
            </div>
            <div>
              <strong>{resultado.creados}</strong>
              <span>Alumnos creados</span>
            </div>
            <div>
              <strong>{resultado.con_errores}</strong>
              <span>Filas con error</span>
            </div>
          </div>

          {resultado.sin_correo.length > 0 && (
            <p className="aviso">
              Se crearon, pero no se pudo enviar el correo a: {resultado.sin_correo.join(', ')}.
            </p>
          )}

          {resultado.errores.length > 0 && (
            <table className="tabla-claves">
              <thead>
                <tr>
                  <th>Fila</th>
                  <th>Correo</th>
                  <th>Motivo</th>
                </tr>
              </thead>
              <tbody>
                {resultado.errores.map((f) => (
                  <tr key={f.fila}>
                    <td>{f.fila}</td>
                    <td>{f.correo || '—'}</td>
                    <td>{f.errores.join(' ')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  )
}
