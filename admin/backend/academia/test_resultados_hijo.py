from datetime import date

from rest_framework.test import APITestCase

from .models import (
    Academia, Apoderado, Ciclo, Curso, Matricula, Perfil, Resultado, Seccion, Simulacro,
)
from .resultados_hijo_calculo import resultados_por_curso
from .test_usuarios import crear_usuario


class ResultadosHijoTests(APITestCase):
    def setUp(self):
        academia = Academia.objects.create(nombre='Academia Test')
        ciclo = Ciclo.objects.create(
            academia=academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        self.mate = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.letras = Curso.objects.create(ciclo=ciclo, nombre='Comunicación')

        self.hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)
        self.ajeno = crear_usuario('ajeno@test.pe', Perfil.Rol.ALUMNO)
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        otro = crear_usuario('otro@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=self.padre.perfil).hijos.add(self.hijo.perfil)
        Apoderado.objects.create(perfil=otro.perfil).hijos.add(self.ajeno.perfil)

        self.mat_mate = self.matricular(self.hijo, self.mate)
        self.mat_letras = self.matricular(self.hijo, self.letras)
        self.client.force_authenticate(self.padre)

    def matricular(self, alumno, curso):
        seccion, _ = Seccion.objects.get_or_create(curso=curso, nombre='A', defaults={'cupo': 10})
        return Matricula.objects.create(alumno=alumno.perfil, seccion=seccion)

    def simulacro(self, curso, nombre, dia, activa=True):
        return Simulacro.objects.create(curso=curso, nombre=nombre, fecha=dia, activa=activa)

    def nota(self, simulacro, matricula, puntaje):
        return Resultado.objects.create(simulacro=simulacro, matricula=matricula, puntaje=puntaje)

    def pedir(self, alumno):
        return self.client.get(f'/api/mis-hijos/{alumno.perfil.id}/simulacros/')

    # ---------- Cálculo ----------

    def test_agrupa_por_curso_y_ordena_por_fecha(self):
        s2 = self.simulacro(self.mate, 'Simulacro 2', date(2026, 10, 17))
        s1 = self.simulacro(self.mate, 'Simulacro 1', date(2026, 9, 26))
        self.nota(s2, self.mat_mate, 15)
        self.nota(s1, self.mat_mate, 12)
        cursos = resultados_por_curso(self.hijo.perfil)
        self.assertEqual(len(cursos), 1)
        nombres = [r['simulacro'] for r in cursos[0]['resultados']]
        self.assertEqual(nombres, ['Simulacro 1', 'Simulacro 2'])

    def test_calcula_la_variacion_contra_el_simulacro_anterior(self):
        s1 = self.simulacro(self.mate, 'Simulacro 1', date(2026, 9, 26))
        s2 = self.simulacro(self.mate, 'Simulacro 2', date(2026, 10, 17))
        s3 = self.simulacro(self.mate, 'Simulacro 3', date(2026, 11, 7))
        self.nota(s1, self.mat_mate, 12)
        self.nota(s2, self.mat_mate, 15.5)
        self.nota(s3, self.mat_mate, 14)
        variaciones = [r['variacion'] for r in resultados_por_curso(self.hijo.perfil)[0]['resultados']]
        self.assertEqual(variaciones, [None, 3.5, -1.5])

    def test_calcula_el_promedio_del_curso(self):
        s1 = self.simulacro(self.mate, 'Simulacro 1', date(2026, 9, 26))
        s2 = self.simulacro(self.mate, 'Simulacro 2', date(2026, 10, 17))
        self.nota(s1, self.mat_mate, 12)
        self.nota(s2, self.mat_mate, 15)
        self.assertEqual(resultados_por_curso(self.hijo.perfil)[0]['promedio'], 13.5)

    def test_cada_curso_lleva_su_propia_variacion(self):
        self.nota(self.simulacro(self.mate, 'Mate 1', date(2026, 9, 26)), self.mat_mate, 10)
        self.nota(self.simulacro(self.letras, 'Letras 1', date(2026, 9, 27)), self.mat_letras, 18)
        cursos = resultados_por_curso(self.hijo.perfil)
        self.assertEqual([c['curso'] for c in cursos], ['Comunicación', 'Matemática'])
        self.assertTrue(all(c['resultados'][0]['variacion'] is None for c in cursos))

    def test_ignora_simulacros_desactivados(self):
        s1 = self.simulacro(self.mate, 'Simulacro 1', date(2026, 9, 26), activa=False)
        self.nota(s1, self.mat_mate, 12)
        self.assertEqual(resultados_por_curso(self.hijo.perfil), [])

    # ---------- Endpoint y acceso ----------

    def test_devuelve_los_resultados_del_hijo(self):
        self.nota(self.simulacro(self.mate, 'Simulacro 1', date(2026, 9, 26)), self.mat_mate, 12)
        r = self.pedir(self.hijo)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data['hijo']['email'], 'hijo@test.pe')
        self.assertEqual(r.data['cursos'][0]['resultados'][0]['puntaje'], 12.0)

    def test_sin_resultados_devuelve_lista_vacia(self):
        r = self.pedir(self.hijo)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data['cursos'], [])

    def test_no_muestra_resultados_de_otros_alumnos(self):
        mat_ajeno = self.matricular(self.ajeno, self.mate)
        s1 = self.simulacro(self.mate, 'Simulacro 1', date(2026, 9, 26))
        self.nota(s1, mat_ajeno, 19)
        self.assertEqual(self.pedir(self.hijo).data['cursos'], [])

    def test_no_puede_consultar_a_hijos_de_otro_apoderado(self):
        self.assertEqual(self.pedir(self.ajeno).status_code, 404)

    def test_solo_entran_apoderados(self):
        docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.client.force_authenticate(docente)
        self.assertEqual(self.pedir(self.hijo).status_code, 403)

    def test_sin_sesion_no_entra(self):
        self.client.force_authenticate(None)
        self.assertIn(self.pedir(self.hijo).status_code, (401, 403))
