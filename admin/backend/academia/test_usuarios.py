from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Perfil


def crear_usuario(email, rol, password='clave12345'):
    user = User.objects.create_user(username=email, email=email, password=password)
    Perfil.objects.create(user=user, rol=rol)
    return user


class UsuariosApiTests(APITestCase):
    def setUp(self):
        crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)

    def login(self, email):
        return self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )

    def autenticar(self, email):
        token = self.login(email).data['access']
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')

    def nuevo_usuario(self, email='nuevo@test.pe'):
        return {
            'email': email,
            'first_name': 'Nuevo',
            'last_name': 'Usuario',
            'password': 'clave12345',
            'rol': 'DOCENTE',
        }

    def test_login_devuelve_token_y_rol(self):
        respuesta = self.login('admin@test.pe')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIn('access', respuesta.data)
        self.assertEqual(respuesta.data['rol'], 'ADMIN')

    def test_usuarios_sin_token_se_rechaza(self):
        respuesta = self.client.get('/api/usuarios/')
        self.assertEqual(respuesta.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_docente_no_puede_listar_usuarios(self):
        self.autenticar('docente@test.pe')
        respuesta = self.client.get('/api/usuarios/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_admin_crea_usuario(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post('/api/usuarios/', self.nuevo_usuario(), format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_correo_duplicado_se_rechaza(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post(
            '/api/usuarios/', self.nuevo_usuario('docente@test.pe'), format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_admin_cambia_el_rol(self):
        self.autenticar('admin@test.pe')
        creado = self.client.post('/api/usuarios/', self.nuevo_usuario(), format='json')
        respuesta = self.client.patch(
            f"/api/usuarios/{creado.data['id']}/", {'rol': 'COORDINADOR'}, format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['rol'], 'COORDINADOR')

    def test_eliminar_usuario_lo_desactiva(self):
        self.autenticar('admin@test.pe')
        creado = self.client.post('/api/usuarios/', self.nuevo_usuario(), format='json')
        respuesta = self.client.delete(f"/api/usuarios/{creado.data['id']}/")
        self.assertEqual(respuesta.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(User.objects.get(username='nuevo@test.pe').is_active)
