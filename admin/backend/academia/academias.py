from rest_framework import permissions, serializers, status, viewsets
from rest_framework.response import Response

from .auth import EsAdminOCoordinador, EsAdministrador
from .models import Academia

LOGO_MAX_BYTES = 2 * 1024 * 1024
LOGO_FORMATOS = ('PNG', 'JPEG', 'WEBP')


class AcademiaSerializer(serializers.ModelSerializer):
    logo = serializers.ImageField(required=False, allow_null=True)

    class Meta:
        model = Academia
        fields = ['id', 'nombre', 'activa', 'logo']

    def validate_logo(self, logo):
        if logo is None:
            return logo
        if logo.size > LOGO_MAX_BYTES:
            raise serializers.ValidationError('El logo no debe superar los 2 MB.')
        if logo.image.format not in LOGO_FORMATOS:
            raise serializers.ValidationError('El logo debe ser PNG, JPG o WEBP.')
        return logo


class AcademiaViewSet(viewsets.ModelViewSet):
    queryset = Academia.objects.all().order_by('id')
    serializer_class = AcademiaSerializer

    def get_permissions(self):
        if self.request.method in permissions.SAFE_METHODS:
            return [EsAdminOCoordinador()]
        return [EsAdministrador()]

    def destroy(self, request, *args, **kwargs):
        academia = self.get_object()
        academia.activa = False
        academia.save(update_fields=['activa'])
        return Response(status=status.HTTP_204_NO_CONTENT)
