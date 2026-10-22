from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Perfil


def crear_usuario(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    Perfil.objects.create(user=user, rol=rol)


class MarcaBorradorTests(APITestCase):
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

    def guardar_borrador(self, color='#00FF00'):
        return self.client.put(
            '/api/marca/borrador/', {'color_primario': color}, format='json'
        )

    def test_el_borrador_no_cambia_los_colores_publicados(self):
        self.autenticar('admin@test.pe')
        self.guardar_borrador()
        borrador = self.client.get('/api/marca/borrador/').data
        publico = self.client.get('/api/marca/').data
        self.assertTrue(borrador['hay_borrador'])
        self.assertEqual(borrador['color_primario'], '#00FF00')
        self.assertEqual(publico['color_primario'], '#2563EB')

    def test_publicar_aplica_el_borrador(self):
        self.autenticar('admin@test.pe')
        self.guardar_borrador()
        respuesta = self.client.post('/api/marca/publicar/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(self.client.get('/api/marca/').data['color_primario'], '#00FF00')
        self.assertFalse(self.client.get('/api/marca/borrador/').data['hay_borrador'])

    def test_publicar_sin_borrador_se_rechaza(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post('/api/marca/publicar/')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_descartar_el_borrador_deja_lo_publicado(self):
        self.autenticar('admin@test.pe')
        self.guardar_borrador()
        respuesta = self.client.delete('/api/marca/borrador/')
        self.assertEqual(respuesta.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(self.client.get('/api/marca/borrador/').data['hay_borrador'])
        self.assertEqual(self.client.get('/api/marca/').data['color_primario'], '#2563EB')

    def test_color_invalido_en_el_borrador_se_rechaza(self):
        self.autenticar('admin@test.pe')
        respuesta = self.guardar_borrador('verde')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_coordinador_ve_el_borrador_pero_no_publica(self):
        self.autenticar('coord@test.pe')
        ver = self.client.get('/api/marca/borrador/')
        publicar = self.client.post('/api/marca/publicar/')
        self.assertEqual(ver.status_code, status.HTTP_200_OK)
        self.assertEqual(publicar.status_code, status.HTTP_403_FORBIDDEN)
