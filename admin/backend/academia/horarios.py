from datetime import date

from rest_framework import serializers, viewsets
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador
from .matriculas import DesactivarViewSet
from .models import Festivo, Horario, Perfil
from .usuarios import UsuarioSerializer


def buscar_conflictos(dia, inicio, fin, seccion, docente, aula, excluir_id=None):
    """Devuelve un dict {campo: mensaje} con los choques de aula, docente y sección."""
    solapados = Horario.objects.filter(
        activa=True, dia_semana=dia, hora_inicio__lt=fin, hora_fin__gt=inicio
    ).select_related('seccion__curso')
    if excluir_id:
        solapados = solapados.exclude(pk=excluir_id)

    errores = {}
    for h in solapados:
        rango = f'{h.hora_inicio:%H:%M} a {h.hora_fin:%H:%M}'
        if h.aula_id == aula.id:
            errores.setdefault('aula', f'El aula ya está ocupada de {rango} por {h.seccion}.')
        if h.docente_id == docente.id:
            errores.setdefault('docente', f'El docente ya tiene clase de {rango} en {h.seccion}.')
        if h.seccion_id == seccion.id:
            errores.setdefault('seccion', f'La sección ya tiene clase de {rango}.')
    return errores


class HorarioSerializer(serializers.ModelSerializer):
    class Meta:
        model = Horario
        fields = ['id', 'seccion', 'docente', 'aula', 'dia_semana', 'hora_inicio', 'hora_fin', 'activa']

    def validate(self, datos):
        def valor(campo):
            return datos.get(campo, getattr(self.instance, campo, None))

        seccion, docente, aula = valor('seccion'), valor('docente'), valor('aula')
        dia, inicio, fin = valor('dia_semana'), valor('hora_inicio'), valor('hora_fin')
        activa = datos.get('activa', self.instance.activa if self.instance else True)

        if fin <= inicio:
            raise serializers.ValidationError(
                {'hora_fin': 'La hora de fin debe ser posterior a la de inicio.'}
            )
        if docente.rol != Perfil.Rol.DOCENTE or not docente.user.is_active:
            raise serializers.ValidationError({'docente': 'Selecciona un docente activo.'})
        if not aula.activa:
            raise serializers.ValidationError({'aula': 'El aula está inactiva.'})
        if not seccion.activa:
            raise serializers.ValidationError({'seccion': 'La sección está inactiva.'})
        if aula.capacidad < seccion.cupo:
            raise serializers.ValidationError(
                {'aula': f'El aula (capacidad {aula.capacidad}) no alcanza para el cupo de la sección ({seccion.cupo}).'}
            )

        if activa:
            excluir = self.instance.pk if self.instance else None
            errores = buscar_conflictos(dia, inicio, fin, seccion, docente, aula, excluir)
            if errores:
                raise serializers.ValidationError(errores)
        return datos


class HorarioViewSet(DesactivarViewSet):
    queryset = Horario.objects.all().order_by('dia_semana', 'hora_inicio')
    serializer_class = HorarioSerializer

    def get_queryset(self):
        consulta = super().get_queryset()
        seccion = self.request.query_params.get('seccion')
        return consulta.filter(seccion_id=seccion) if seccion else consulta


class DocenteViewSet(viewsets.ReadOnlyModelViewSet):
    permission_classes = [EsAdminOCoordinador]
    serializer_class = UsuarioSerializer
    queryset = Perfil.objects.filter(
        rol=Perfil.Rol.DOCENTE, user__is_active=True
    ).order_by('id')


class SesionesView(APIView):
    """Clases que se dictan en una fecha, sin contar los días festivos."""

    permission_classes = [EsAdminOCoordinador]

    def get(self, request):
        try:
            fecha = date.fromisoformat(request.query_params.get('fecha', ''))
        except ValueError:
            return Response({'fecha': 'Usa el formato AAAA-MM-DD.'}, status=400)

        festivos = Festivo.objects.filter(fecha=fecha, activo=True)
        academias_festivas = [f.academia_id for f in festivos]
        horarios = (
            Horario.objects.filter(
                activa=True,
                dia_semana=fecha.weekday(),
                seccion__curso__ciclo__fecha_inicio__lte=fecha,
                seccion__curso__ciclo__fecha_fin__gte=fecha,
            )
            .exclude(seccion__curso__ciclo__academia_id__in=academias_festivas)
            .order_by('hora_inicio')
        )
        return Response(
            {
                'fecha': fecha.isoformat(),
                'festivos': [f.nombre for f in festivos],
                'sesiones': HorarioSerializer(horarios, many=True).data,
            }
        )
