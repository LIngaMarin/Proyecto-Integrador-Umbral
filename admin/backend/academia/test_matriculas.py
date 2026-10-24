from datetime import date

from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Aula, Ciclo, Curso, Perfil, Sede, Seccion


def crear_perfil(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    return Perfil.objects.create(user=user, rol=rol)


class MatriculasApiTests(APITestCase):
    def setUp(self):
        crear_perfil('coord@test.pe', Perfil.Rol.COORDINADOR)
        crear_perfil('docente@test.pe', Perfil.Rol.DOCENTE)
        self.ana = crear_perfil('ana@test.pe', Perfil.Rol.ALUMNO)
        self.luis = crear_perfil('luis@test.pe', Perfil.Rol.ALUMNO)

        academia = Academia.objects.create(nombre='Academia Test')
        sede = Sede.objects.create(academia=academia, nombre='Central', direccion='Av. 1')
        self.aula = Aula.objects.create(sede=sede, nombre='A-101', capacidad=30)
        ciclo = Ciclo.objects.create(
            academia=academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        self.curso = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.seccion_a = Seccion.objects.create(curso=self.curso, nombre='A', cupo=1)
        self.seccion_b = Seccion.objects.create(curso=self.curso, nombre='B', cupo=5)

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def matricular(self, alumno, seccion):
        return self.client.post(
            '/api/matriculas/', {'alumno': alumno.id, 'seccion': seccion.id}, format='json'
        )

    def test_coordinador_matricula_a_un_alumno(self):
        self.autenticar('coord@test.pe')
        respuesta = self.matricular(self.ana, self.seccion_a)
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_docente_no_puede_matricular(self):
        self.autenticar('docente@test.pe')
        respuesta = self.matricular(self.ana, self.seccion_a)
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_se_matricula_dos_veces_en_la_misma_seccion(self):
        self.autenticar('coord@test.pe')
        self.matricular(self.ana, self.seccion_b)
        respuesta = self.matricular(self.ana, self.seccion_b)
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_seccion_llena_rechaza_nuevas_matriculas(self):
        self.autenticar('coord@test.pe')
        self.matricular(self.ana, self.seccion_a)
        respuesta = self.matricular(self.luis, self.seccion_a)
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('seccion', respuesta.data)

    def test_solo_se_matriculan_alumnos(self):
        self.autenticar('coord@test.pe')
        docente = Perfil.objects.get(user__email='docente@test.pe')
        respuesta = self.matricular(docente, self.seccion_b)
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_no_puede_estar_en_dos_secciones_del_mismo_curso(self):
        self.autenticar('coord@test.pe')
        self.matricular(self.ana, self.seccion_a)
        respuesta = self.matricular(self.ana, self.seccion_b)
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_desactivar_una_matricula_libera_el_cupo(self):
        self.autenticar('coord@test.pe')
        creada = self.matricular(self.ana, self.seccion_a)
        self.client.delete(f"/api/matriculas/{creada.data['id']}/")
        respuesta = self.matricular(self.luis, self.seccion_a)
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_cupo_no_puede_superar_la_capacidad_del_aula(self):
        self.autenticar('coord@test.pe')
        respuesta = self.client.post(
            '/api/secciones/',
            {'curso': self.curso.id, 'nombre': 'C', 'aula': self.aula.id, 'cupo': 40},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('cupo', respuesta.data)
