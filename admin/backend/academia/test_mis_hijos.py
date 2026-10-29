from rest_framework import status
from rest_framework.test import APITestCase

from .models import Apoderado, Perfil
from .test_usuarios import crear_usuario


class MisHijosApiTests(APITestCase):
    def setUp(self):
        self.admin = crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        self.docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.hijo_propio = crear_usuario('propio@test.pe', Perfil.Rol.ALUMNO)
        self.hijo_ajeno = crear_usuario('ajeno@test.pe', Perfil.Rol.ALUMNO)
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        otro_padre = crear_usuario('otro@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=self.padre.perfil).hijos.add(self.hijo_propio.perfil)
        Apoderado.objects.create(perfil=otro_padre.perfil).hijos.add(self.hijo_ajeno.perfil)

    def test_apoderado_ve_solo_a_sus_hijos(self):
        self.client.force_authenticate(self.padre)
        respuesta = self.client.get('/api/mis-hijos/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual([h['email'] for h in respuesta.data], ['propio@test.pe'])

    def test_apoderado_ve_el_detalle_de_su_hijo(self):
        self.client.force_authenticate(self.padre)
        respuesta = self.client.get(f'/api/mis-hijos/{self.hijo_propio.perfil.id}/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['email'], 'propio@test.pe')

    def test_apoderado_no_ve_el_hijo_de_otro(self):
        self.client.force_authenticate(self.padre)
        respuesta = self.client.get(f'/api/mis-hijos/{self.hijo_ajeno.perfil.id}/')
        self.assertEqual(respuesta.status_code, status.HTTP_404_NOT_FOUND)

    def test_otros_roles_no_pueden_usar_mis_hijos(self):
        for usuario in (self.admin, self.docente, self.hijo_propio):
            self.client.force_authenticate(usuario)
            respuesta = self.client.get('/api/mis-hijos/')
            self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_sin_sesion_se_rechaza(self):
        respuesta = self.client.get('/api/mis-hijos/')
        self.assertIn(
            respuesta.status_code,
            (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN),
        )
