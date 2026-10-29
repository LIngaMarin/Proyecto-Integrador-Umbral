from datetime import date
from io import BytesIO

from django.contrib.auth.models import User
from django.core import mail
from openpyxl import Workbook
from rest_framework import status
from rest_framework.test import APITestCase

from .carga_masiva import COLUMNAS
from .models import Academia, Ciclo, Curso, Matricula, Perfil, Seccion
from .test_usuarios import crear_usuario

RUTA = '/api/alumnos-masivo/cargar/'


def excel(filas, encabezado=COLUMNAS):
    libro = Workbook()
    hoja = libro.active
    hoja.title = 'Alumnos'
    hoja.append(encabezado)
    for fila in filas:
        hoja.append(fila)
    salida = BytesIO()
    libro.save(salida)
    salida.seek(0)
    salida.name = 'alumnos.xlsx'
    return salida


def alumno(correo, curso='Matemática', seccion='A'):
    return ['Ana', 'Pérez', correo, curso, seccion]


class ImportarAlumnosTests(APITestCase):
    def setUp(self):
        self.admin = crear_usuario('admin@test.pe', Perfil.Rol.ADMIN)
        self.docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        academia = Academia.objects.create(nombre='Academia Test')
        ciclo = Ciclo.objects.create(
            academia=academia,
            nombre='2026-II',
            fecha_inicio=date(2026, 8, 1),
            fecha_fin=date(2026, 12, 15),
        )
        curso = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.seccion = Seccion.objects.create(curso=curso, nombre='A', cupo=2)
        self.client.force_authenticate(self.admin)

    def cargar(self, archivo):
        return self.client.post(RUTA, {'archivo': archivo}, format='multipart')

    def test_carga_valida_crea_alumnos_matriculados(self):
        respuesta = self.cargar(excel([alumno('uno@test.pe'), alumno('dos@test.pe')]))
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['creados'], 2)
        self.assertEqual(respuesta.data['con_errores'], 0)
        perfil = User.objects.get(username='uno@test.pe').perfil
        self.assertEqual(perfil.rol, Perfil.Rol.ALUMNO)
        self.assertTrue(perfil.debe_cambiar_clave)
        self.assertEqual(Matricula.objects.count(), 2)
        self.assertEqual(len(mail.outbox), 2)

    def test_las_filas_con_error_se_reportan_sin_detener_las_validas(self):
        respuesta = self.cargar(
            excel(
                [
                    alumno('valido@test.pe'),
                    alumno('correo-malo'),
                    alumno('otro@test.pe', seccion='Z'),
                ]
            )
        )
        self.assertEqual(respuesta.data['creados'], 1)
        self.assertEqual(respuesta.data['con_errores'], 2)
        self.assertEqual([e['fila'] for e in respuesta.data['errores']], [3, 4])

    def test_se_detectan_correos_repetidos_y_ya_registrados(self):
        crear_usuario('existe@test.pe', Perfil.Rol.ALUMNO)
        respuesta = self.cargar(
            excel(
                [alumno('nuevo@test.pe'), alumno('nuevo@test.pe'), alumno('existe@test.pe')]
            )
        )
        self.assertEqual(respuesta.data['creados'], 1)
        self.assertEqual(respuesta.data['con_errores'], 2)

    def test_se_respeta_el_cupo_de_la_seccion(self):
        respuesta = self.cargar(
            excel([alumno('a@test.pe'), alumno('b@test.pe'), alumno('c@test.pe')])
        )
        self.assertEqual(respuesta.data['creados'], 2)
        self.assertIn('cupo', respuesta.data['errores'][0]['errores'][0])
        self.assertEqual(Matricula.objects.filter(seccion=self.seccion).count(), 2)

    def test_faltan_columnas_se_rechaza_el_archivo(self):
        respuesta = self.cargar(excel([['Ana', 'Pérez']], encabezado=['Nombres', 'Apellidos']))
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_un_archivo_que_no_es_excel_se_rechaza(self):
        falso = BytesIO(b'esto no es un excel')
        falso.name = 'alumnos.xlsx'
        respuesta = self.cargar(falso)
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_docente_no_puede_cargar_alumnos(self):
        self.client.force_authenticate(self.docente)
        respuesta = self.cargar(excel([alumno('uno@test.pe')]))
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)
