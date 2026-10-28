from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, ConfiguracionIA, Perfil
from .test_usuarios import crear_usuario


class ConfiguracionIAApiTests(APITestCase):
    def setUp(self):
        self.admin = crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        self.docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.academia = Academia.objects.create(nombre='Academia Test')
        self.ruta = f'/api/configuracion-ia/?academia={self.academia.id}'

    def test_consulta_crea_la_configuracion_por_defecto(self):
        self.client.force_authenticate(self.admin)
        respuesta = self.client.get(self.ruta)
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertTrue(respuesta.data['activa'])
        self.assertEqual(respuesta.data['limite_por_docente'], 100)
        self.assertEqual(ConfiguracionIA.objects.count(), 1)

    def test_admin_actualiza_estado_y_limite(self):
        self.client.force_authenticate(self.admin)
        respuesta = self.client.put(
            self.ruta, {'activa': False, 'limite_por_docente': 20}, format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        config = ConfiguracionIA.objects.get(academia=self.academia)
        self.assertFalse(config.activa)
        self.assertEqual(config.limite_por_docente, 20)

    def test_limite_negativo_se_rechaza(self):
        self.client.force_authenticate(self.admin)
        respuesta = self.client.put(self.ruta, {'limite_por_docente': -5}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_docente_no_puede_configurar_la_ia(self):
        self.client.force_authenticate(self.docente)
        respuesta = self.client.put(self.ruta, {'activa': False}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_sin_academia_se_pide_indicarla(self):
        self.client.force_authenticate(self.admin)
        respuesta = self.client.get('/api/configuracion-ia/')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
