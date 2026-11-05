from datetime import date, time
from unittest.mock import patch

from django.core.exceptions import ValidationError
from django.db import IntegrityError, transaction
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Apoderado, Asistencia, Perfil
from .test_usuarios import crear_usuario

HOY = date(2026, 11, 5)


def marcar(alumno, dia, estado='PRESENTE', hora=time(7, 55)):
    if estado == 'FALTA':
        hora = None
    return Asistencia.objects.create(
        alumno=alumno.perfil, fecha=dia, estado=estado, hora_registro=hora
    )


class AsistenciaModeloTests(APITestCase):
    def setUp(self):
        self.alumno = crear_usuario('alumno@test.pe', Perfil.Rol.ALUMNO)

    def test_un_alumno_tiene_una_sola_asistencia_por_dia(self):
        marcar(self.alumno, HOY)
        with self.assertRaises(IntegrityError), transaction.atomic():
            marcar(self.alumno, HOY, 'TARDANZA')

    def test_solo_se_registra_asistencia_a_alumnos(self):
        docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        with self.assertRaises(ValidationError):
            Asistencia(alumno=docente.perfil, fecha=HOY).full_clean()

    def test_por_defecto_el_estado_es_presente(self):
        registro = Asistencia.objects.create(alumno=self.alumno.perfil, fecha=HOY)
        self.assertEqual(registro.estado, 'PRESENTE')


