from .models import Resultado


def resultados_por_curso(hijo):
    """Resultados de simulacros de un alumno, agrupados por curso y en orden de fecha.

    Cada resultado trae la variación respecto al simulacro anterior del mismo curso
    (None en el primero), para mostrar el avance entre simulacros.
    """
    filas = (
        Resultado.objects.filter(matricula__alumno=hijo, simulacro__activa=True)
        .select_related('simulacro__curso')
        .order_by('simulacro__fecha', 'simulacro_id')
    )
    cursos = {}
    for fila in filas:
        simulacro = fila.simulacro
        grupo = cursos.setdefault(
            simulacro.curso_id,
            {'curso_id': simulacro.curso_id, 'curso': simulacro.curso.nombre, 'resultados': []},
        )
        puntaje = float(fila.puntaje)
        anterior = grupo['resultados'][-1]['puntaje'] if grupo['resultados'] else None
        grupo['resultados'].append({
            'simulacro_id': simulacro.id,
            'simulacro': simulacro.nombre,
            'fecha': simulacro.fecha,
            'puntaje': puntaje,
            'variacion': None if anterior is None else round(puntaje - anterior, 2),
        })

    resultado = []
    for grupo in cursos.values():
        puntajes = [r['puntaje'] for r in grupo['resultados']]
        grupo['promedio'] = round(sum(puntajes) / len(puntajes), 2)
        resultado.append(grupo)
    return sorted(resultado, key=lambda g: g['curso'])
