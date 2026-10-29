from django.shortcuts import get_object_or_404
from rest_framework import serializers, status
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdministrador
from .models import Academia, ConfiguracionIA


class ConfiguracionIASerializer(serializers.ModelSerializer):
    class Meta:
        model = ConfiguracionIA
        fields = ['academia', 'activa', 'limite_por_docente', 'actualizado_en']
        read_only_fields = ['academia', 'actualizado_en']


class ConfiguracionIAView(APIView):
    permission_classes = [EsAdministrador]

    def _config(self, request):
        academia_id = request.query_params.get('academia')
        if not academia_id or not academia_id.isdigit():
            return None
        academia = get_object_or_404(Academia, pk=int(academia_id))
        config, _ = ConfiguracionIA.objects.get_or_create(academia=academia)
        return config

    def _sin_academia(self):
        return Response(
            {'academia': ['Indica la academia.']}, status=status.HTTP_400_BAD_REQUEST
        )

    def get(self, request):
        config = self._config(request)
        if config is None:
            return self._sin_academia()
        return Response(ConfiguracionIASerializer(config).data)

    def put(self, request):
        config = self._config(request)
        if config is None:
            return self._sin_academia()
        serializer = ConfiguracionIASerializer(config, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)
