import { useState } from 'react'
import GestionRecurso from './components/GestionRecurso'
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

const SECCIONES = [
  {
    clave: 'academias',
    titulo: 'Academias',
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', requerido: true },
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
]

export default function App() {
  const [clave, setClave] = useState('academias')
  const actual = SECCIONES.find((s) => s.clave === clave)

  return (
    <div className="app">
      <header>
        <h1>Umbral · Panel de administración</h1>
        <p>Infraestructura académica</p>
      </header>
      <nav>
        {SECCIONES.map((s) => (
          <button
            key={s.clave}
            className={s.clave === clave ? 'activa' : ''}
            onClick={() => setClave(s.clave)}
          >
            {s.titulo}
          </button>
        ))}
      </nav>
      <GestionRecurso
        key={actual.clave}
        titulo={actual.titulo}
        endpoint={actual.clave}
        campos={actual.campos}
      />
    </div>
  )
}
