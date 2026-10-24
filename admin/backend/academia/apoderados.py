from django.contrib.auth.models import User
from django.db import transaction
from rest_framework import serializers, status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from .auth import EsAdministrador
from .models import Apoderado, Perfil


class ApoderadoSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(source='perfil.user.email')
    first_name = serializers.CharField(source='perfil.user.first_name')
    last_name = serializers.CharField(source='perfil.user.last_name')
    activo = serializers.BooleanField(source='perfil.user.is_active', required=False)
    password = serializers.CharField(write_only=True, required=False, min_length=8)
    hijos = serializers.PrimaryKeyRelatedField(
        many=True,
        queryset=Perfil.objects.filter(rol=Perfil.Rol.ALUMNO),
        required=False,
    )

    class Meta:
        model = Apoderado
        fields = ['id', 'email', 'first_name', 'last_name', 'password', 'telefono', 'hijos', 'activo']

    def validate(self, datos):
        email = datos.get('perfil', {}).get('user', {}).get('email')
        if email:
            existentes = User.objects.filter(username__iexact=email)
            if self.instance:
                existentes = existentes.exclude(pk=self.instance.perfil.user_id)
            if existentes.exists():
                raise serializers.ValidationError(
                    {'email': 'Ya existe un usuario con este correo.'}
                )
        return datos

    def create(self, validated_data):
        datos_user = validated_data.pop('perfil', {}).get('user', {})
        password = validated_data.pop('password', None)
        hijos = validated_data.pop('hijos', [])
        if not password:
            raise serializers.ValidationError({'password': 'La contraseña es obligatoria.'})
        with transaction.atomic():
            user = User.objects.create_user(
                username=datos_user['email'],
                email=datos_user['email'],
                password=password,
                first_name=datos_user.get('first_name', ''),
                last_name=datos_user.get('last_name', ''),
                is_active=datos_user.get('is_active', True),
            )
            perfil = Perfil.objects.create(user=user, rol=Perfil.Rol.APODERADO)
            apoderado = Apoderado.objects.create(perfil=perfil, **validated_data)
            apoderado.hijos.set(hijos)
        return apoderado

    def update(self, instance, validated_data):
        datos_user = validated_data.pop('perfil', {}).get('user', {})
        password = validated_data.pop('password', None)
        hijos = validated_data.pop('hijos', None)
        user = instance.perfil.user
        for campo, valor in datos_user.items():
            setattr(user, campo, valor)
        if 'email' in datos_user:
            user.username = datos_user['email']
        if password:
            user.set_password(password)
        user.save()
        for campo, valor in validated_data.items():
            setattr(instance, campo, valor)
        instance.save()
        if hijos is not None:
            instance.hijos.set(hijos)
        return instance


class ApoderadoViewSet(viewsets.ModelViewSet):
    queryset = (
        Apoderado.objects.select_related('perfil__user')
        .prefetch_related('hijos')
        .order_by('id')
    )
    serializer_class = ApoderadoSerializer
    permission_classes = [EsAdministrador]

    def destroy(self, request, *args, **kwargs):
        apoderado = self.get_object()
        apoderado.perfil.user.is_active = False
        apoderado.perfil.user.save()
        return Response(status=status.HTTP_204_NO_CONTENT)

    def _alumno(self, request):
        try:
            return Perfil.objects.get(pk=request.data.get('alumno'), rol=Perfil.Rol.ALUMNO)
        except (Perfil.DoesNotExist, ValueError, TypeError):
            return None

    @action(detail=True, methods=['post'])
    def vincular(self, request, pk=None):
        apoderado = self.get_object()
        alumno = self._alumno(request)
        if alumno is None:
            return Response(
                {'alumno': ['Indica un alumno válido.']},
                status=status.HTTP_400_BAD_REQUEST,
            )
        apoderado.hijos.add(alumno)
        return Response(self.get_serializer(apoderado).data)

    @action(detail=True, methods=['post'])
    def desvincular(self, request, pk=None):
        apoderado = self.get_object()
        alumno = self._alumno(request)
        if alumno is None:
            return Response(
                {'alumno': ['Indica un alumno válido.']},
                status=status.HTTP_400_BAD_REQUEST,
            )
        apoderado.hijos.remove(alumno)
        return Response(self.get_serializer(apoderado).data)
