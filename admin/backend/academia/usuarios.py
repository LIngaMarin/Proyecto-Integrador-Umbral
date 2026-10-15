from django.contrib.auth.models import User
from rest_framework import serializers, status, viewsets
from rest_framework.response import Response

from .auth import EsAdministrador
from .models import Perfil


class UsuarioSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(source='user.email')
    first_name = serializers.CharField(source='user.first_name')
    last_name = serializers.CharField(source='user.last_name')
    activo = serializers.BooleanField(source='user.is_active', required=False)
    password = serializers.CharField(write_only=True, required=False, min_length=8)

    class Meta:
        model = Perfil
        fields = ['id', 'email', 'first_name', 'last_name', 'password', 'rol', 'sede', 'activo']

    def validate(self, datos):
        email = datos.get('user', {}).get('email')
        if email:
            existentes = User.objects.filter(username__iexact=email)
            if self.instance:
                existentes = existentes.exclude(pk=self.instance.user_id)
            if existentes.exists():
                raise serializers.ValidationError(
                    {'email': 'Ya existe un usuario con este correo.'}
                )
        return datos

    def create(self, validated_data):
        datos_user = validated_data.pop('user', {})
        password = validated_data.pop('password', None)
        if not password:
            raise serializers.ValidationError({'password': 'La contraseña es obligatoria.'})
        user = User.objects.create_user(
            username=datos_user['email'],
            email=datos_user['email'],
            password=password,
            first_name=datos_user.get('first_name', ''),
            last_name=datos_user.get('last_name', ''),
            is_active=datos_user.get('is_active', True),
        )
        return Perfil.objects.create(user=user, **validated_data)

    def update(self, instance, validated_data):
        datos_user = validated_data.pop('user', {})
        password = validated_data.pop('password', None)
        user = instance.user
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
        return instance


class UsuarioViewSet(viewsets.ModelViewSet):
    queryset = Perfil.objects.select_related('user').order_by('id')
    serializer_class = UsuarioSerializer
    permission_classes = [EsAdministrador]

    def get_queryset(self):
        queryset = super().get_queryset()
        rol = self.request.query_params.get('rol')
        if rol:
            queryset = queryset.filter(rol=rol)
        return queryset

    def destroy(self, request, *args, **kwargs):
        perfil = self.get_object()
        perfil.user.is_active = False
        perfil.user.save()
        return Response(status=status.HTTP_204_NO_CONTENT)
