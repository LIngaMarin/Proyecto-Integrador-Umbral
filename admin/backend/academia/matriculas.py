from django.contrib.auth.models import User
from django.core.mail import send_mail
from django.db import transaction
from django.utils.crypto import get_random_string
from rest_framework import serializers, status, viewsets
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador
from .models import Matricula, Perfil, Seccion
from .usuarios import UsuarioSerializer


class SeccionSerializer(serializers.ModelSerializer):
    matriculados = serializers.SerializerMethodField()

    class Meta:
        model = Seccion
        fields = ['id', 'curso', 'nombre', 'aula', 'cupo', 'activa', 'matriculados']

    def get_matriculados(self, seccion):
        return seccion.matriculas.filter(activa=True).count()

    def validate(self, datos):
        aula = datos.get('aula', getattr(self.instance, 'aula', None))
        cupo = datos.get('cupo', getattr(self.instance, 'cupo', None))
        if aula and cupo and cupo > aula.capacidad:
            raise serializers.ValidationError(
                {'cupo': f'El cupo supera la capacidad del aula ({aula.capacidad}).'}
            )
        return datos


class MatriculaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Matricula
        fields = ['id', 'alumno', 'seccion', 'activa']

    def validate(self, datos):
        alumno = datos.get('alumno', getattr(self.instance, 'alumno', None))
        seccion = datos.get('seccion', getattr(self.instance, 'seccion', None))
        if alumno.rol != Perfil.Rol.ALUMNO or not alumno.user.is_active:
            raise serializers.ValidationError(
                {'alumno': 'Selecciona un alumno activo.'}
            )
        if not seccion.activa:
            raise serializers.ValidationError({'seccion': 'La sección está inactiva.'})

        activa = datos.get('activa', self.instance.activa if self.instance else True)
        if activa:
            otras = Matricula.objects.filter(activa=True)
            if self.instance:
                otras = otras.exclude(pk=self.instance.pk)
            if otras.filter(seccion=seccion).count() >= seccion.cupo:
                raise serializers.ValidationError(
                    {'seccion': 'La sección no tiene cupos disponibles.'}
                )
            if otras.filter(alumno=alumno, seccion__curso=seccion.curso).exists():
                raise serializers.ValidationError(
                    {'alumno': 'El alumno ya está matriculado en este curso.'}
                )
        return datos


class DesactivarViewSet(viewsets.ModelViewSet):
    permission_classes = [EsAdminOCoordinador]

    def destroy(self, request, *args, **kwargs):
        objeto = self.get_object()
        objeto.activa = False
        objeto.save(update_fields=['activa'])
        return Response(status=status.HTTP_204_NO_CONTENT)


class SeccionViewSet(DesactivarViewSet):
    queryset = Seccion.objects.all().order_by('id')
    serializer_class = SeccionSerializer

    def get_queryset(self):
        consulta = super().get_queryset()
        curso = self.request.query_params.get('curso')
        return consulta.filter(curso_id=curso) if curso else consulta


class MatriculaViewSet(DesactivarViewSet):
    queryset = Matricula.objects.all().order_by('id')
    serializer_class = MatriculaSerializer

    def get_queryset(self):
        consulta = super().get_queryset()
        seccion = self.request.query_params.get('seccion')
        return consulta.filter(seccion_id=seccion) if seccion else consulta


class AlumnoViewSet(viewsets.ReadOnlyModelViewSet):
    permission_classes = [EsAdminOCoordinador]
    serializer_class = UsuarioSerializer
    queryset = Perfil.objects.filter(
        rol=Perfil.Rol.ALUMNO, user__is_active=True
    ).order_by('id')


class NuevoAlumnoSerializer(serializers.Serializer):
    first_name = serializers.CharField(max_length=150)
    last_name = serializers.CharField(max_length=150)
    email = serializers.EmailField()
    seccion = serializers.PrimaryKeyRelatedField(queryset=Seccion.objects.all())

    def validate_email(self, email):
        existe = User.objects.filter(email__iexact=email) | User.objects.filter(
            username__iexact=email
        )
        if existe.exists():
            raise serializers.ValidationError('Ya existe un usuario con ese correo.')
        return email


class MatricularNuevoAlumnoView(APIView):
    permission_classes = [EsAdminOCoordinador]

    def post(self, request):
        entrada = NuevoAlumnoSerializer(data=request.data)
        entrada.is_valid(raise_exception=True)
        datos = entrada.validated_data
        clave = get_random_string(10)

        with transaction.atomic():
            user = User.objects.create_user(
                username=datos['email'],
                email=datos['email'],
                password=clave,
                first_name=datos['first_name'],
                last_name=datos['last_name'],
            )
            perfil = Perfil.objects.create(user=user, rol=Perfil.Rol.ALUMNO)
            matricula = MatriculaSerializer(
                data={'alumno': perfil.id, 'seccion': datos['seccion'].id}
            )
            matricula.is_valid(raise_exception=True)
            matricula.save()

        try:
            send_mail(
                'Tu matrícula en Umbral',
                f"Hola {user.first_name}, ya estás matriculado en {datos['seccion']}.\n\n"
                f'Usuario: {user.email}\n'
                f'Contraseña temporal: {clave}\n',
                None,
                [user.email],
            )
            correo_enviado = True
        except Exception:
            correo_enviado = False

        return Response(
            {
                'alumno': perfil.id,
                'matricula': matricula.data['id'],
                'correo_enviado': correo_enviado,
            },
            status=status.HTTP_201_CREATED,
        )
