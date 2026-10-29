import { useEffect, useState } from 'react'
import { api } from './api'
import { aplicarMarca } from './marca'
import GestionRecurso from './components/GestionRecurso'
import Login from './components/Login'
import CambiarClave from './components/CambiarClave'
import Dashboard from './components/Dashboard'
import Horarios from './components/Horarios'
import Marca from './components/Marca'
import Rendimiento from './components/Rendimiento'
import RestablecerClave from './components/RestablecerClave'
import Apoderados from './components/Apoderados'
import ConfiguracionIA from './components/ConfiguracionIA'
import CargaMasiva from './components/CargaMasiva'
import Matriculas from './components/Matriculas'
import './App.css'

const ACTIVA = { nombre: 'activa', etiqueta: 'Activa', tipo: 'checkbox' }
const ACTIVO = { nombre: 'activo', etiqueta: 'Activo', tipo: 'checkbox' }
const ACADEMIA = {
  nombre: 'academia',
  etiqueta: 'Academia',
  tipo: 'select',
  requerido: true,
  relacion: { endpoint: 'academias', mostrar: 'nombre' },
}
const ROLES = [
  { valor: 'ADMIN', etiqueta: 'Administrador' },
  { valor: 'COORDINADOR', etiqueta: 'Coordinador académico' },
  { valor: 'DOCENTE', etiqueta: 'Docente' },
  { valor: 'ALUMNO', etiqueta: 'Alumno' },
  { valor: 'APODERADO', etiqueta: 'Apoderado' },
]

const SECCIONES = [
  { clave: 'panel', titulo: 'Panel', campos: [] },
  {
    clave: 'academias',
    titulo: 'Academias',
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      { nombre: 'logo', etiqueta: 'Logo', tipo: 'file' },
      ACTIVA,
    ],
  },
  {
    clave: 'sedes',
    titulo: 'Sedes',
    campos: [
      ACADEMIA,
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      { nombre: 'direccion', etiqueta: 'Dirección', tipo: 'text', requerido: true },
      { nombre: 'contacto', etiqueta: 'Contacto', tipo: 'text' },
      ACTIVA,
    ],
  },
  {
    clave: 'aulas',
    titulo: 'Aulas',
    campos: [
      {
        nombre: 'sede',
        etiqueta: 'Sede',
        tipo: 'select',
        requerido: true,
        relacion: { endpoint: 'sedes', mostrar: 'nombre' },
      },
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      { nombre: 'capacidad', etiqueta: 'Capacidad', tipo: 'number', requerido: true },
      ACTIVA,
    ],
  },
  {
    clave: 'ciclos',
    titulo: 'Ciclos',
    campos: [
      ACADEMIA,
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      { nombre: 'fecha_inicio', etiqueta: 'Inicio', tipo: 'date', requerido: true },
      { nombre: 'fecha_fin', etiqueta: 'Fin', tipo: 'date', requerido: true },
      ACTIVO,
    ],
  },
  {
    clave: 'festivos',
    titulo: 'Festivos',
    campos: [
      ACADEMIA,
      { nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true },
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      ACTIVO,
    ],
  },
  {
    clave: 'cursos',
    titulo: 'Cursos',
    campos: [
      {
        nombre: 'ciclo',
        etiqueta: 'Ciclo',
        tipo: 'select',
        requerido: true,
        relacion: { endpoint: 'ciclos', mostrar: 'nombre' },
      },
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      ACTIVO,
    ],
  },
  {
    clave: 'secciones',
    titulo: 'Secciones',
    campos: [
      {
        nombre: 'curso',
        etiqueta: 'Curso',
        tipo: 'select',
        requerido: true,
        relacion: { endpoint: 'cursos', mostrar: 'nombre' },
      },
      { nombre: 'nombre', etiqueta: 'Sección', tipo: 'text', requerido: true },
      {
        nombre: 'aula',
        etiqueta: 'Aula',
        tipo: 'select',
        relacion: { endpoint: 'aulas', mostrar: 'nombre' },
      },
      { nombre: 'cupo', etiqueta: 'Cupo', tipo: 'number', requerido: true },
      ACTIVA,
    ],
  },
  { clave: 'matriculas', titulo: 'Matrículas', campos: [] },
  { clave: 'horarios', titulo: 'Horarios', campos: [] },
  { clave: 'marca', titulo: 'Marca', campos: [], soloAdmin: true },
  { clave: 'rendimiento', titulo: 'Rendimiento', campos: [] },
  { clave: 'claves', titulo: 'Contraseñas', campos: [], soloAdmin: true },
  { clave: 'apoderados', titulo: 'Apoderados', campos: [], soloAdmin: true },
  { clave: 'ia', titulo: 'Inteligencia artificial', campos: [], soloAdmin: true },
  { clave: 'carga', titulo: 'Carga masiva', campos: [], soloAdmin: true },
  {
    clave: 'simulacros',
    titulo: 'Simulacros',
    campos: [
      {
        nombre: 'curso',
        etiqueta: 'Curso',
        tipo: 'select',
        requerido: true,
        relacion: { endpoint: 'cursos', mostrar: 'nombre' },
      },
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
      { nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true },
      ACTIVA,
    ],
  },
  {
    clave: 'resultados',
    titulo: 'Resultados',
    campos: [
      {
        nombre: 'simulacro',
        etiqueta: 'Simulacro',
        tipo: 'select',
        requerido: true,
        relacion: { endpoint: 'simulacros', mostrar: 'nombre' },
      },
      {
        nombre: 'alumno',
        etiqueta: 'Alumno',
        tipo: 'select',
        requerido: true,
        relacion: { endpoint: 'alumnos', mostrar: 'email' },
      },
      { nombre: 'puntaje', etiqueta: 'Puntaje (0 a 20)', tipo: 'number', paso: 'any', requerido: true },
    ],
  },
  {
    clave: 'usuarios',
    titulo: 'Usuarios',
    soloAdmin: true,
    campos: [
      { nombre: 'first_name', etiqueta: 'Nombres', tipo: 'text', requerido: true },
      { nombre: 'last_name', etiqueta: 'Apellidos', tipo: 'text', requerido: true },
      { nombre: 'email', etiqueta: 'Correo', tipo: 'email', requerido: true },
      {
        nombre: 'password',
        etiqueta: 'Contraseña',
        tipo: 'password',
        requeridoAlCrear: true,
        oculto: true,
      },
      { nombre: 'rol', etiqueta: 'Rol', tipo: 'select', requerido: true, opciones: ROLES },
      {
        nombre: 'sede',
        etiqueta: 'Sede',
        tipo: 'select',
        relacion: { endpoint: 'sedes', mostrar: 'nombre' },
      },
      ACTIVO,
    ],
  },
]

