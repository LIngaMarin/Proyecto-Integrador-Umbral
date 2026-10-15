from rest_framework import serializers, viewsets

from .models import Academia


class AcademiaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Academia
        fields = ['id', 'nombre', 'activa']


class AcademiaViewSet(viewsets.ModelViewSet):
    queryset = Academia.objects.all().order_by('id')
    serializer_class = AcademiaSerializer