@patch('academia.asistencia_hijo.hoy', return_value=HOY)
class AsistenciaHijoTests(APITestCase):
    def setUp(self):
        self.hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)
        self.hija = crear_usuario('hija@test.pe', Perfil.Rol.ALUMNO)
        self.ajeno = crear_usuario('ajeno@test.pe', Perfil.Rol.ALUMNO)
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        otro = crear_usuario('otro@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=self.padre.perfil).hijos.add(
            self.hijo.perfil, self.hija.perfil
        )
        Apoderado.objects.create(perfil=otro.perfil).hijos.add(self.ajeno.perfil)
        self.client.force_authenticate(self.padre)

    def pedir(self, alumno, consulta=''):
        return self.client.get(f'/api/mis-hijos/{alumno.perfil.id}/asistencia/{consulta}')

    # ---------- Datos que devuelve ----------

    def test_devuelve_la_asistencia_del_hijo_de_la_mas_reciente_a_la_mas_antigua(self, _):
        marcar(self.hijo, date(2026, 11, 3))
        marcar(self.hijo, date(2026, 11, 4), 'TARDANZA', time(8, 20))
        marcar(self.hijo, date(2026, 11, 2), 'FALTA')
        respuesta = self.pedir(self.hijo)
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        datos = respuesta.json()
        self.assertEqual(datos['hijo']['email'], 'hijo@test.pe')
        self.assertEqual(
            [r['fecha'] for r in datos['registros']],
            ['2026-11-04', '2026-11-03', '2026-11-02'],
        )
        self.assertEqual(
            datos['registros'][0],
            {'fecha': '2026-11-04', 'estado': 'TARDANZA', 'estado_texto': 'Tardanza',
             'hora_registro': '08:20'},
        )
        self.assertIsNone(datos['registros'][2]['hora_registro'])

    def test_resumen_con_porcentaje_donde_la_tardanza_cuenta_como_asistencia(self, _):
        marcar(self.hijo, date(2026, 11, 2))
        marcar(self.hijo, date(2026, 11, 3), 'TARDANZA', time(8, 20))
        marcar(self.hijo, date(2026, 11, 4))
        marcar(self.hijo, date(2026, 10, 30), 'FALTA')
        resumen = self.pedir(self.hijo).json()['resumen']
        self.assertEqual(
            resumen,
            {'presentes': 2, 'tardanzas': 1, 'faltas': 1, 'total': 4, 'porcentaje': 75},
        )

    def test_sin_registros_el_porcentaje_es_nulo(self, _):
        datos = self.pedir(self.hijo).json()
        self.assertEqual(datos['registros'], [])
        self.assertEqual(datos['resumen']['total'], 0)
        self.assertIsNone(datos['resumen']['porcentaje'])

    def test_no_mezcla_la_asistencia_de_otros_alumnos(self, _):
        marcar(self.hijo, date(2026, 11, 4))
        marcar(self.hija, date(2026, 11, 4), 'FALTA')
        marcar(self.ajeno, date(2026, 11, 4), 'FALTA')
        datos = self.pedir(self.hijo).json()
        self.assertEqual(datos['resumen']['total'], 1)
        self.assertEqual(datos['registros'][0]['estado'], 'PRESENTE')

    def test_un_apoderado_con_varios_hijos_ve_a_cada_uno(self, _):
        marcar(self.hijo, date(2026, 11, 4))
        marcar(self.hija, date(2026, 11, 4), 'FALTA')
        self.assertEqual(self.pedir(self.hijo).status_code, 200)
        datos = self.pedir(self.hija).json()
        self.assertEqual(datos['hijo']['email'], 'hija@test.pe')
        self.assertEqual(datos['registros'][0]['estado'], 'FALTA')

    # ---------- Rango de fechas ----------

    def test_sin_fechas_devuelve_los_ultimos_30_dias(self, _):
        marcar(self.hijo, date(2026, 10, 6))   # justo el día 30 (incluido)
        marcar(self.hijo, date(2026, 10, 5))   # un día antes: queda fuera
        marcar(self.hijo, HOY)
        datos = self.pedir(self.hijo).json()
        self.assertEqual((datos['desde'], datos['hasta']), ('2026-10-07', '2026-11-05'))
        self.assertEqual([r['fecha'] for r in datos['registros']], ['2026-11-05'])

    def test_el_rango_incluye_los_dos_extremos(self, _):
        for dia in (1, 2, 3, 4):
            marcar(self.hijo, date(2026, 11, dia))
        datos = self.pedir(self.hijo, '?desde=2026-11-02&hasta=2026-11-03').json()
        self.assertEqual([r['fecha'] for r in datos['registros']], ['2026-11-03', '2026-11-02'])

    def test_con_solo_desde_llega_hasta_hoy(self, _):
        marcar(self.hijo, date(2026, 11, 1))
        marcar(self.hijo, HOY)
        datos = self.pedir(self.hijo, '?desde=2026-11-02').json()
        self.assertEqual(datos['hasta'], '2026-11-05')
        self.assertEqual([r['fecha'] for r in datos['registros']], ['2026-11-05'])

    def test_con_solo_hasta_cuenta_30_dias_hacia_atras(self, _):
        datos = self.pedir(self.hijo, '?hasta=2026-10-31').json()
        self.assertEqual((datos['desde'], datos['hasta']), ('2026-10-02', '2026-10-31'))

    def test_un_dia_suelto(self, _):
        marcar(self.hijo, date(2026, 11, 3))
        datos = self.pedir(self.hijo, '?desde=2026-11-03&hasta=2026-11-03').json()
        self.assertEqual(datos['resumen']['total'], 1)

    def test_fechas_invalidas_devuelven_400(self, _):
        casos = [
            ('?desde=ayer', 'desde'),
            ('?hasta=2026-13-45', 'hasta'),
            ('?desde=2026-11-05&hasta=2026-11-01', 'desde'),
            ('?desde=2025-01-01&hasta=2026-11-05', 'hasta'),
        ]
        for consulta, campo in casos:
            respuesta = self.pedir(self.hijo, consulta)
            self.assertEqual(respuesta.status_code, 400, consulta)
            self.assertIn(campo, respuesta.json())

    def test_un_ano_justo_es_valido(self, _):
        respuesta = self.pedir(self.hijo, '?desde=2025-11-06&hasta=2026-11-05')
        self.assertEqual(respuesta.status_code, 200)

    # ---------- Control de acceso ----------

    def test_el_apoderado_no_ve_la_asistencia_del_hijo_de_otro(self, _):
        marcar(self.ajeno, date(2026, 11, 4))
        respuesta = self.pedir(self.ajeno)
        self.assertEqual(respuesta.status_code, status.HTTP_404_NOT_FOUND)
        self.assertNotIn('registros', respuesta.json())

    def test_un_alumno_inexistente_da_404(self, _):
        respuesta = self.client.get('/api/mis-hijos/999999/asistencia/')
        self.assertEqual(respuesta.status_code, status.HTTP_404_NOT_FOUND)

    def test_un_apoderado_sin_hijos_no_ve_a_nadie(self, _):
        sin_hijos = crear_usuario('solo@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=sin_hijos.perfil)
        self.client.force_authenticate(sin_hijos)
        self.assertEqual(self.pedir(self.hijo).status_code, status.HTTP_404_NOT_FOUND)

    def test_otros_roles_reciben_403(self, _):
        for rol in (Perfil.Rol.ADMIN, Perfil.Rol.COORDINADOR, Perfil.Rol.DOCENTE):
            usuario = crear_usuario(f'{rol}@test.pe', rol)
            self.client.force_authenticate(usuario)
            respuesta = self.pedir(self.hijo)
            self.assertEqual(respuesta.status_code, status.HTTP_403_FORBIDDEN, rol)
        self.client.force_authenticate(self.hijo)
        self.assertEqual(self.pedir(self.hijo).status_code, status.HTTP_403_FORBIDDEN)

    def test_sin_sesion_se_rechaza(self, _):
        self.client.force_authenticate(None)
        respuesta = self.pedir(self.hijo)
        self.assertIn(
            respuesta.status_code,
            (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN),
        )

    def test_el_endpoint_es_de_solo_lectura(self, _):
        for metodo in (self.client.post, self.client.put, self.client.patch, self.client.delete):
            respuesta = metodo(f'/api/mis-hijos/{self.hijo.perfil.id}/asistencia/')
            self.assertEqual(respuesta.status_code, status.HTTP_405_METHOD_NOT_ALLOWED)
