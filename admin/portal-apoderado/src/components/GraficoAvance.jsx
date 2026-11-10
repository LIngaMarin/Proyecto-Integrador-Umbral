const ANCHO = 320
const ALTO = 120
const MARGEN = { izq: 28, der: 12, arriba: 10, abajo: 18 }
const NOTA_MAXIMA = 20

// Línea de puntajes (de 0 a 20) en el orden en que se dieron los simulacros.
export default function GraficoAvance({ resultados }) {
  const largo = ANCHO - MARGEN.izq - MARGEN.der
  const alto = ALTO - MARGEN.arriba - MARGEN.abajo
  const x = (i) =>
    MARGEN.izq + (resultados.length === 1 ? largo / 2 : (i * largo) / (resultados.length - 1))
  const y = (nota) => MARGEN.arriba + alto - (nota / NOTA_MAXIMA) * alto

  const puntos = resultados.map((r, i) => `${x(i)},${y(r.puntaje)}`).join(' ')
  const descripcion = resultados.map((r) => `${r.simulacro}: ${r.puntaje}`).join('; ')

  return (
    <svg
      className="grafico"
      viewBox={`0 0 ${ANCHO} ${ALTO}`}
      role="img"
      aria-label={`Avance entre simulacros. ${descripcion}`}
    >
      {[0, 10, 20].map((nota) => (
        <g key={nota}>
          <line
            x1={MARGEN.izq}
            x2={ANCHO - MARGEN.der}
            y1={y(nota)}
            y2={y(nota)}
            className="grafico-guia"
          />
          <text x={MARGEN.izq - 6} y={y(nota) + 4} textAnchor="end" className="grafico-nota">
            {nota}
          </text>
        </g>
      ))}
      {resultados.length > 1 && <polyline points={puntos} className="grafico-linea" />}
      {resultados.map((r, i) => (
        <circle key={r.simulacro_id} cx={x(i)} cy={y(r.puntaje)} r="4" className="grafico-punto">
          <title>{`${r.simulacro}: ${r.puntaje}`}</title>
        </circle>
      ))}
    </svg>
  )
}
