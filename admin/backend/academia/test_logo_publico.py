import tempfile
from io import BytesIO

from django.core.files.base import ContentFile
from django.test import override_settings
from PIL import Image
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia


def png():
    buffer = BytesIO()
    Image.new('RGB', (10, 10), 'red').save(buffer, 'PNG')
    return ContentFile(buffer.getvalue())


class LogoPublicoTests(APITestCase):
    def setUp(self):
        self.carpeta = tempfile.TemporaryDirectory()
        self.addCleanup(self.carpeta.cleanup)
        self.ajustes = override_settings(MEDIA_ROOT=self.carpeta.name)
        self.ajustes.enable()
        self.addCleanup(self.ajustes.disable)

    def test_no_pide_sesion(self):
        Academia.objects.create(nombre='Academia Uno')
        respuesta = self.client.get('/api/logo/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)

    def test_devuelve_la_direccion_completa_del_logo(self):
        academia = Academia.objects.create(nombre='Academia Uno')
        academia.logo.save('logo.png', png())
        respuesta = self.client.get('/api/logo/')
        self.assertEqual(respuesta.data['nombre'], 'Academia Uno')
        self.assertTrue(respuesta.data['logo'].startswith('http://testserver/media/'))
        self.assertTrue(respuesta.data['logo'].endswith('.png'))

    def test_sin_logo_devuelve_null(self):
        Academia.objects.create(nombre='Sin logo')
        respuesta = self.client.get('/api/logo/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIsNone(respuesta.data['logo'])

    def test_usa_la_primera_academia_activa_por_defecto(self):
        Academia.objects.create(nombre='Inactiva', activa=False)
        activa = Academia.objects.create(nombre='Activa')
        respuesta = self.client.get('/api/logo/')
        self.assertEqual(respuesta.data['academia'], activa.id)

    def test_permite_pedir_una_academia_concreta(self):
        Academia.objects.create(nombre='Primera')
        segunda = Academia.objects.create(nombre='Segunda')
        respuesta = self.client.get(f'/api/logo/?academia={segunda.id}')
        self.assertEqual(respuesta.data['nombre'], 'Segunda')

    def test_academia_inexistente_o_invalida_da_404(self):
        Academia.objects.create(nombre='Primera')
        self.assertEqual(self.client.get('/api/logo/?academia=999').status_code, 404)
        self.assertEqual(self.client.get('/api/logo/?academia=abc').status_code, 404)

    def test_academia_desactivada_da_404(self):
        inactiva = Academia.objects.create(nombre='Cerrada', activa=False)
        self.assertEqual(self.client.get(f'/api/logo/?academia={inactiva.id}').status_code, 404)

    def test_sin_academias_da_404(self):
        self.assertEqual(self.client.get('/api/logo/').status_code, 404)

    def test_un_token_invalido_no_bloquea_la_lectura_publica(self):
        Academia.objects.create(nombre='Academia Uno')
        self.client.credentials(HTTP_AUTHORIZATION='Bearer token-invalido')
        self.assertEqual(self.client.get('/api/logo/').status_code, 200)
