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


class RestablecerClaveTests(APITestCase):
    def setUp(self):
        from django.core import mail

        self.mail = mail
        crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        self.docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)

    def login(self, email, password='clave12345'):
        return self.client.post(
            '/api/auth/login/', {'username': email, 'password': password}, format='json'
        )

    def autenticar(self, email, password='clave12345'):
        token = self.login(email, password).data['access']
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')

    def test_admin_restablece_clave_y_se_envia_correo(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post(f'/api/usuarios/{self.docente.perfil.id}/restablecer-clave/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(len(self.mail.outbox), 1)
        self.assertEqual(self.mail.outbox[0].to, ['docente@test.pe'])
        self.client.credentials()
        self.assertEqual(self.login('docente@test.pe').status_code, status.HTTP_401_UNAUTHORIZED)

    def test_login_con_clave_temporal_pide_cambiarla(self):
        self.autenticar('admin@test.pe')
        self.client.post(f'/api/usuarios/{self.docente.perfil.id}/restablecer-clave/')
        cuerpo = self.mail.outbox[0].body
        temporal = [l for l in cuerpo.splitlines() if l.startswith('Contraseña temporal:')][0]
        temporal = temporal.split(': ')[1].strip()
        self.client.credentials()
        respuesta = self.login('docente@test.pe', temporal)
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertTrue(respuesta.data['debe_cambiar_clave'])

    def test_docente_no_puede_restablecer_claves(self):
        self.autenticar('docente@test.pe')
        respuesta = self.client.post(f'/api/usuarios/{self.docente.perfil.id}/restablecer-clave/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_usuario_cambia_su_clave_y_se_quita_la_marca(self):
        perfil = self.docente.perfil
        perfil.debe_cambiar_clave = True
        perfil.save()
        self.autenticar('docente@test.pe')
        mala = self.client.post(
            '/api/auth/cambiar-clave/',
            {'clave_actual': 'incorrecta', 'clave_nueva': 'nueva12345'},
            format='json',
        )
        self.assertEqual(mala.status_code, status.HTTP_400_BAD_REQUEST)
        buena = self.client.post(
            '/api/auth/cambiar-clave/',
            {'clave_actual': 'clave12345', 'clave_nueva': 'nueva12345'},
            format='json',
        )
        self.assertEqual(buena.status_code, status.HTTP_200_OK)
        self.client.credentials()
        respuesta = self.login('docente@test.pe', 'nueva12345')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertFalse(respuesta.data['debe_cambiar_clave'])
