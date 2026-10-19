from datetime import date

from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Ciclo, Curso, Matricula, Perfil, Resultado, Seccion, Simulacro


def crear_perfil(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    return Perfil.objects.create(user=user, rol=rol)


class RendimientoApiTests(APITestCase):
    def setUp(self):
        crear_perfil('coord@test.pe', Perfil.Rol.COORDINADOR)
        crear_perfil('docente@test.pe', Perfil.Rol.DOCENTE)
        self.zoe = crear_perfil('zoe@test.pe', Perfil.Rol.ALUMNO)

        academia = Academia.objects.create(nombre='Academia Test')
        ciclo = Ciclo.objects.create(
            academia=academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        self.curso = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        sec_a = Seccion.objects.create(curso=self.curso, nombre='A', cupo=10)
        sec_b = Seccion.objects.create(curso=self.curso, nombre='B', cupo=10)
        self.sim1 = Simulacro.objects.create(
            curso=self.curso, nombre='Simulacro 1', fecha=date(2026, 9, 15)
        )

        self.matriculas = {}
        for correo, seccion in [('ana', sec_a), ('bea', sec_a), ('luis', sec_b), ('mia', sec_b)]:
            alumno = crear_perfil(f'{correo}@test.pe', Perfil.Rol.ALUMNO)
            self.matriculas[correo] = Matricula.objects.create(alumno=alumno, seccion=seccion)
        for correo, puntaje in [('ana', 16), ('bea', 14), ('luis', 8), ('mia', 10)]:
            Resultado.objects.create(
                simulacro=self.sim1, matricula=self.matriculas[correo], puntaje=puntaje
            )
        self.autenticar('coord@test.pe')

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def test_calcula_el_promedio_por_seccion(self):
        respuesta = self.client.get(f'/api/rendimiento/?simulacro={self.sim1.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        promedios = {s['nombre']: s['promedio'] for s in respuesta.data['secciones']}
        self.assertEqual(promedios['Matemática - A'], 15.0)
        self.assertEqual(promedios['Matemática - B'], 9.0)

    def test_marca_las_secciones_que_necesitan_refuerzo(self):
        respuesta = self.client.get(f'/api/rendimiento/?simulacro={self.sim1.id}')
        secciones = respuesta.data['secciones']
        self.assertEqual(secciones[0]['nombre'], 'Matemática - B')
        self.assertTrue(secciones[0]['necesita_refuerzo'])
        self.assertFalse(secciones[1]['necesita_refuerzo'])

    def test_sin_parametro_usa_el_simulacro_mas_reciente(self):
        sim2 = Simulacro.objects.create(
            curso=self.curso, nombre='Simulacro 2', fecha=date(2026, 10, 10)
        )
        Resultado.objects.create(simulacro=sim2, matricula=self.matriculas['ana'], puntaje=20)
        respuesta = self.client.get('/api/rendimiento/')
        self.assertEqual(respuesta.data['simulacro']['nombre'], 'Simulacro 2')
        self.assertEqual(len(respuesta.data['secciones']), 1)

    def test_simulacro_inexistente_da_404(self):
        respuesta = self.client.get('/api/rendimiento/?simulacro=9999')
        self.assertEqual(respuesta.status_code, status.HTTP_404_NOT_FOUND)

    def test_docente_no_puede_ver_el_resumen(self):
        self.autenticar('docente@test.pe')
        respuesta = self.client.get('/api/rendimiento/')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_registra_un_resultado_con_el_alumno(self):
        zoe_matricula = Matricula.objects.create(
            alumno=self.zoe, seccion=Seccion.objects.get(nombre='A')
        )
        respuesta = self.client.post(
            '/api/resultados/',
            {'simulacro': self.sim1.id, 'alumno': self.zoe.id, 'puntaje': '12.50'},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertEqual(respuesta.data['alumno'], self.zoe.id)
        self.assertTrue(Resultado.objects.filter(matricula=zoe_matricula).exists())

    def test_alumno_sin_matricula_en_el_curso_se_rechaza(self):
        respuesta = self.client.post(
            '/api/resultados/',
            {'simulacro': self.sim1.id, 'alumno': self.zoe.id, 'puntaje': 12},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('alumno', respuesta.data)

    def test_resultado_repetido_se_rechaza(self):
        ana = self.matriculas['ana'].alumno
        respuesta = self.client.post(
            '/api/resultados/',
            {'simulacro': self.sim1.id, 'alumno': ana.id, 'puntaje': 18},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_puntaje_fuera_de_rango_se_rechaza(self):
        Matricula.objects.create(alumno=self.zoe, seccion=Seccion.objects.get(nombre='A'))
        respuesta = self.client.post(
            '/api/resultados/',
            {'simulacro': self.sim1.id, 'alumno': self.zoe.id, 'puntaje': 25},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('puntaje', respuesta.data)
