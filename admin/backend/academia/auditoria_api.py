from datetime import date, datetime, time, timedelta
from zoneinfo import ZoneInfo

from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdministrador
from .models import RegistroAuditoria

ZONA = ZoneInfo('America/Lima')
POR_PAGINA = 20
MAXIMO_POR_PAGINA = 100


def _entero(valor):
    return int(valor) if valor and valor.isdigit() else None


def _fecha(valor):
    try:
        return date.fromisoformat(valor)
    except ValueError:
        return None


def _serializar(registro):
    perfil = getattr(registro.usuario, 'perfil', None)
    return {
        'id': registro.id,
        'fecha': registro.fecha,
        'usuario': perfil.id if perfil else None,
        'usuario_nombre': registro.usuario_nombre,
        'rol': registro.rol,
        'accion': registro.accion,
        'accion_texto': registro.get_accion_display(),
        'recurso': registro.recurso,
        'objeto_id': registro.objeto_id,
        'metodo': registro.metodo,
        'ruta': registro.ruta,
        'estado': registro.estado,
        'ip': registro.ip,
    }


class AuditoriaView(APIView):
    """Historial de acciones hechas en el panel, de la más reciente a la más antigua.

    Filtros (todos opcionales):
      ?usuario=<id>   acciones de ese usuario (el id que usa /api/usuarios/).
      ?accion=CREAR|EDITAR|ELIMINAR
      ?recurso=<nombre>   por ejemplo alumnos, matriculas, simulacros.
      ?desde=AAAA-MM-DD & ?hasta=AAAA-MM-DD   ambos días incluidos, hora de Lima.
      ?pagina=<n> & ?por_pagina=<n>   20 por página, máximo 100.
    """

    permission_classes = [EsAdministrador]

    def get(self, request):
        parametros = request.query_params
        registros = RegistroAuditoria.objects.select_related('usuario__perfil')

        usuario = parametros.get('usuario')
        if usuario:
            if _entero(usuario) is None:
                return Response({'usuario': 'Indica un identificador válido.'}, status=400)
            registros = registros.filter(usuario__perfil__id=int(usuario))

        accion = parametros.get('accion')
        if accion:
            if accion not in RegistroAuditoria.Accion.values:
                return Response(
                    {'accion': 'Usa CREAR, EDITAR o ELIMINAR.'}, status=400
                )
            registros = registros.filter(accion=accion)

        recurso = parametros.get('recurso')
        if recurso:
            registros = registros.filter(recurso=recurso)

        desde = parametros.get('desde')
        if desde:
            dia = _fecha(desde)
            if dia is None:
                return Response({'desde': 'Usa el formato AAAA-MM-DD.'}, status=400)
            registros = registros.filter(fecha__gte=datetime.combine(dia, time.min, ZONA))

        hasta = parametros.get('hasta')
        if hasta:
            dia = _fecha(hasta)
            if dia is None:
                return Response({'hasta': 'Usa el formato AAAA-MM-DD.'}, status=400)
            registros = registros.filter(
                fecha__lt=datetime.combine(dia + timedelta(days=1), time.min, ZONA)
            )

        por_pagina = _entero(parametros.get('por_pagina')) or POR_PAGINA
        por_pagina = min(por_pagina, MAXIMO_POR_PAGINA)
        pagina = max(_entero(parametros.get('pagina')) or 1, 1)

        total = registros.count()
        paginas = max(-(-total // por_pagina), 1)
        inicio = (pagina - 1) * por_pagina
        return Response({
            'total': total,
            'pagina': pagina,
            'paginas': paginas,
            'resultados': [_serializar(r) for r in registros[inicio:inicio + por_pagina]],
        })
