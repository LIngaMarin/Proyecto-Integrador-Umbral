from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Perfil, Sede


class DesactivarAcademiaTests(APITestCase):
    def setUp(self):
        user = User.objects.create_user(
            username='admin@test.pe', email='admin@test.pe', password='clave12345'
        )
        Perfil.objects.create(user=user, rol=Perfil.Rol.ADMIN)
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': 'admin@test.pe', 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def test_desactivar_no_borra_la_academia_ni_sus_sedes(self):
        academia = Academia.objects.create(nombre='Academia Test')
        Sede.objects.create(academia=academia, nombre='Central', direccion='Av. 1')

        respuesta = self.client.delete(f'/api/academias/{academia.id}/')

        self.assertEqual(respuesta.status_code, status.HTTP_204_NO_CONTENT)
        academia.refresh_from_db()
        self.assertFalse(academia.activa)
        self.assertEqual(Sede.objects.filter(academia=academia).count(), 1)
