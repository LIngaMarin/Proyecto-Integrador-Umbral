from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from . import asistencia_hijo
from .apoderados import EsApoderado, _datos_hijo
from .asistencia_hijo import _rango, resumen_asistencia
from .resultados_hijo_calculo import resultados_por_curso


def _tendencia(puntajes):
    """Compara el último simulacro con el anterior (puntajes en orden de fecha)."""
    if len(puntajes) < 2:
        return 'sin_datos'
    anterior, ultimo = puntajes[-2], puntajes[-1]
    if ultimo > anterior:
        return 'sube'
    if ultimo < anterior:
        return 'baja'
    return 'estable'


class ReporteHijoView(APIView):
    """Reporte periódico de un hijo: asistencia y simulacros del período.

    ?desde=AAAA-MM-DD & ?hasta=AAAA-MM-DD, igual que la asistencia (por defecto, los
    últimos 30 días). Un apoderado solo puede consultar a sus propios hijos (404 con
    los demás).
    """

    permission_classes = [EsApoderado]

    def get(self, request, pk):
        hijos = request.user.perfil.apoderado.hijos.select_related('user')
        hijo = get_object_or_404(hijos, pk=pk)

        desde, hasta, error = _rango(request.query_params, asistencia_hijo.hoy())
        if error:
            return Response(error, status=400)

        resumen, _ = resumen_asistencia(hijo, desde, hasta)

        simulacros = sorted(
            (
                {'nombre': r['simulacro'], 'fecha': r['fecha'], 'puntaje': r['puntaje']}
                for curso in resultados_por_curso(hijo, desde, hasta)
                for r in curso['resultados']
            ),
            key=lambda s: s['fecha'],
        )
        puntajes = [s['puntaje'] for s in simulacros]

        return Response({
            'alumno': _datos_hijo(hijo),
            'periodo': {'desde': desde, 'hasta': hasta},
            'asistencia': {
                'presentes': resumen['presentes'],
                'tardanzas': resumen['tardanzas'],
                'faltas': resumen['faltas'],
                'porcentaje': resumen['porcentaje'],
            },
            'simulacros': simulacros,
            'promedio': round(sum(puntajes) / len(puntajes), 2) if puntajes else None,
            'tendencia': _tendencia(puntajes),
        })