export default function App() {
  const [sesion, setSesion] = useState(() => {
    const token = localStorage.getItem('umbral_token')
    return token
      ? {
          nombre: localStorage.getItem('umbral_nombre'),
          rol: localStorage.getItem('umbral_rol'),
          cambiar: localStorage.getItem('umbral_cambiar') === '1',
        }
      : null
  })
  const [clave, setClave] = useState('panel')
  const [logo, setLogo] = useState(null)

  useEffect(() => {
    if (!sesion) return
    api
      .get('/academias/')
      .then((r) => {
        const conLogo = r.data.find((a) => a.logo && a.activa)
        setLogo(conLogo ? conLogo.logo : null)
      })
      .catch(() => {})
  }, [sesion, clave])

  useEffect(() => {
    api
      .get('/marca/')
      .then((r) => aplicarMarca(r.data))
      .catch(() => {})
  }, [])

  if (!sesion) return <Login onLogin={setSesion} />

  const secciones = SECCIONES.filter((s) => !s.soloAdmin || sesion.rol === 'ADMIN')
  const actual = secciones.find((s) => s.clave === clave) || secciones[0]

  const salir = () => {
    localStorage.removeItem('umbral_token')
    localStorage.removeItem('umbral_nombre')
    localStorage.removeItem('umbral_rol')
    localStorage.removeItem('umbral_cambiar')
    setSesion(null)
  }

  if (sesion.cambiar) {
    return (
      <CambiarClave
        onListo={() => {
          localStorage.removeItem('umbral_cambiar')
          setSesion({ ...sesion, cambiar: false })
        }}
        onSalir={salir}
      />
    )
  }

  return (
    <div className="app">
      <header className="encabezado">
        <div className="marca">
          {logo && <img className="logo-encabezado" src={logo} alt="Logo de la academia" />}
          <div>
            <h1>Umbral · Panel de administración</h1>
            <p>Infraestructura académica</p>
          </div>
        </div>
        <div className="sesion">
          <span>{sesion.nombre}</span>
          <button className="secundario" onClick={salir}>
            Salir
          </button>
        </div>
      </header>
      <nav>
        {secciones.map((s) => (
          <button
            key={s.clave}
            className={s.clave === actual.clave ? 'activa' : ''}
            onClick={() => setClave(s.clave)}
          >
            {s.titulo}
          </button>
        ))}
      </nav>
      {actual.clave === 'panel' ? (
        <Dashboard />
      ) : actual.clave === 'matriculas' ? (
        <Matriculas />
      ) : actual.clave === 'horarios' ? (
        <Horarios />
      ) : actual.clave === 'marca' ? (
        <Marca />
      ) : actual.clave === 'rendimiento' ? (
        <Rendimiento />
      ) : actual.clave === 'claves' ? (
        <RestablecerClave />
      ) : actual.clave === 'apoderados' ? (
        <Apoderados />
      ) : actual.clave === 'ia' ? (
        <ConfiguracionIA />
      ) : actual.clave === 'carga' ? (
        <CargaMasiva />
      ) : (
        <GestionRecurso
          key={actual.clave}
          titulo={actual.titulo}
          endpoint={actual.clave}
          campos={actual.campos}
        />
      )}
    </div>
  )
}
