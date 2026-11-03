"""HU 9: lectura pública del logo de la academia (la usan el login y la app móvil)."""
from rest_framework import permissions
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Academia


class LogoPublicoView(APIView):
    """GET /api/logo/ devuelve el logo de la primera academia activa.

    Con ?academia=<id> devuelve el de esa academia. No requiere sesión.
    Si la academia no subió logo, 'logo' llega como null.
    """

    permission_classes = [permissions.AllowAny]
    authentication_classes = []

    def get(self, request):
        pedida = request.query_params.get('academia')
        academias = Academia.objects.filter(activa=True)
        if pedida:
            academia = academias.filter(pk=pedida).first() if pedida.isdigit() else None
        else:
            academia = academias.order_by('id').first()
        if academia is None:
            return Response({'detail': 'No existe la academia.'}, status=404)

        logo = request.build_absolute_uri(academia.logo.url) if academia.logo else None
        return Response({'academia': academia.id, 'nombre': academia.nombre, 'logo': logo})
