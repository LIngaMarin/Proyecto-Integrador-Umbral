from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Festivo, Perfil


def crear_usuario(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    Perfil.objects.create(user=user, rol=rol)


class FestivosApiTests(APITestCase):
    def setUp(self):
        crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        crear_usuario('coord@test.pe', Perfil.Rol.COORDINADOR)
        self.academia = Academia.objects.create(nombre='Academia Test')

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def datos(self, fecha='2026-12-25', nombre='Navidad'):
        return {'academia': self.academia.id, 'fecha': fecha, 'nombre': nombre}

    def test_admin_registra_un_festivo(self):
        self.autenticar('admin@test.pe')
        respuesta = self.client.post('/api/festivos/', self.datos(), format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Festivo.objects.count(), 1)

    def test_no_se_repite_la_fecha_en_la_misma_academia(self):
        self.autenticar('admin@test.pe')
        self.client.post('/api/festivos/', self.datos(), format='json')
        respuesta = self.client.post(
            '/api/festivos/', self.datos(nombre='Otro nombre'), format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_coordinador_puede_ver_pero_no_crear(self):
        self.autenticar('coord@test.pe')
        lista = self.client.get('/api/festivos/')
        crear = self.client.post('/api/festivos/', self.datos(), format='json')
        self.assertEqual(lista.status_code, status.HTTP_200_OK)
        self.assertEqual(crear.status_code, status.HTTP_403_FORBIDDEN)

    def test_filtra_por_anio(self):
        Festivo.objects.create(academia=self.academia, fecha='2026-07-28', nombre='Fiestas Patrias')
        Festivo.objects.create(academia=self.academia, fecha='2027-01-01', nombre='Año Nuevo')
        self.autenticar('admin@test.pe')
        respuesta = self.client.get('/api/festivos/?anio=2026')
        self.assertEqual(len(respuesta.data), 1)
        self.assertEqual(respuesta.data[0]['nombre'], 'Fiestas Patrias')

    def test_desactivar_no_borra_el_festivo(self):
        festivo = Festivo.objects.create(
            academia=self.academia, fecha='2026-12-25', nombre='Navidad'
        )
        self.autenticar('admin@test.pe')
        respuesta = self.client.delete(f'/api/festivos/{festivo.id}/')
        self.assertEqual(respuesta.status_code, status.HTTP_204_NO_CONTENT)
        festivo.refresh_from_db()
        self.assertFalse(festivo.activo)
