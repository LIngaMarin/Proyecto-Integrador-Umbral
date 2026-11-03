import json
from datetime import date
from unittest.mock import patch

from django.contrib.auth.models import User
from django.core.exceptions import ValidationError
from django.test import override_settings
from rest_framework import status
from rest_framework.test import APITestCase

from .models import (
    Academia, Ciclo, Curso, ErrorTema, Matricula, Perfil, Resultado, Seccion, Simulacro, Tema,
)
from .temas_criticos import analizar_con_ia, construir_prompt, temas_por_seccion


def crear_perfil(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    return Perfil.objects.create(user=user, rol=rol)


class TemasCriticosTests(APITestCase):
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
        self.sim = Simulacro.objects.create(
            curso=self.curso, nombre='Simulacro 1', fecha=date(2026, 10, 15)
        )
        self.algebra = Tema.objects.create(curso=self.curso, nombre='Álgebra')
        self.geometria = Tema.objects.create(curso=self.curso, nombre='Geometría')

        self.resultados = {}
        for nombre, seccion, puntaje in [
            ('ana', self.sec_a, 12), ('bea', self.sec_a, 14),
            ('luis', self.sec_b, 8), ('mia', self.sec_b, 9),
        ]:
            alumno = crear_perfil(f'{nombre}@test.pe', Perfil.Rol.ALUMNO)
            matricula = Matricula.objects.create(alumno=alumno, seccion=seccion)
            self.resultados[nombre] = Resultado.objects.create(
                simulacro=self.sim, matricula=matricula, puntaje=puntaje
            )
        # Salón A: Álgebra 2 + 1 = 3 errores en 2 alumnos. Salón B: más errores, en ambos temas.
        for nombre, tema, cantidad in [
            ('ana', self.algebra, 2), ('bea', self.algebra, 1), ('bea', self.geometria, 0),
            ('luis', self.algebra, 4), ('luis', self.geometria, 3),
            ('mia', self.algebra, 3), ('mia', self.geometria, 5),
        ]:
            ErrorTema.objects.create(
                resultado=self.resultados[nombre], tema=tema, cantidad=cantidad
            )
        self.autenticar('coord@test.pe')

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/', {'username': email, 'password': 'clave12345'}, format='json'
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def por_nombre(self, secciones):
        return {s['nombre']: s for s in secciones}

    # --- consulta ---

    def test_agrupa_los_errores_por_tema_y_salon(self):
        secciones = self.por_nombre(temas_por_seccion(self.sim))
        a = secciones['Matemática - A']['temas']
        self.assertEqual(len(a), 1)
        self.assertEqual(
            (a[0]['tema'], a[0]['errores'], a[0]['alumnos_con_error']), ('Álgebra', 3, 2)
        )
        b = secciones['Matemática - B']['temas']
        self.assertEqual([t['tema'] for t in b], ['Geometría', 'Álgebra'])
        self.assertEqual([t['errores'] for t in b], [8, 7])

    def test_calcula_el_porcentaje_de_alumnos_afectados(self):
        b = self.por_nombre(temas_por_seccion(self.sim))['Matemática - B']['temas']
        self.assertEqual(b[0]['porcentaje_alumnos'], 100)

    def test_no_cuenta_los_temas_sin_errores(self):
        a = self.por_nombre(temas_por_seccion(self.sim))['Matemática - A']['temas']
        self.assertNotIn('Geometría', [t['tema'] for t in a])

    def test_ordena_los_salones_por_total_de_errores(self):
        nombres = [s['nombre'] for s in temas_por_seccion(self.sim)]
        self.assertEqual(nombres, ['Matemática - B', 'Matemática - A'])

    def test_puede_filtrar_un_salon(self):
        secciones = temas_por_seccion(self.sim, self.sec_a.id)
        self.assertEqual([s['nombre'] for s in secciones], ['Matemática - A'])

    def test_el_tema_debe_ser_del_curso_del_simulacro(self):
        ajeno = Tema.objects.create(curso=self.otro_curso, nombre='Cinemática')
        error = ErrorTema(resultado=self.resultados['ana'], tema=ajeno, cantidad=1)
        with self.assertRaises(ValidationError):
            error.clean()

    # --- endpoint ---

    def test_endpoint_devuelve_el_simulacro_mas_reciente(self):
        respuesta = self.client.get('/api/rendimiento/temas/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(respuesta.data['simulacro']['id'], self.sim.id)
        self.assertEqual(len(respuesta.data['secciones']), 2)

    def test_sin_ia_devuelve_un_resumen_basico_y_no_llama_a_gemini(self):
        with patch('academia.temas_criticos.llamar_gemini') as gemini:
            respuesta = self.client.get(f'/api/rendimiento/temas/?simulacro={self.sim.id}')
        gemini.assert_not_called()
        self.assertFalse(respuesta.data['ia']['disponible'])
        self.assertIn('Geometría', respuesta.data['secciones'][0]['resumen'])

    def test_filtra_por_salon(self):
        respuesta = self.client.get(f'/api/rendimiento/temas/?seccion={self.sec_a.id}')
        self.assertEqual(len(respuesta.data['secciones']), 1)

    def test_identificadores_invalidos_dan_400(self):
        self.assertEqual(self.client.get('/api/rendimiento/temas/?simulacro=abc').status_code, 400)
        self.assertEqual(self.client.get('/api/rendimiento/temas/?seccion=abc').status_code, 400)

    def test_simulacro_inexistente_da_404(self):
        self.assertEqual(self.client.get('/api/rendimiento/temas/?simulacro=999').status_code, 404)

    def test_sin_simulacros_devuelve_una_lista_vacia(self):
        Simulacro.objects.all().delete()
        respuesta = self.client.get('/api/rendimiento/temas/')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIsNone(respuesta.data['simulacro'])
        self.assertEqual(respuesta.data['secciones'], [])

    def test_un_docente_no_puede_consultarlo(self):
        self.autenticar('docente@test.pe')
        self.assertEqual(self.client.get('/api/rendimiento/temas/').status_code, 403)

    def test_exige_sesion(self):
        self.client.credentials()
        self.assertEqual(self.client.get('/api/rendimiento/temas/').status_code, 401)

    # --- análisis con IA ---

    def respuesta_de_gemini(self):
        return json.dumps(
            {
                'secciones': [
                    {'seccion': self.sec_b.id, 'resumen': 'Refuerza Geometría con práctica guiada.'},
                    {'seccion': self.sec_a.id, 'resumen': 'Repasa Álgebra básica.'},
                ]
            }
        )

    @override_settings(GEMINI_API_KEY='clave-de-prueba')
    def test_con_clave_usa_el_resumen_de_gemini(self):
        with patch('academia.temas_criticos.llamar_gemini', return_value=self.respuesta_de_gemini()):
            respuesta = self.client.get(f'/api/rendimiento/temas/?simulacro={self.sim.id}&ia=1')
        self.assertTrue(respuesta.data['ia']['disponible'])
        resumenes = {s['nombre']: s['resumen'] for s in respuesta.data['secciones']}
        self.assertEqual(resumenes['Matemática - B'], 'Refuerza Geometría con práctica guiada.')

    @override_settings(GEMINI_API_KEY='clave-de-prueba')
    def test_el_prompt_incluye_los_temas_y_los_errores(self):
        prompt = construir_prompt(temas_por_seccion(self.sim))
        self.assertIn('Geometría: 8 errores', prompt)
        self.assertIn('Matemática - A', prompt)

    @override_settings(GEMINI_API_KEY='')
    def test_sin_clave_avisa_y_usa_el_resumen_basico(self):
        respuesta = self.client.get(f'/api/rendimiento/temas/?simulacro={self.sim.id}&ia=1')
        self.assertFalse(respuesta.data['ia']['disponible'])
        self.assertIn('clave', respuesta.data['ia']['motivo'])
        self.assertTrue(respuesta.data['secciones'][0]['resumen'])

    @override_settings(GEMINI_API_KEY='clave-de-prueba')
    def test_si_gemini_falla_no_rompe_la_pantalla(self):
        with patch('academia.temas_criticos.llamar_gemini', side_effect=TimeoutError('lento')):
            respuesta = self.client.get(f'/api/rendimiento/temas/?simulacro={self.sim.id}&ia=1')
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertFalse(respuesta.data['ia']['disponible'])
        self.assertIn('Geometría', respuesta.data['secciones'][0]['resumen'])

    @override_settings(GEMINI_API_KEY='clave-de-prueba')
    def test_si_gemini_responde_algo_invalido_usa_el_resumen_basico(self):
        with patch('academia.temas_criticos.llamar_gemini', return_value='esto no es json'):
            secciones, ia = analizar_con_ia(temas_por_seccion(self.sim))
        self.assertFalse(ia['disponible'])
        self.assertTrue(all(s['resumen'] for s in secciones))

    @override_settings(GEMINI_API_KEY='clave-de-prueba')
    def test_completa_con_resumen_basico_los_salones_que_gemini_omitio(self):
        parcial = json.dumps({'secciones': [{'seccion': self.sec_b.id, 'resumen': 'Solo el salón B.'}]})
        with patch('academia.temas_criticos.llamar_gemini', return_value=parcial):
            secciones, ia = analizar_con_ia(temas_por_seccion(self.sim))
        resumenes = {s['nombre']: s['resumen'] for s in secciones}
        self.assertTrue(ia['disponible'])
        self.assertEqual(resumenes['Matemática - B'], 'Solo el salón B.')
        self.assertIn('Álgebra', resumenes['Matemática - A'])

    @override_settings(GEMINI_API_KEY='clave-de-prueba')
    def test_la_clave_no_se_filtra_en_la_respuesta(self):
        with patch('academia.temas_criticos.llamar_gemini', side_effect=ValueError('x')):
            respuesta = self.client.get(f'/api/rendimiento/temas/?simulacro={self.sim.id}&ia=1')
        self.assertNotIn('clave-de-prueba', json.dumps(respuesta.data))
