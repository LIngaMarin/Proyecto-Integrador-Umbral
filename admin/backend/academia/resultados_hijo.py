from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from .apoderados import EsApoderado, _datos_hijo
from .resultados_hijo_calculo import resultados_por_curso


class ResultadosHijoView(APIView):
    """Resultados de simulacros de un hijo del apoderado que inició sesión.

    Un apoderado solo puede consultar a sus propios hijos (404 con los demás).
    """

    permission_classes = [EsApoderado]

    def get(self, request, pk):
        hijos = request.user.perfil.apoderado.hijos.select_related('user')
        hijo = get_object_or_404(hijos, pk=pk)
        return Response({'hijo': _datos_hijo(hijo), 'cursos': resultados_por_curso(hijo)})
