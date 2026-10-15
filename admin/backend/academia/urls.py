from django.urls import path
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import TokenRefreshView

from .academias import AcademiaViewSet
from .auth import LoginView
from .dashboard import DashboardView
from .festivos import FestivoViewSet
from .usuarios import UsuarioViewSet
from .views import AulaViewSet, CicloViewSet, CursoViewSet, SedeViewSet

router = DefaultRouter()
router.register('academias', AcademiaViewSet)
router.register('sedes', SedeViewSet)
router.register('aulas', AulaViewSet)
router.register('ciclos', CicloViewSet)
router.register('cursos', CursoViewSet)
router.register('usuarios', UsuarioViewSet)
router.register('festivos', FestivoViewSet)

urlpatterns = router.urls + [
    path('auth/login/', LoginView.as_view()),
    path('auth/refresh/', TokenRefreshView.as_view()),
    path('dashboard/', DashboardView.as_view()),
]
