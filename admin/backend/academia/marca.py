from django.core.validators import RegexValidator
from rest_framework import permissions, serializers
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdministrador
from .models import Academia, Marca

COLOR = RegexValidator(r'^#[0-9A-Fa-f]{6}$', 'Usa un color hexadecimal, por ejemplo #2563EB.')


class MarcaSerializer(serializers.ModelSerializer):
    color_primario = serializers.CharField(max_length=7, validators=[COLOR])
    color_secundario = serializers.CharField(max_length=7, validators=[COLOR])

    class Meta:
        model = Marca
        fields = ['academia', 'color_primario', 'color_secundario']
        read_only_fields = ['academia']


class MarcaView(APIView):
    """Colores de la marca: lectura pública (la usa también la app móvil), escritura solo del administrador."""

    def get_permissions(self):
        if self.request.method in permissions.SAFE_METHODS:
            return [permissions.AllowAny()]
        return [EsAdministrador()]

    def obtener(self, request):
        pedida = request.query_params.get('academia')
        academias = Academia.objects.filter(activa=True)
        if pedida:
            academia = academias.filter(pk=pedida).first() if pedida.isdigit() else None
        else:
            academia = academias.order_by('id').first()
        if academia is None:
            return None
        marca, _ = Marca.objects.get_or_create(academia=academia)
        return marca

    def get(self, request):
        marca = self.obtener(request)
        if marca is None:
            return Response({'academia': 'No existe la academia.'}, status=404)
        return Response(MarcaSerializer(marca).data)

    def put(self, request):
        marca = self.obtener(request)
        if marca is None:
            return Response({'academia': 'No existe la academia.'}, status=404)
        serializer = MarcaSerializer(marca, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)
