from datetime import date

from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .comparacion import comparar_simulacros
from .models import Academia, Ciclo, Curso, Matricula, Perfil, Resultado, Seccion, Simulacro


def crear_perfil(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    return Perfil.objects.create(user=user, rol=rol)


class ComparacionTests(APITestCase):
    def setUp(self):
        crear_perfil('coord@test.pe', Perfil.Rol.COORDINADOR)
        crear_perfil('docente@test.pe', Perfil.Rol.DOCENTE)

        academia = Academia.objects.create(nombre='Academia Test')
        ciclo = Ciclo.objects.create(
            academia=academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        self.curso = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.otro_curso = Curso.objects.create(ciclo=ciclo, nombre='Física')
        self.sec_a = Seccion.objects.create(curso=self.curso, nombre='A', cupo=10)
        self.sec_b = Seccion.objects.create(curso=self.curso, nombre='B', cupo=10)
        self.sim1 = Simulacro.objects.create(
            curso=self.curso, nombre='Simulacro 1', fecha=date(2026, 9, 15)
        )
        self.sim2 = Simulacro.objects.create(
            curso=self.curso, nombre='Simulacro 2', fecha=date(2026, 10, 15)
        )

        matriculas = {}
        for nombre, seccion in [('ana', self.sec_a), ('bea', self.sec_a),
                                ('luis', self.sec_b), ('mia', self.sec_b)]:
            alumno = crear_perfil(f'{nombre}@test.pe', Perfil.Rol.ALUMNO)
            matriculas[nombre] = Matricula.objects.create(alumno=alumno, seccion=seccion)
        self.matriculas = matriculas
        # Sección A baja de 15 a 14; la B sube de 9 a 13 y sale de refuerzo.
        for nombre, p1, p2 in [('ana', 16, 14), ('bea', 14, 14), ('luis', 8, 12), ('mia', 10, 14)]:
            Resultado.objects.create(simulacro=self.sim1, matricula=matriculas[nombre], puntaje=p1)
            Resultado.objects.create(simulacro=self.sim2, matricula=matriculas[nombre], puntaje=p2)
        self.autenticar('coord@test.pe')

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/', {'username': email, 'password': 'clave12345'}, format='json'
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def por_nombre(self, secciones):
        return {s['nombre']: s for s in secciones}

    # --- consulta ---

    def test_calcula_la_diferencia_y_la_tendencia_por_seccion(self):
        resultado = comparar_simulacros(self.sim1, self.sim2)
        secciones = self.por_nombre(resultado['secciones'])
        a, b = secciones['Matemática - A'], secciones['Matemática - B']
        self.assertEqual((a['promedio_anterior'], a['promedio_actual']), (15.0, 14.0))
        self.assertEqual(a['diferencia'], -1.0)
        self.assertEqual(a['tendencia'], 'baja')
        self.assertEqual(b['diferencia'], 4.0)
        self.assertEqual(b['tendencia'], 'mejora')

    def test_detecta_si_el_refuerzo_funciono(self):
        secciones = self.por_nombre(comparar_simulacros(self.sim1, self.sim2)['secciones'])
        self.assertTrue(secciones['Matemática - B']['refuerzo_resuelto'])
        self.assertFalse(secciones['Matemática - B']['necesita_refuerzo'])
        self.assertFalse(secciones['Matemática - A']['refuerzo_resuelto'])

    def test_resume_cuantas_secciones_mejoraron_o_bajaron(self):
        resumen = comparar_simulacros(self.sim1, self.sim2)['resumen']
        self.assertEqual(resumen['mejoraron'], 1)
        self.assertEqual(resumen['bajaron'], 1)
        self.assertEqual(resumen['igual'], 0)

    def test_seccion_sin_resultados_en_un_simulacro_queda_sin_datos(self):
        sim3 = Simulacro.objects.create(
            curso=self.curso, nombre='Simulacro 3', fecha=date(2026, 11, 15)
        )
        for nombre in ('ana', 'bea'):
            Resultado.objects.create(simulacro=sim3, matricula=self.matriculas[nombre], puntaje=15)
        secciones = self.por_nombre(comparar_simulacros(self.sim2, sim3)['secciones'])
        self.assertEqual(secciones['Matemática - B']['tendencia'], 'sin_datos')
        self.assertIsNone(secciones['Matemática - B']['diferencia'])
        self.assertEqual(secciones['Matemática - A']['tendencia'], 'mejora')

    # --- endpoint ---

    def test_compara_los_dos_simulacros_mas_recientes_de_un_curso(self):
        respuesta = self.client.get(f'/api/rendimiento/comparar/?curso={self.curso.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['anterior']['id'], self.sim1.id)
        self.assertEqual(respuesta.data['actual']['id'], self.sim2.id)
        self.assertEqual(respuesta.data['curso']['nombre'], 'Matemática')
        self.assertEqual(len(respuesta.data['secciones']), 2)

    def test_compara_dos_simulacros_concretos(self):
        respuesta = self.client.get(
            f'/api/rendimiento/comparar/?anterior={self.sim1.id}&actual={self.sim2.id}'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['resumen']['mejoraron'], 1)

    def test_exige_curso_o_simulacros(self):
        respuesta = self.client.get('/api/rendimiento/comparar/')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_exige_ambos_simulacros(self):
        respuesta = self.client.get(f'/api/rendimiento/comparar/?actual={self.sim2.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_rechaza_simulacros_de_distinto_curso(self):
        fisica = Simulacro.objects.create(
            curso=self.otro_curso, nombre='Simulacro F', fecha=date(2026, 10, 20)
        )
        respuesta = self.client.get(
            f'/api/rendimiento/comparar/?anterior={self.sim1.id}&actual={fisica.id}'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_rechaza_el_orden_invertido(self):
        respuesta = self.client.get(
            f'/api/rendimiento/comparar/?anterior={self.sim2.id}&actual={self.sim1.id}'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_rechaza_el_mismo_simulacro_dos_veces(self):
        respuesta = self.client.get(
            f'/api/rendimiento/comparar/?anterior={self.sim1.id}&actual={self.sim1.id}'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_curso_con_un_solo_simulacro_no_se_puede_comparar(self):
        Simulacro.objects.create(curso=self.otro_curso, nombre='Unico', fecha=date(2026, 10, 1))
        respuesta = self.client.get(f'/api/rendimiento/comparar/?curso={self.otro_curso.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_simulacro_inexistente_da_404(self):
        respuesta = self.client.get(
            f'/api/rendimiento/comparar/?anterior={self.sim1.id}&actual=9999'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_404_NOT_FOUND)

    def test_ignora_simulacros_desactivados(self):
        Simulacro.objects.filter(pk=self.sim2.pk).update(activa=False)
        respuesta = self.client.get(f'/api/rendimiento/comparar/?curso={self.curso.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_un_docente_no_puede_comparar(self):
        self.autenticar('docente@test.pe')
        respuesta = self.client.get(f'/api/rendimiento/comparar/?curso={self.curso.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN)

    def test_exige_sesion(self):
        self.client.credentials()
        respuesta = self.client.get(f'/api/rendimiento/comparar/?curso={self.curso.id}')
        self.assertEqual(respuesta.status_code, status.HTTP_401_UNAUTHORIZED)
