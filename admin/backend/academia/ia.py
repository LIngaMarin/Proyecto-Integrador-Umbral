from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import BasePermission
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdministrador
from .models import Academia, ConfiguracionIA, Perfil, UsoIA


class UsoIABloqueado(Exception):
    pass


class EsDocente(BasePermission):
    def has_permission(self, request, view):
        usuario = request.user
        if not usuario or not usuario.is_authenticated:
            return False
        perfil = getattr(usuario, 'perfil', None)
        return bool(perfil and perfil.rol == Perfil.Rol.DOCENTE)


def inicio_de_mes():
    ahora = timezone.now()
    if timezone.is_aware(ahora):
        ahora = timezone.localtime(ahora)
    return ahora.replace(day=1, hour=0, minute=0, second=0, microsecond=0)


def usos_del_mes(docente):
    return UsoIA.objects.filter(docente=docente, creado_en__gte=inicio_de_mes()).count()


def configuracion_del_docente(docente):
    if not docente.sede_id:
        raise UsoIABloqueado(
            'Tu usuario no tiene una sede asignada, por lo que no se puede usar la IA.'
        )
    config, _ = ConfiguracionIA.objects.get_or_create(academia=docente.sede.academia)
    return config


def registrar_uso_ia(docente):
    with transaction.atomic():
        config = configuracion_del_docente(docente)
        ConfiguracionIA.objects.select_for_update().get(pk=config.pk)
        if not config.activa:
            raise UsoIABloqueado('El uso de IA está desactivado en tu academia.')
        usados = usos_del_mes(docente)
        if usados >= config.limite_por_docente:
            raise UsoIABloqueado(
                f'Alcanzaste el límite de {config.limite_por_docente} usos de IA este mes.'
            )
        UsoIA.objects.create(docente=docente)
        return {
            'usados': usados + 1,
            'limite': config.limite_por_docente,
            'restantes': config.limite_por_docente - usados - 1,
        }


class UsarIAView(APIView):
    permission_classes = [EsDocente]

    def post(self, request):
        try:
            return Response(registrar_uso_ia(request.user.perfil))
        except UsoIABloqueado as error:
            return Response({'error': str(error)}, status=status.HTTP_403_FORBIDDEN)


class MiUsoIAView(APIView):
    permission_classes = [EsDocente]

    def get(self, request):
        docente = request.user.perfil
        try:
            config = configuracion_del_docente(docente)
        except UsoIABloqueado as error:
            return Response({'error': str(error)}, status=status.HTTP_403_FORBIDDEN)
        usados = usos_del_mes(docente)
        return Response(
            {
                'activa': config.activa,
                'usados': usados,
                'limite': config.limite_por_docente,
                'restantes': max(config.limite_por_docente - usados, 0),
            }
        )


class UsoDocentesView(APIView):
    permission_classes = [EsAdministrador]

    def get(self, request):
        academia_id = request.query_params.get('academia')
        if not academia_id or not academia_id.isdigit():
            return Response(
                {'academia': ['Indica la academia.']}, status=status.HTTP_400_BAD_REQUEST
            )
        academia = get_object_or_404(Academia, pk=int(academia_id))
        config, _ = ConfiguracionIA.objects.get_or_create(academia=academia)
        docentes = (
            Perfil.objects.filter(rol=Perfil.Rol.DOCENTE, sede__academia=academia)
            .select_related('user')
            .order_by('id')
        )
        return Response(
            [
                {
                    'id': d.id,
                    'nombre': f'{d.user.first_name} {d.user.last_name}'.strip() or d.user.username,
                    'email': d.user.email,
                    'usados': usos_del_mes(d),
                    'limite': config.limite_por_docente,
                }
                for d in docentes
            ]
        )
