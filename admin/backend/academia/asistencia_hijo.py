from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

from django.db.models import Count
from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from .apoderados import EsApoderado, _datos_hijo
from .models import Asistencia

ZONA = ZoneInfo('America/Lima')
DIAS_POR_DEFECTO = 30
MAXIMO_DIAS = 366


def hoy():
    return datetime.now(ZONA).date()


def _fecha(valor):
    try:
        return date.fromisoformat(valor)
    except ValueError:
        return None


def _rango(parametros, dia_actual):
    """Devuelve (desde, hasta, error). Sin fechas, los últimos 30 días hasta hoy."""
    desde, hasta = parametros.get('desde'), parametros.get('hasta')
    fin = inicio = None
    if hasta:
        fin = _fecha(hasta)
        if fin is None:
            return None, None, {'hasta': 'Usa el formato AAAA-MM-DD.'}
    if desde:
        inicio = _fecha(desde)
        if inicio is None:
            return None, None, {'desde': 'Usa el formato AAAA-MM-DD.'}
    fin = fin or dia_actual
    inicio = inicio or fin - timedelta(days=DIAS_POR_DEFECTO - 1)
    if inicio > fin:
        return None, None, {'desde': 'La fecha inicial no puede ser posterior a la final.'}
    if (fin - inicio).days >= MAXIMO_DIAS:
        return None, None, {'hasta': 'El rango máximo es de un año.'}
    return inicio, fin, None


class AsistenciaHijoView(APIView):
    """Asistencia de un hijo del apoderado que inició sesión.

    ?desde=AAAA-MM-DD & ?hasta=AAAA-MM-DD   ambos días incluidos. Sin fechas se
    devuelven los últimos 30 días; con una sola fecha, el otro extremo se completa.
    Un apoderado solo puede consultar a sus propios hijos (404 con los demás).
    """

    permission_classes = [EsApoderado]

    def get(self, request, pk):
        hijos = request.user.perfil.apoderado.hijos.select_related('user')
        hijo = get_object_or_404(hijos, pk=pk)

        desde, hasta, error = _rango(request.query_params, hoy())
        if error:
            return Response(error, status=400)

        registros = Asistencia.objects.filter(alumno=hijo, fecha__range=(desde, hasta))
        cuenta = {
            fila['estado']: fila['total']
            for fila in registros.values('estado').annotate(total=Count('id'))
        }
        presentes = cuenta.get(Asistencia.Estado.PRESENTE, 0)
        tardanzas = cuenta.get(Asistencia.Estado.TARDANZA, 0)
        faltas = cuenta.get(Asistencia.Estado.FALTA, 0)
        total = presentes + tardanzas + faltas

        return Response({
            'hijo': _datos_hijo(hijo),
            'desde': desde,
            'hasta': hasta,
            'resumen': {
                'presentes': presentes,
                'tardanzas': tardanzas,
                'faltas': faltas,
                'total': total,
                # Una tardanza cuenta como asistencia: el alumno sí estuvo.
                'porcentaje': round((presentes + tardanzas) * 100 / total) if total else None,
            },
            'registros': [
                {
                    'fecha': r.fecha,
                    'estado': r.estado,
                    'estado_texto': r.get_estado_display(),
                    'hora_registro': r.hora_registro.strftime('%H:%M') if r.hora_registro else None,
                }
                for r in registros.order_by('-fecha')
            ],
        })
