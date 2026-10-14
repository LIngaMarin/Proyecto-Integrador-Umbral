from rest_framework.routers import DefaultRouter

from .views import AulaViewSet, CicloViewSet, CursoViewSet, SedeViewSet

router = DefaultRouter()
router.register('sedes', SedeViewSet)
router.register('aulas', AulaViewSet)
router.register('ciclos', CicloViewSet)
router.register('cursos', CursoViewSet)

urlpatterns = router.urls
