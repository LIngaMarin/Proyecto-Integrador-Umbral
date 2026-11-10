from django.core.exceptions import ValidationError
from django.db import transaction
from rest_framework.response import Response
from rest_framework.views import APIView

from .apoderados import EsApoderado
from .contacto_validacion import validar_correo, validar_telefono


def _datos(apoderado):
    return {'email': apoderado.perfil.user.email, 'telefono': apoderado.telefono}


class MisDatosContactoView(APIView):
    """Datos de contacto del apoderado que inició sesión (correo y teléfono).

    El correo también es su usuario de ingreso, así que al cambiarlo se actualiza
    el usuario para que pueda entrar con el correo nuevo.
    """

    permission_classes = [EsApoderado]

    def get(self, request):
        return Response(_datos(request.user.perfil.apoderado))

    def put(self, request):
        apoderado = request.user.perfil.apoderado
        errores = {}
        correo = telefono = None
        try:
            correo = validar_correo(request.data.get('email'), request.user)
        except ValidationError as e:
            errores['email'] = e.messages
        try:
            telefono = validar_telefono(request.data.get('telefono'))
        except ValidationError as e:
            errores['telefono'] = e.messages
        if errores:
            return Response(errores, status=400)

        with transaction.atomic():
            usuario = request.user
            usuario.email = correo
            usuario.username = correo
            usuario.save(update_fields=['email', 'username'])
            apoderado.telefono = telefono
            apoderado.save(update_fields=['telefono'])
        return Response(_datos(apoderado))
