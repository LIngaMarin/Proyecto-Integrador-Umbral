import { useEffect, useState } from 'react'
import { api } from '../api'
const INDICADORES = [
  { clave: 'sedes_activas', etiqueta: 'Sedes activas' },
  { clave: 'aulas_activas', etiqueta: 'Aulas activas' },
  { clave: 'ciclos_activos', etiqueta: 'Ciclos activos' },
  { clave: 'cursos_activos', etiqueta: 'Cursos activos' },
  { clave: 'docentes_activos', etiqueta: 'Docentes activos' },
  { clave: 'alumnos_matriculados', etiqueta: 'Alumnos matriculados' },
]

export default function Dashboard() {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get('/dashboard/')
      .then((r) => setDatos(r.data))
      .catch(() => setError('No se pudieron cargar los indicadores.'))
  }, [])

  if (error) return <p className="error">{error}</p>
  if (!datos) return <p>Cargando indicadores...</p>

  return (
    <section>
      <h2>Panel de indicadores</h2>
      <div className="tarjetas">
        {INDICADORES.map((i) => (
          <div key={i.clave} className="tarjeta">
            <span className="tarjeta-valor">{datos[i.clave]}</span>
            <span className="tarjeta-etiqueta">{i.etiqueta}</span>
          </div>
        ))}
      </div>
    </section>
  )
}
