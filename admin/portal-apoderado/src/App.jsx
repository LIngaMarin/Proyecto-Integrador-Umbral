import { useEffect, useState } from 'react'
import { api, CLAVES, cerrarSesionLocal } from './api'
import { aplicarMarca } from './marca'
import CambiarClave from './components/CambiarClave'
import Login from './components/Login'
import './App.css'

const primerNombre = (nombre) => (nombre || '').split(' ')[0]

export default function App() {
  const [sesion, setSesion] = useState(() =>
    localStorage.getItem(CLAVES.token)
      ? {
          nombre: localStorage.getItem(CLAVES.nombre),
          cambiar: localStorage.getItem(CLAVES.cambiar) === '1',
        }
      : null,
  )
  const [academia, setAcademia] = useState(null)
  const [hijos, setHijos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get('/marca/')
      .then((r) => aplicarMarca(r.data))
      .catch(() => {})
    api
      .get('/logo/')
      .then((r) => setAcademia(r.data))
      .catch(() => {})
  }, [])

  const entrado = !!sesion && !sesion.cambiar
  useEffect(() => {
    if (!entrado) return
    api
      .get('/mis-hijos/')
      .then((r) => setHijos(r.data))
      .catch(() => setError('No se pudo cargar la información de tus hijos.'))
  }, [entrado])

  const salir = () => {
    cerrarSesionLocal()
    setHijos(null)
    setError('')
    setSesion(null)
  }

  if (!sesion) return <Login onLogin={setSesion} />

  if (sesion.cambiar) {
    return (
      <CambiarClave
        onListo={() => {
          localStorage.removeItem(CLAVES.cambiar)
          setSesion({ ...sesion, cambiar: false })
        }}
        onSalir={salir}
      />
    )
  }

  return (
    <>
      <header className="cabecera">
        <div className="cabecera-marca">
          {academia?.logo && <img src={academia.logo} alt="" />}
          <div>
            <strong>Umbral</strong>
            {academia?.nombre && <span>{academia.nombre}</span>}
          </div>
        </div>
        <div className="cabecera-usuario">
          <span>{sesion.nombre}</span>
          <button type="button" className="secundario" onClick={salir}>
            Salir
          </button>
        </div>
      </header>

      <main className="contenedor">
        <h1>Hola, {primerNombre(sesion.nombre)}</h1>
        <p className="suave">Aquí puedes seguir el avance de tus hijos en la academia.</p>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {!hijos && !error && <p className="suave">Cargando...</p>}
        {hijos && hijos.length === 0 && (
          <p className="tarjeta vacio">
            Todavía no tienes hijos vinculados a tu cuenta. Pide a la academia que los vincule.
          </p>
        )}
        {hijos && hijos.length > 0 && (
          <ul className="hijos">
            {hijos.map((h) => (
              <li className="tarjeta" key={h.id}>
                <strong>
                  {h.first_name} {h.last_name}
                </strong>
                <span className="suave">{h.email}</span>
              </li>
            ))}
          </ul>
        )}
      </main>
    </>
  )
}
