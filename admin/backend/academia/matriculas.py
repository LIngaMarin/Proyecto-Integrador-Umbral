from rest_framework import serializers, status, viewsets
from rest_framework.response import Response

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
