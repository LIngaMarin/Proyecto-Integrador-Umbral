from rest_framework import status, viewsets
from rest_framework.response import Response

from .models import Aula, Ciclo, Curso, Sede
from .serializers import (
    AulaSerializer,
    CicloSerializer,
    CursoSerializer,
    SedeSerializer,
)


class DesactivarMixin:
    campo_estado = 'activa'

    def destroy(self, request, *args, **kwargs):
        objeto = self.get_object()
        setattr(objeto, self.campo_estado, False)
        objeto.save()
        return Response(status=status.HTTP_204_NO_CONTENT)


class FiltroPorPadreMixin:
    parametro_filtro = None
    campo_filtro = None

    def get_queryset(self):
        queryset = super().get_queryset()
        valor = self.request.query_params.get(self.parametro_filtro)
        if valor:
            queryset = queryset.filter(**{self.campo_filtro: valor})
        return queryset


class SedeViewSet(DesactivarMixin, FiltroPorPadreMixin, viewsets.ModelViewSet):
    queryset = Sede.objects.all().order_by('id')
    serializer_class = SedeSerializer
    parametro_filtro = 'academia'
    campo_filtro = 'academia_id'


class AulaViewSet(DesactivarMixin, FiltroPorPadreMixin, viewsets.ModelViewSet):
    queryset = Aula.objects.all().order_by('id')
    serializer_class = AulaSerializer
    parametro_filtro = 'sede'
    campo_filtro = 'sede_id'


class CicloViewSet(DesactivarMixin, FiltroPorPadreMixin, viewsets.ModelViewSet):
    queryset = Ciclo.objects.all().order_by('id')
    serializer_class = CicloSerializer
    campo_estado = 'activo'
    parametro_filtro = 'academia'
    campo_filtro = 'academia_id'


class CursoViewSet(DesactivarMixin, FiltroPorPadreMixin, viewsets.ModelViewSet):
    queryset = Curso.objects.all().order_by('id')
    serializer_class = CursoSerializer
    campo_estado = 'activo'
    parametro_filtro = 'ciclo'
    campo_filtro = 'ciclo_id'
