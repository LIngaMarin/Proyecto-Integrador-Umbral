from datetime import date
from unittest.mock import patch

from rest_framework.test import APITestCase

from .models import Academia, Apoderado, Ciclo, Curso, Matricula, Perfil, Seccion, Simulacro
from .programados_hijo_calculo import simulacros_programados
from .test_usuarios import crear_usuario

HOY = date(2026, 11, 10)


class ProgramadosBase(APITestCase):
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

        self.matricular(self.hijo, self.mate, 'B')
        self.client.force_authenticate(self.padre)

    def matricular(self, alumno, curso, nombre='A', activa=True):
        seccion, _ = Seccion.objects.get_or_create(
            curso=curso, nombre=nombre, defaults={'cupo': 10}
        )
        return Matricula.objects.create(alumno=alumno.perfil, seccion=seccion, activa=activa)

    def simulacro(self, curso, nombre, dia, activa=True):
        return Simulacro.objects.create(curso=curso, nombre=nombre, fecha=dia, activa=activa)


class CalculoProgramadosTests(ProgramadosBase):
    def lista(self, desde=HOY, hasta=date(2026, 12, 31)):
        return simulacros_programados(self.hijo.perfil, desde, hasta)

    def test_ordena_por_fecha(self):
        self.simulacro(self.mate, 'Simulacro 3', date(2026, 12, 5))
        self.simulacro(self.mate, 'Simulacro 2', date(2026, 11, 20))
        self.assertEqual([s['nombre'] for s in self.lista()], ['Simulacro 2', 'Simulacro 3'])

    def test_incluye_curso_y_seccion_del_alumno(self):
        self.simulacro(self.mate, 'Simulacro 2', date(2026, 11, 20))
        fila = self.lista()[0]
        self.assertEqual(fila['curso'], 'Matemática')
        self.assertEqual(fila['seccion'], 'B')

    def test_los_extremos_del_rango_estan_incluidos(self):
        self.simulacro(self.mate, 'Primero', date(2026, 11, 10))
        self.simulacro(self.mate, 'Ultimo', date(2026, 11, 30))
        self.simulacro(self.mate, 'Fuera', date(2026, 12, 1))
        nombres = [s['nombre'] for s in self.lista(HOY, date(2026, 11, 30))]
        self.assertEqual(nombres, ['Primero', 'Ultimo'])

    def test_no_incluye_simulacros_de_cursos_sin_matricula(self):
        self.simulacro(self.letras, 'Letras 1', date(2026, 11, 20))
        self.assertEqual(self.lista(), [])

    def test_ignora_la_matricula_desactivada(self):
        self.matricular(self.hijo, self.letras, activa=False)
        self.simulacro(self.letras, 'Letras 1', date(2026, 11, 20))
        self.assertEqual(self.lista(), [])

    def test_ignora_simulacros_desactivados(self):
        self.simulacro(self.mate, 'Cancelado', date(2026, 11, 20), activa=False)
        self.assertEqual(self.lista(), [])

    def test_un_simulacro_no_se_repite_si_hay_dos_matriculas_en_el_curso(self):
        self.matricular(self.hijo, self.mate, 'C')
        self.simulacro(self.mate, 'Simulacro 2', date(2026, 11, 20))
        self.assertEqual(len(self.lista()), 1)


@patch('academia.programados_hijo.hoy', return_value=HOY)
class EndpointProgramadosTests(ProgramadosBase):
    def pedir(self, alumno, consulta=''):
        return self.client.get(
            f'/api/mis-hijos/{alumno.perfil.id}/simulacros-programados/{consulta}'
        )

    def test_por_defecto_devuelve_de_hoy_en_adelante(self, _hoy):
        self.simulacro(self.mate, 'Pasado', date(2026, 11, 1))
        self.simulacro(self.mate, 'Proximo', date(2026, 11, 20))
        r = self.pedir(self.hijo)
        self.assertEqual(r.status_code, 200)
        self.assertEqual([s['nombre'] for s in r.data['simulacros']], ['Proximo'])
        self.assertEqual(r.data['desde'], HOY)
        self.assertEqual(r.data['hijo']['email'], 'hijo@test.pe')

    def test_acepta_un_rango_de_fechas(self, _hoy):
        self.simulacro(self.mate, 'Pasado', date(2026, 11, 1))
        r = self.pedir(self.hijo, '?desde=2026-11-01&hasta=2026-11-30')
        self.assertEqual([s['nombre'] for s in r.data['simulacros']], ['Pasado'])

    def test_sin_simulacros_devuelve_lista_vacia(self, _hoy):
        self.assertEqual(self.pedir(self.hijo).data['simulacros'], [])

    def test_rechaza_fechas_con_formato_incorrecto(self, _hoy):
        self.assertEqual(self.pedir(self.hijo, '?desde=10-11-2026').status_code, 400)
        self.assertEqual(self.pedir(self.hijo, '?hasta=mañana').status_code, 400)

    def test_rechaza_rango_invertido(self, _hoy):
        r = self.pedir(self.hijo, '?desde=2026-12-01&hasta=2026-11-01')
        self.assertEqual(r.status_code, 400)

    def test_rechaza_rangos_de_mas_de_un_anio(self, _hoy):
        r = self.pedir(self.hijo, '?desde=2026-01-01&hasta=2027-12-31')
        self.assertEqual(r.status_code, 400)

    def test_no_puede_consultar_a_hijos_de_otro_apoderado(self, _hoy):
        self.assertEqual(self.pedir(self.ajeno).status_code, 404)

    def test_solo_entran_apoderados(self, _hoy):
        docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.client.force_authenticate(docente)
        self.assertEqual(self.pedir(self.hijo).status_code, 403)

    def test_sin_sesion_no_entra(self, _hoy):
        self.client.force_authenticate(None)
        self.assertIn(self.pedir(self.hijo).status_code, (401, 403))
