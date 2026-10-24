from django.db.models import Avg, Count, Max, Min
from rest_framework import serializers, viewsets
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador
from .matriculas import DesactivarViewSet
from .models import Matricula, Perfil, Resultado, Simulacro

UMBRAL_REFUERZO = 11


class SimulacroSerializer(serializers.ModelSerializer):
    class Meta:
        model = Simulacro
        fields = ['id', 'curso', 'nombre', 'fecha', 'activa']


class SimulacroViewSet(DesactivarViewSet):
    queryset = Simulacro.objects.all().order_by('-fecha', '-id')
    serializer_class = SimulacroSerializer


class ResultadoSerializer(serializers.ModelSerializer):
    alumno = serializers.PrimaryKeyRelatedField(
        queryset=Perfil.objects.filter(rol=Perfil.Rol.ALUMNO), write_only=True
    )

    class Meta:
        model = Resultado
        fields = ['id', 'simulacro', 'alumno', 'puntaje']

    def to_representation(self, instance):
        datos = super().to_representation(instance)
        datos['alumno'] = instance.matricula.alumno_id
        return datos

    def validate(self, datos):
        simulacro = datos['simulacro']
        alumno = datos.pop('alumno')
        matricula = Matricula.objects.filter(
            activa=True, alumno=alumno, seccion__curso=simulacro.curso
        ).first()
        if matricula is None:
            raise serializers.ValidationError(
                {'alumno': 'El alumno no está matriculado en el curso de este simulacro.'}
            )
        repetidos = Resultado.objects.filter(simulacro=simulacro, matricula=matricula)
        if self.instance:
            repetidos = repetidos.exclude(pk=self.instance.pk)
        if repetidos.exists():
            raise serializers.ValidationError(
                {'alumno': 'El alumno ya tiene un resultado en este simulacro.'}
            )
        datos['matricula'] = matricula
        return datos


class ResultadoViewSet(viewsets.ModelViewSet):
    permission_classes = [EsAdminOCoordinador]
    queryset = Resultado.objects.all().order_by('id')
    serializer_class = ResultadoSerializer

    def get_queryset(self):
        consulta = super().get_queryset()
        simulacro = self.request.query_params.get('simulacro')
        return consulta.filter(simulacro_id=simulacro) if simulacro else consulta


class RendimientoView(APIView):
    """Promedio de puntaje por sección en un simulacro (el más reciente por defecto)."""

    permission_classes = [EsAdminOCoordinador]

    def get(self, request):
        simulacros = Simulacro.objects.filter(activa=True)
        pedido = request.query_params.get('simulacro')
        if pedido:
            if not pedido.isdigit():
                return Response({'simulacro': 'Indica un identificador válido.'}, status=400)
            simulacro = simulacros.filter(pk=int(pedido)).first()
            if simulacro is None:
                return Response({'simulacro': 'No existe ese simulacro.'}, status=404)
        else:
            simulacro = simulacros.order_by('-fecha', '-id').first()

        if simulacro is None:
            return Response({'simulacro': None, 'umbral': UMBRAL_REFUERZO, 'secciones': []})

        filas = (
            Resultado.objects.filter(simulacro=simulacro)
            .values(
                'matricula__seccion',
                'matricula__seccion__nombre',
                'matricula__seccion__curso__nombre',
            )
            .annotate(
                promedio=Avg('puntaje'),
                maximo=Max('puntaje'),
                minimo=Min('puntaje'),
                evaluados=Count('id'),
            )
            .order_by('promedio')
        )
        secciones = [
            {
                'seccion': f['matricula__seccion'],
                'nombre': f"{f['matricula__seccion__curso__nombre']} - {f['matricula__seccion__nombre']}",
                'evaluados': f['evaluados'],
                'promedio': round(float(f['promedio']), 2),
                'maximo': float(f['maximo']),
                'minimo': float(f['minimo']),
                'necesita_refuerzo': float(f['promedio']) < UMBRAL_REFUERZO,
            }
            for f in filas
        ]
        return Response(
            {
                'simulacro': {
                    'id': simulacro.id,
                    'nombre': simulacro.nombre,
                    'fecha': simulacro.fecha.isoformat(),
                },
                'umbral': UMBRAL_REFUERZO,
                'secciones': secciones,
            }
        )
