from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Sede


class AcademiaApiTests(APITestCase):
    def setUp(self):
        self.academia = Academia.objects.create(nombre='Academia Test')
        self.sede = Sede.objects.create(
            academia=self.academia, nombre='Sede Centro', direccion='Av. 1'
        )

    def test_crear_sede(self):
        respuesta = self.client.post(
            '/api/sedes/',
            {'academia': self.academia.id, 'nombre': 'Sede Norte', 'direccion': 'Av. 2'},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_sede_duplicada_se_rechaza(self):
        respuesta = self.client.post(
            '/api/sedes/',
            {'academia': self.academia.id, 'nombre': 'Sede Centro', 'direccion': 'Av. 1'},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_eliminar_sede_la_desactiva(self):
        respuesta = self.client.delete(f'/api/sedes/{self.sede.id}/')
        self.assertEqual(respuesta.status_code, status.HTTP_204_NO_CONTENT)
        self.sede.refresh_from_db()
        self.assertFalse(self.sede.activa)

    def test_crear_aula(self):
        respuesta = self.client.post(
            '/api/aulas/',
            {'sede': self.sede.id, 'nombre': 'Aula 101', 'capacidad': 30},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_aula_con_capacidad_cero_se_rechaza(self):
        respuesta = self.client.post(
            '/api/aulas/',
            {'sede': self.sede.id, 'nombre': 'Aula 102', 'capacidad': 0},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_ciclo_con_fechas_invalidas_se_rechaza(self):
        respuesta = self.client.post(
            '/api/ciclos/',
            {
                'academia': self.academia.id,
                'nombre': 'Ciclo malo',
                'fecha_inicio': '2026-10-01',
                'fecha_fin': '2026-09-01',
            },
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_crear_ciclo_y_curso(self):
        ciclo = self.client.post(
            '/api/ciclos/',
            {
                'academia': self.academia.id,
                'nombre': 'Ciclo 2026-II',
                'fecha_inicio': '2026-10-01',
                'fecha_fin': '2027-02-01',
            },
            format='json',
        )
        self.assertEqual(ciclo.status_code, status.HTTP_201_CREATED)
        curso = self.client.post(
            '/api/cursos/',
            {'ciclo': ciclo.data['id'], 'nombre': 'Matemática'},
            format='json',
        )
        self.assertEqual(curso.status_code, status.HTTP_201_CREATED)
