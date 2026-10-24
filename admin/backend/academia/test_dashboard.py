from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Perfil, Sede


def crear_usuario(email, rol, activo=True):
    user = User.objects.create_user(
        username=email, email=email, password='clave12345', is_active=activo
    )
    Perfil.objects.create(user=user, rol=rol)
    return user


class DashboardApiTests(APITestCase):
    def setUp(self):
        crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        crear_usuario('coord@test.pe', Perfil.Rol.COORDINADOR)
        crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def test_sin_token_se_rechaza(self):
        respuesta = self.client.get('/api/dashboard/')
        self.assertEqual(respuesta.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_docente_no_puede_ver_el_panel(self):
        self.autenticar('docente@test.pe')
        respuesta = self.client.get('/api/dashboard/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_coordinador_puede_ver_el_panel(self):
        self.autenticar('coord@test.pe')
        respuesta = self.client.get('/api/dashboard/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)

    def test_cuenta_solo_los_registros_activos(self):
        academia = Academia.objects.create(nombre='Academia Test')
        Sede.objects.create(academia=academia, nombre='S1', direccion='Dir 1')
        Sede.objects.create(academia=academia, nombre='S2', direccion='Dir 2')
        Sede.objects.create(academia=academia, nombre='S3', direccion='Dir 3', activa=False)
        crear_usuario('d2@test.pe', Perfil.Rol.DOCENTE)
        crear_usuario('d3@test.pe', Perfil.Rol.DOCENTE, activo=False)
        crear_usuario('a1@test.pe', Perfil.Rol.ALUMNO)
        crear_usuario('a2@test.pe', Perfil.Rol.ALUMNO)
        crear_usuario('a3@test.pe', Perfil.Rol.ALUMNO, activo=False)

        self.autenticar('admin@test.pe')
        datos = self.client.get('/api/dashboard/').data

        self.assertEqual(datos['sedes_activas'], 2)
        self.assertEqual(datos['docentes_activos'], 2)
        self.assertEqual(datos['alumnos_matriculados'], 2)
