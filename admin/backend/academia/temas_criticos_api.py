from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador
from .models import Simulacro
from .temas_criticos import analizar_con_ia, resumen_basico, temas_por_seccion


class TemasCriticosView(APIView):
    """Temas con más errores por salón en un simulacro.

    ?simulacro=<id>   simulacro a revisar (el más reciente por defecto).
    ?seccion=<id>     limita el resultado a un salón.
    ?ia=1             pide además el análisis de la IA (Gemini); sin esto se
                      devuelve un resumen básico y no se consulta a la IA.
    """

    permission_classes = [EsAdminOCoordinador]

    def get(self, request):
        parametros = request.query_params
        simulacros = Simulacro.objects.filter(activa=True)

        pedido = parametros.get('simulacro')
        if pedido:
            if not pedido.isdigit():
                return Response({'simulacro': 'Indica un identificador válido.'}, status=400)
            simulacro = simulacros.filter(pk=int(pedido)).first()
            if simulacro is None:
                return Response({'simulacro': 'No existe ese simulacro.'}, status=404)
        else:
            simulacro = simulacros.order_by('-fecha', '-id').first()

        seccion = parametros.get('seccion')
        if seccion and not seccion.isdigit():
            return Response({'seccion': 'Indica un identificador válido.'}, status=400)

        if simulacro is None:
            return Response({'simulacro': None, 'ia': {'disponible': False, 'motivo': None}, 'secciones': []})

        secciones = temas_por_seccion(simulacro, int(seccion) if seccion else None)
        if parametros.get('ia') == '1':
            secciones, ia = analizar_con_ia(secciones)
        else:
            for s in secciones:
                s['resumen'] = resumen_basico(s)
            ia = {'disponible': False, 'motivo': None}

        return Response(
            {
                'simulacro': {
                    'id': simulacro.id,
                    'nombre': simulacro.nombre,
                    'fecha': simulacro.fecha.isoformat(),
                    'curso': simulacro.curso.nombre,
                },
                'ia': ia,
                'secciones': secciones,
            }
        )
