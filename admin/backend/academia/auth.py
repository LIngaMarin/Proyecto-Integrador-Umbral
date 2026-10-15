from rest_framework.permissions import BasePermission
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from rest_framework_simplejwt.views import TokenObtainPairView


class EsAdministrador(BasePermission):
    def has_permission(self, request, view):
        usuario = request.user
        if not usuario or not usuario.is_authenticated:
            return False
        perfil = getattr(usuario, 'perfil', None)
        return bool(perfil and perfil.rol == 'ADMIN')


class LoginSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        datos = super().validate(attrs)
        perfil = getattr(self.user, 'perfil', None)
        datos['rol'] = perfil.rol if perfil else None
        datos['nombre'] = self.user.get_full_name() or self.user.username
        return datos


class LoginView(TokenObtainPairView):
    serializer_class = LoginSerializer


class EsAdminOCoordinador(BasePermission):
    def has_permission(self, request, view):
        usuario = request.user
        if not usuario or not usuario.is_authenticated:
            return False
        perfil = getattr(usuario, 'perfil', None)
        return bool(perfil and perfil.rol in ('ADMIN', 'COORDINADOR'))
