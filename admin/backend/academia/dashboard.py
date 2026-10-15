from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import EsAdminOCoordinador
from .models import Aula, Ciclo, Curso, Perfil, Sede


def calcular_indicadores():
    perfiles_activos = Perfil.objects.filter(user__is_active=True)
    return {
        'sedes_activas': Sede.objects.filter(activa=True).count(),
        'aulas_activas': Aula.objects.filter(activa=True).count(),
        'ciclos_activos': Ciclo.objects.filter(activo=True).count(),
        'cursos_activos': Curso.objects.filter(activo=True).count(),
        'docentes_activos': perfiles_activos.filter(rol=Perfil.Rol.DOCENTE).count(),
        'alumnos_matriculados': perfiles_activos.filter(rol=Perfil.Rol.ALUMNO).count(),
    }


class DashboardView(APIView):
    permission_classes = [EsAdminOCoordinador]

    def get(self, request):
        return Response(calcular_indicadores())
