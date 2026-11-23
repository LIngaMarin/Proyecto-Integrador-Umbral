from datetime import date
from unittest.mock import patch

from rest_framework.test import APITestCase

from .models import (
    Academia, Apoderado, Asistencia, Ciclo, Curso, Matricula, Perfil, Resultado, Seccion,
    Simulacro,
)
from .test_usuarios import crear_usuario

HOY = date(2026, 11, 5)


@patch('academia.asistencia_hijo.hoy', return_value=HOY)
class ReporteHijoTests(APITestCase):
    def setUp(self):
        academia = Academia.objects.create(nombre='Academia Test')
        ciclo = Ciclo.objects.create(
            academia=academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        self.mate = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)
        self.ajeno = crear_usuario('ajeno@test.pe', Perfil.Rol.ALUMNO)
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        otro = crear_usuario('otro@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=self.padre.perfil).hijos.add(self.hijo.perfil)
        Apoderado.objects.create(perfil=otro.perfil).hijos.add(self.ajeno.perfil)
        seccion = Seccion.objects.create(curso=self.mate, nombre='A', cupo=10)
        self.mat = Matricula.objects.create(alumno=self.hijo.perfil, seccion=seccion)
        self.client.force_authenticate(self.padre)

    def falta(self, dia):
        Asistencia.objects.create(alumno=self.hijo.perfil, fecha=dia, estado='FALTA')

    def simulacro(self, nombre, dia, puntaje):
        sim = Simulacro.objects.create(curso=self.mate, nombre=nombre, fecha=dia)
        Resultado.objects.create(simulacro=sim, matricula=self.mat, puntaje=puntaje)

    def pedir(self, alumno=None, consulta=''):
        alumno = alumno or self.hijo
        return self.client.get(f'/api/mis-hijos/{alumno.perfil.id}/reporte/{consulta}')

    # ---------- Contenido ----------

    def test_devuelve_asistencia_simulacros_y_promedio_del_periodo(self, _):
        Asistencia.objects.create(alumno=self.hijo.perfil, fecha=date(2026, 11, 2),
                                  estado='PRESENTE')
        self.falta(date(2026, 11, 3))
        self.simulacro('Simulacro 1', date(2026, 10, 17), 12)
        self.simulacro('Simulacro 2', date(2026, 11, 1), 15)
        datos = self.pedir().json()
        self.assertEqual(datos['alumno']['email'], 'hijo@test.pe')
        self.assertEqual(datos['periodo'], {'desde': '2026-10-07', 'hasta': '2026-11-05'})
        self.assertEqual(datos['asistencia']['presentes'], 1)
        self.assertEqual(datos['asistencia']['faltas'], 1)
        self.assertEqual(datos['asistencia']['porcentaje'], 50)
        self.assertEqual(
            datos['simulacros'],
            [
                {'nombre': 'Simulacro 1', 'fecha': '2026-10-17', 'puntaje': 12.0},
                {'nombre': 'Simulacro 2', 'fecha': '2026-11-01', 'puntaje': 15.0},
            ],
        )
        self.assertEqual(datos['promedio'], 13.5)

    def test_solo_incluye_simulacros_dentro_del_periodo(self, _):
        self.simulacro('Antiguo', date(2026, 10, 1), 5)
        self.simulacro('Reciente', date(2026, 10, 20), 14)
        datos = self.pedir().json()
        self.assertEqual([s['nombre'] for s in datos['simulacros']], ['Reciente'])
        self.assertEqual(datos['promedio'], 14.0)

    def test_respeta_el_rango_pedido(self, _):
        self.simulacro('Dentro', date(2026, 11, 2), 16)
        self.simulacro('Fuera', date(2026, 11, 4), 8)
        datos = self.pedir(consulta='?desde=2026-11-01&hasta=2026-11-02').json()
        self.assertEqual(datos['periodo'], {'desde': '2026-11-01', 'hasta': '2026-11-02'})
        self.assertEqual([s['nombre'] for s in datos['simulacros']], ['Dentro'])

    def test_reporte_sin_datos(self, _):
        datos = self.pedir().json()
        self.assertEqual(datos['asistencia'], {
            'presentes': 0, 'tardanzas': 0, 'faltas': 0, 'porcentaje': None,
        })
        self.assertEqual(datos['simulacros'], [])
        self.assertIsNone(datos['promedio'])
        self.assertEqual(datos['tendencia'], 'sin_datos')

    def test_un_solo_simulacro_no_tiene_tendencia(self, _):
        self.simulacro('Simulacro 1', date(2026, 11, 1), 11)
        datos = self.pedir().json()
        self.assertEqual(datos['tendencia'], 'sin_datos')
        self.assertEqual(datos['promedio'], 11.0)

    # ---------- Tendencia ----------

    def test_tendencia_sube_si_el_ultimo_supera_al_anterior(self, _):
        self.simulacro('Simulacro 1', date(2026, 10, 17), 10)
        self.simulacro('Simulacro 2', date(2026, 11, 1), 14)
        self.assertEqual(self.pedir().json()['tendencia'], 'sube')

    def test_tendencia_baja_si_el_ultimo_es_menor(self, _):
        self.simulacro('Simulacro 1', date(2026, 10, 17), 14)
        self.simulacro('Simulacro 2', date(2026, 11, 1), 9)
        self.assertEqual(self.pedir().json()['tendencia'], 'baja')

    def test_tendencia_estable_si_son_iguales(self, _):
        self.simulacro('Simulacro 1', date(2026, 10, 17), 12)
        self.simulacro('Simulacro 2', date(2026, 11, 1), 12)
        self.assertEqual(self.pedir().json()['tendencia'], 'estable')

    # ---------- Acceso ----------

    def test_reporte_de_hijo_ajeno_devuelve_404(self, _):
        self.assertEqual(self.pedir(self.ajeno).status_code, 404)

    def test_fechas_invalidas_devuelven_400(self, _):
        respuesta = self.pedir(consulta='?desde=ayer')
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn('desde', respuesta.json())

    def test_un_docente_no_puede_ver_reportes(self, _):
        docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.client.force_authenticate(docente)
        self.assertEqual(self.pedir().status_code, 403)
