from django.urls import path
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import TokenRefreshView

from .academias import AcademiaViewSet
from .auth import LoginView
from .dashboard import DashboardView
from .festivos import FestivoViewSet
from .horarios import DocenteViewSet, HorarioViewSet, SesionesView
from .marca import MarcaBorradorView, MarcaPublicarView, MarcaView
from .matriculas import (
    AlumnoViewSet,
    MatricularNuevoAlumnoView,
    MatriculaViewSet,
    SeccionViewSet,
)
from .rendimiento import RendimientoView, ResultadoViewSet, SimulacroViewSet
from .apoderados import ApoderadoViewSet, MiHijoView, MisHijosView
from .configuracion_ia import ConfiguracionIAView
from .ia import MiUsoIAView, UsarIAView, UsoDocentesView
from .usuarios import CambiarClaveView, UsuarioViewSet
from .views import AulaViewSet, CicloViewSet, CursoViewSet, SedeViewSet

router = DefaultRouter()
router.register('academias', AcademiaViewSet)
router.register('sedes', SedeViewSet)
router.register('aulas', AulaViewSet)
router.register('ciclos', CicloViewSet)
router.register('cursos', CursoViewSet)
router.register('usuarios', UsuarioViewSet)
router.register('apoderados', ApoderadoViewSet)
router.register('festivos', FestivoViewSet)
router.register('secciones', SeccionViewSet)
router.register('matriculas', MatriculaViewSet)
router.register('alumnos', AlumnoViewSet, basename='alumno')
router.register('horarios', HorarioViewSet)
router.register('simulacros', SimulacroViewSet)
router.register('resultados', ResultadoViewSet)
router.register('docentes', DocenteViewSet, basename='docente')

urlpatterns = router.urls + [
    path('auth/cambiar-clave/', CambiarClaveView.as_view()),
    path('mis-hijos/', MisHijosView.as_view()),
    path('mis-hijos/<int:pk>/', MiHijoView.as_view()),
    path('configuracion-ia/', ConfiguracionIAView.as_view()),
    path('ia/usar/', UsarIAView.as_view()),
    path('ia/uso/', MiUsoIAView.as_view()),
    path('ia/uso-docentes/', UsoDocentesView.as_view()),
    path('auth/login/', LoginView.as_view()),
    path('auth/refresh/', TokenRefreshView.as_view()),
    path('dashboard/', DashboardView.as_view()),
    path('matricular-nuevo/', MatricularNuevoAlumnoView.as_view()),
    path('sesiones/', SesionesView.as_view()),
    path('rendimiento/', RendimientoView.as_view()),
    path('marca/', MarcaView.as_view()),
    path('marca/borrador/', MarcaBorradorView.as_view()),
    path('marca/publicar/', MarcaPublicarView.as_view()),
]
