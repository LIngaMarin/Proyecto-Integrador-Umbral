from django.core.validators import RegexValidator
from rest_framework import permissions, serializers, status
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador, EsAdministrador
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


class ColoresSerializer(serializers.Serializer):
    color_primario = serializers.CharField(max_length=7, validators=[COLOR])
    color_secundario = serializers.CharField(max_length=7, validators=[COLOR])


def datos_del_borrador(marca):
    hay_borrador = bool(marca.borrador_primario)
    return {
        'hay_borrador': hay_borrador,
        'color_primario': marca.borrador_primario if hay_borrador else marca.color_primario,
        'color_secundario': marca.borrador_secundario if hay_borrador else marca.color_secundario,
    }


class MarcaBorradorView(APIView):
    """Colores en borrador: se guardan y se previsualizan sin afectar lo publicado."""

    def get_permissions(self):
        if self.request.method in permissions.SAFE_METHODS:
            return [EsAdminOCoordinador()]
        return [EsAdministrador()]

    def get(self, request):
        marca = MarcaView().obtener(request)
        if marca is None:
            return Response({'academia': 'No existe la academia.'}, status=404)
        return Response(datos_del_borrador(marca))

    def put(self, request):
        marca = MarcaView().obtener(request)
        if marca is None:
            return Response({'academia': 'No existe la academia.'}, status=404)
        actuales = datos_del_borrador(marca)
        entrada = ColoresSerializer(
            data={
                'color_primario': request.data.get('color_primario', actuales['color_primario']),
                'color_secundario': request.data.get('color_secundario', actuales['color_secundario']),
            }
        )
        entrada.is_valid(raise_exception=True)
        marca.borrador_primario = entrada.validated_data['color_primario'].upper()
        marca.borrador_secundario = entrada.validated_data['color_secundario'].upper()
        marca.save()
        return Response(datos_del_borrador(marca))

    def delete(self, request):
        marca = MarcaView().obtener(request)
        if marca is None:
            return Response({'academia': 'No existe la academia.'}, status=404)
        marca.borrador_primario = ''
        marca.borrador_secundario = ''
        marca.save()
        return Response(status=status.HTTP_204_NO_CONTENT)


class MarcaPublicarView(APIView):
    """Aplica el borrador como colores publicados."""

    permission_classes = [EsAdministrador]

    def post(self, request):
        marca = MarcaView().obtener(request)
        if marca is None:
            return Response({'academia': 'No existe la academia.'}, status=404)
        if not marca.borrador_primario:
            return Response({'borrador': 'No hay cambios para publicar.'}, status=400)
        marca.color_primario = marca.borrador_primario
        marca.color_secundario = marca.borrador_secundario
        marca.borrador_primario = ''
        marca.borrador_secundario = ''
        marca.save()
        return Response(MarcaSerializer(marca).data)
