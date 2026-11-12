from datetime import timedelta

from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from .apoderados import EsApoderado, _datos_hijo
from .asistencia_hijo import _fecha, hoy
from .programados_hijo_calculo import simulacros_programados

DIAS_POR_DEFECTO = 120
MAXIMO_DIAS = 366


def _rango(parametros, dia_actual):
    """Devuelve (desde, hasta, error). Sin fechas, desde hoy y los próximos 120 días."""
    desde = hasta = None
    if parametros.get('desde'):
        desde = _fecha(parametros['desde'])
        if desde is None:
            return None, None, {'desde': 'Usa el formato AAAA-MM-DD.'}
    if parametros.get('hasta'):
        hasta = _fecha(parametros['hasta'])
        if hasta is None:
            return None, None, {'hasta': 'Usa el formato AAAA-MM-DD.'}
    desde = desde or dia_actual
    hasta = hasta or desde + timedelta(days=DIAS_POR_DEFECTO)
    if desde > hasta:
        return None, None, {'desde': 'La fecha inicial no puede ser posterior a la final.'}
    if (hasta - desde).days >= MAXIMO_DIAS:
        return None, None, {'hasta': 'El rango máximo es de un año.'}
    return desde, hasta, None


class SimulacrosProgramadosHijoView(APIView):
    """Simulacros programados para un hijo del apoderado que inició sesión.

    ?desde=AAAA-MM-DD & ?hasta=AAAA-MM-DD   ambos días incluidos. Sin fechas se
    devuelven los de hoy en adelante (120 días). Un apoderado solo puede consultar
    a sus propios hijos (404 con los demás).
    """

    permission_classes = [EsApoderado]

    def get(self, request, pk):
        hijos = request.user.perfil.apoderado.hijos.select_related('user')
        hijo = get_object_or_404(hijos, pk=pk)

        desde, hasta, error = _rango(request.query_params, hoy())
        if error:
            return Response(error, status=400)

        return Response({
            'hijo': _datos_hijo(hijo),
            'desde': desde,
            'hasta': hasta,
            'simulacros': simulacros_programados(hijo, desde, hasta),
        })
