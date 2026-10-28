from datetime import timedelta

from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, ConfiguracionIA, Perfil, Sede, UsoIA
from .test_usuarios import crear_usuario


class UsoIAApiTests(APITestCase):
    def setUp(self):
        self.academia = Academia.objects.create(nombre='Academia Test')
        self.sede = Sede.objects.create(
            academia=self.academia, nombre='Sede Central', direccion='Av. Principal 123'
        )
        self.admin = crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        self.alumno = crear_usuario('alumno@test.pe', Perfil.Rol.ALUMNO)
        self.docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.docente.perfil.sede = self.sede
        self.docente.perfil.save()

    def configurar(self, activa=True, limite=100):
        return ConfiguracionIA.objects.update_or_create(
            academia=self.academia,
            defaults={'activa': activa, 'limite_por_docente': limite},
        )[0]

    def test_docente_registra_un_uso(self):
        self.client.force_authenticate(self.docente)
        respuesta = self.client.post('/api/ia/usar/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['usados'], 1)
        self.assertEqual(respuesta.data['restantes'], 99)
        self.assertEqual(UsoIA.objects.count(), 1)

    def test_se_bloquea_al_llegar_al_limite(self):
        self.configurar(limite=2)
        self.client.force_authenticate(self.docente)
        self.client.post('/api/ia/usar/')
        self.client.post('/api/ia/usar/')
        respuesta = self.client.post('/api/ia/usar/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)
        self.assertIn('límite', respuesta.data['error'])
        self.assertEqual(UsoIA.objects.count(), 2)

    def test_ia_desactivada_bloquea_el_uso(self):
        self.configurar(activa=False)
        self.client.force_authenticate(self.docente)
        respuesta = self.client.post('/api/ia/usar/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(UsoIA.objects.count(), 0)

    def test_docente_sin_sede_no_puede_usar_la_ia(self):
        sin_sede = crear_usuario('sinsede@test.pe', Perfil.Rol.DOCENTE)
        self.client.force_authenticate(sin_sede)
        respuesta = self.client.post('/api/ia/usar/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_los_usos_de_meses_anteriores_no_cuentan(self):
        self.configurar(limite=1)
        UsoIA.objects.create(docente=self.docente.perfil)
        UsoIA.objects.update(creado_en=timezone.now() - timedelta(days=40))
        self.client.force_authenticate(self.docente)
        respuesta = self.client.post('/api/ia/usar/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)

    def test_docente_consulta_su_propio_uso(self):
        self.client.force_authenticate(self.docente)
        self.client.post('/api/ia/usar/')
        respuesta = self.client.get('/api/ia/uso/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['usados'], 1)
        self.assertEqual(respuesta.data['limite'], 100)

    def test_admin_ve_el_uso_por_docente(self):
        UsoIA.objects.create(docente=self.docente.perfil)
        self.client.force_authenticate(self.admin)
        respuesta = self.client.get(f'/api/ia/uso-docentes/?academia={self.academia.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(len(respuesta.data), 1)
        self.assertEqual(respuesta.data[0]['email'], 'docente@test.pe')
        self.assertEqual(respuesta.data[0]['usados'], 1)

    def test_otros_roles_no_pueden_registrar_usos(self):
        for usuario in (self.admin, self.alumno):
            self.client.force_authenticate(usuario)
            respuesta = self.client.post('/api/ia/usar/')
            self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)
