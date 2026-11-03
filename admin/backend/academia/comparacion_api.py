from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador
from .comparacion import comparar_simulacros
from .models import Simulacro


def _simulacro_json(simulacro):
    return {
        'id': simulacro.id,
        'nombre': simulacro.nombre,
        'fecha': simulacro.fecha.isoformat(),
    }


def _error(campo, mensaje, estado=400):
    return Response({campo: mensaje}, status=estado)


class ComparacionView(APIView):
    """Compara dos simulacros de un mismo curso.

    Dos formas de pedirlo:
      ?curso=<id>                      compara los dos simulacros más recientes del curso.
      ?anterior=<id>&actual=<id>       compara dos simulacros concretos.
    """

    permission_classes = [EsAdminOCoordinador]

    def get(self, request):
        parametros = request.query_params
        simulacros = Simulacro.objects.filter(activa=True).select_related('curso')
        anterior_id = parametros.get('anterior')
        actual_id = parametros.get('actual')
        curso_id = parametros.get('curso')

        if anterior_id or actual_id:
            if not (anterior_id and actual_id):
                return _error('simulacro', 'Indica el simulacro anterior y el actual.')
            if not (anterior_id.isdigit() and actual_id.isdigit()):
                return _error('simulacro', 'Indica identificadores válidos.')
            if anterior_id == actual_id:
                return _error('simulacro', 'Elige dos simulacros distintos.')
            anterior = simulacros.filter(pk=int(anterior_id)).first()
            actual = simulacros.filter(pk=int(actual_id)).first()
            if anterior is None or actual is None:
                return _error('simulacro', 'No existe alguno de los simulacros.', 404)
            if anterior.curso_id != actual.curso_id:
                return _error('simulacro', 'Los simulacros deben ser del mismo curso.')
            if anterior.fecha > actual.fecha:
                return _error('simulacro', 'El simulacro anterior debe ser previo al actual.')
        elif curso_id:
            if not curso_id.isdigit():
                return _error('curso', 'Indica un identificador válido.')
            recientes = list(
                simulacros.filter(curso_id=int(curso_id)).order_by('-fecha', '-id')[:2]
            )
            if len(recientes) < 2:
                return _error(
                    'curso', 'El curso necesita al menos dos simulacros para compararlos.'
                )
            actual, anterior = recientes
        else:
            return _error('curso', 'Indica un curso o los dos simulacros a comparar.')

        resultado = comparar_simulacros(anterior, actual)
        return Response(
            {
                'curso': {'id': actual.curso_id, 'nombre': actual.curso.nombre},
                'anterior': _simulacro_json(anterior),
                'actual': _simulacro_json(actual),
                **resultado,
            }
        )
