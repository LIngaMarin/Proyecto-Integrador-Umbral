from rest_framework import status
from rest_framework.test import APITestCase

from .models import Apoderado, Perfil
from .test_usuarios import crear_usuario


class ApoderadosApiTests(APITestCase):
    def setUp(self):
        self.admin = crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        self.docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)

    def nuevo(self, **extra):
        datos = {
            'email': 'padre@test.pe',
            'first_name': 'Pedro',
            'last_name': 'Quispe',
            'password': 'clave12345',
            'telefono': '999888777',
            'hijos': [self.hijo.perfil.id],
        }
        datos.update(extra)
        return datos

    def test_admin_registra_apoderado_con_hijos(self):
        self.client.force_authenticate(self.admin)
        respuesta = self.client.post('/api/apoderados/', self.nuevo(), format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        apoderado = Apoderado.objects.get()
        self.assertEqual(apoderado.perfil.rol, Perfil.Rol.APODERADO)
        self.assertEqual(list(apoderado.hijos.all()), [self.hijo.perfil])

    def test_docente_no_puede_registrar_apoderados(self):
        self.client.force_authenticate(self.docente)
        respuesta = self.client.post('/api/apoderados/', self.nuevo(), format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_se_puede_vincular_a_quien_no_es_alumno(self):
        self.client.force_authenticate(self.admin)
        respuesta = self.client.post(
            '/api/apoderados/', self.nuevo(hijos=[self.docente.perfil.id]), format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(Apoderado.objects.count(), 0)

    def test_correo_repetido_se_rechaza(self):
        self.client.force_authenticate(self.admin)
        self.client.post('/api/apoderados/', self.nuevo(), format='json')
        respuesta = self.client.post('/api/apoderados/', self.nuevo(), format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_vincular_y_desvincular_alumno(self):
        self.client.force_authenticate(self.admin)
        creado = self.client.post('/api/apoderados/', self.nuevo(hijos=[]), format='json')
        ruta = f"/api/apoderados/{creado.data['id']}"
        vincular = self.client.post(
            f'{ruta}/vincular/', {'alumno': self.hijo.perfil.id}, format='json'
        )
        self.assertEqual(vincular.status_code, status.HTTP_200_OK)
        self.assertEqual(Apoderado.objects.get().hijos.count(), 1)
        self.client.post(f'{ruta}/desvincular/', {'alumno': self.hijo.perfil.id}, format='json')
        self.assertEqual(Apoderado.objects.get().hijos.count(), 0)
        invalido = self.client.post(
            f'{ruta}/vincular/', {'alumno': self.docente.perfil.id}, format='json'
        )
        self.assertEqual(invalido.status_code, status.HTTP_400_BAD_REQUEST)
