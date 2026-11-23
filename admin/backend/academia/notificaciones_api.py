from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from .apoderados import EsApoderado, _datos_hijo
from .models import Notificacion


def _mis_notificaciones(apoderado):
    """Notificaciones del apoderado sobre sus hijos actuales."""
    return Notificacion.objects.filter(
        apoderado=apoderado, alumno__in=apoderado.hijos.all()
    ).select_related('alumno__user')


def _datos_notificacion(notificacion):
    return {
        'id': notificacion.id,
        'tipo': notificacion.tipo,
        'tipo_texto': notificacion.get_tipo_display(),
        'mensaje': notificacion.mensaje,
        'alumno': _datos_hijo(notificacion.alumno),
        'fecha_creacion': notificacion.fecha_creacion,
        'leida': notificacion.leida,
    }


class MisNotificacionesView(APIView):
    """Notificaciones de los hijos del apoderado que inició sesión, más reciente primero.

    `sin_leer` cuenta las que aún no se han abierto.
    """

    permission_classes = [EsApoderado]

    def get(self, request):
        notificaciones = _mis_notificaciones(request.user.perfil.apoderado).order_by(
            '-fecha_creacion', '-id'
        )
        return Response({
            'sin_leer': notificaciones.filter(leida=False).count(),
            'notificaciones': [_datos_notificacion(n) for n in notificaciones],
        })


class LeerNotificacionView(APIView):
    """Marca como leída una notificación propia (404 con las ajenas)."""

    permission_classes = [EsApoderado]

    def post(self, request, pk):
        notificacion = get_object_or_404(
            _mis_notificaciones(request.user.perfil.apoderado), pk=pk
        )
        if not notificacion.leida:
            notificacion.leida = True
            notificacion.save(update_fields=['leida'])
        return Response(_datos_notificacion(notificacion))
