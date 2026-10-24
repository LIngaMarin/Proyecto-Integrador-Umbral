from rest_framework import permissions, serializers, status, viewsets
from rest_framework.response import Response

from .auth import EsAdminOCoordinador, EsAdministrador
from .models import Festivo


class FestivoSerializer(serializers.ModelSerializer):
    class Meta:
        model = Festivo
        fields = ['id', 'academia', 'fecha', 'nombre', 'activo']


class FestivoViewSet(viewsets.ModelViewSet):
    queryset = Festivo.objects.all().order_by('fecha')
    serializer_class = FestivoSerializer

    def get_permissions(self):
        if self.request.method in permissions.SAFE_METHODS:
            return [EsAdminOCoordinador()]
        return [EsAdministrador()]

    def get_queryset(self):
        consulta = super().get_queryset()
        academia = self.request.query_params.get('academia')
        anio = self.request.query_params.get('anio')
        if academia:
            consulta = consulta.filter(academia_id=academia)
        if anio:
            consulta = consulta.filter(fecha__year=anio)
        return consulta

    def destroy(self, request, *args, **kwargs):
        festivo = self.get_object()
        festivo.activo = False
        festivo.save(update_fields=['activo'])
        return Response(status=status.HTTP_204_NO_CONTENT)
