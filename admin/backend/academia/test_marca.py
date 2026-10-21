from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Perfil


def crear_usuario(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    Perfil.objects.create(user=user, rol=rol)


class MarcaApiTests(APITestCase):
    def setUp(self):
        crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        crear_usuario('coord@test.pe', Perfil.Rol.COORDINADOR)
        Academia.objects.create(nombre='Academia Test')

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def test_lectura_publica_con_colores_por_defecto(self):
        respuesta = self.client.get('/api/marca/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['color_primario'], '#2563EB')

    def test_admin_actualiza_los_colores(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.put(
            '/api/marca/', {'color_primario': '#FF0000'}, format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.client.credentials()
        self.assertEqual(self.client.get('/api/marca/').data['color_primario'], '#FF0000')

    def test_color_invalido_se_rechaza(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.put('/api/marca/', {'color_primario': 'rojo'}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('color_primario', respuesta.data)

    def test_coordinador_no_puede_modificar(self):
        self.autenticar('coord@test.pe')
        respuesta = self.client.put('/api/marca/', {'color_primario': '#FF0000'}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_sin_token_no_puede_modificar(self):
        respuesta = self.client.put('/api/marca/', {'color_primario': '#FF0000'}, format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_401_UNAUTHORIZED)
