from .models import Matricula, Simulacro


def simulacros_programados(hijo, desde, hasta):
    """Simulacros activos de los cursos en que el alumno está matriculado.

    Incluye ambos extremos del rango, en orden de fecha. Cada simulacro trae la
    sección del alumno en ese curso para mostrarla en el detalle del calendario.
    """
    matriculas = (
        Matricula.objects.filter(alumno=hijo, activa=True)
        .select_related('seccion')
        .order_by('id')
    )
    seccion_por_curso = {}
    for matricula in matriculas:
        seccion_por_curso.setdefault(matricula.seccion.curso_id, matricula.seccion.nombre)

    simulacros = (
        Simulacro.objects.filter(
            activa=True,
            curso_id__in=seccion_por_curso.keys(),
            fecha__range=(desde, hasta),
        )
        .select_related('curso')
        .order_by('fecha', 'id')
    )
    return [
        {
            'id': s.id,
            'nombre': s.nombre,
            'fecha': s.fecha,
            'curso_id': s.curso_id,
            'curso': s.curso.nombre,
            'seccion': seccion_por_curso[s.curso_id],
        }
        for s in simulacros
    ]
