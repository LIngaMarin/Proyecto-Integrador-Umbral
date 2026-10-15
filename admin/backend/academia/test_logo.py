import tempfile
from io import BytesIO
from unittest.mock import patch

from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from PIL import Image
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Perfil


def imagen(nombre='logo.png', formato='PNG'):
    buffer = BytesIO()
    Image.new('RGB', (10, 10), 'red').save(buffer, formato)
    return SimpleUploadedFile(nombre, buffer.getvalue(), content_type='image/png')


def crear_usuario(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    Perfil.objects.create(user=user, rol=rol)


@override_settings(MEDIA_ROOT=tempfile.mkdtemp())
class LogoAcademiaTests(APITestCase):
    def setUp(self):
        crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        crear_usuario('coord@test.pe', Perfil.Rol.COORDINADOR)

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def test_admin_crea_academia_con_logo(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post(
            '/api/academias/', {'nombre': 'Academia Sol', 'logo': imagen()}, format='multipart'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertIn('/media/logos/', respuesta.data['logo'])

    def test_academia_sin_logo_sigue_funcionando(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post('/api/academias/', {'nombre': 'Sin logo'}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertIsNone(respuesta.data['logo'])

    def test_archivo_que_no_es_imagen_se_rechaza(self):
        self.autenticar('admin@test.pe')
        falso = SimpleUploadedFile('logo.png', b'esto no es una imagen', content_type='image/png')
        respuesta = self.client.post(
            '/api/academias/', {'nombre': 'Falsa', 'logo': falso}, format='multipart'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('logo', respuesta.data)

    def test_formato_no_permitido_se_rechaza(self):
        self.autenticar('admin@test.pe')
        gif = imagen('logo.gif', 'GIF')
        respuesta = self.client.post(
            '/api/academias/', {'nombre': 'Gif', 'logo': gif}, format='multipart'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_logo_muy_pesado_se_rechaza(self):
        self.autenticar('admin@test.pe')
        with patch('academia.academias.LOGO_MAX_BYTES', 10):
            respuesta = self.client.post(
                '/api/academias/', {'nombre': 'Pesada', 'logo': imagen()}, format='multipart'
            )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_coordinador_no_puede_crear_academias(self):
        self.autenticar('coord@test.pe')
        respuesta = self.client.post('/api/academias/', {'nombre': 'X'}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)
