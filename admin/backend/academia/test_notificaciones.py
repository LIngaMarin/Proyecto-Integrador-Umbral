from datetime import date, time

from rest_framework import status
from rest_framework.test import APITestCase

from .models import Apoderado, Asistencia, Notificacion, Perfil
from .test_usuarios import crear_usuario

DIA = date(2026, 11, 4)


def marcar(alumno, estado='PRESENTE', dia=DIA):
    hora = None if estado == 'FALTA' else time(7, 55)
    return Asistencia.objects.create(
        alumno=alumno.perfil, fecha=dia, estado=estado, hora_registro=hora
    )


class NotificacionPorInasistenciaTests(APITestCase):
    def setUp(self):
        self.hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        self.madre = crear_usuario('madre@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=self.padre.perfil).hijos.add(self.hijo.perfil)
        Apoderado.objects.create(perfil=self.madre.perfil).hijos.add(self.hijo.perfil)

    def test_una_falta_avisa_a_cada_apoderado_del_alumno(self):
        marcar(self.hijo, 'FALTA')
        self.assertEqual(Notificacion.objects.count(), 2)
        apoderados = set(Notificacion.objects.values_list('apoderado__perfil__user__email', flat=True))
        self.assertEqual(apoderados, {'padre@test.pe', 'madre@test.pe'})

    def test_la_notificacion_describe_la_falta(self):
        marcar(self.hijo, 'FALTA')
        notificacion = Notificacion.objects.get(apoderado__perfil=self.padre.perfil)
        self.assertEqual(notificacion.tipo, 'INASISTENCIA')
        self.assertEqual(notificacion.alumno, self.hijo.perfil)
        self.assertFalse(notificacion.leida)
        self.assertIn('04/11/2026', notificacion.mensaje)

    def test_presente_y_tardanza_no_generan_notificacion(self):
        marcar(self.hijo, 'PRESENTE')
        Asistencia.objects.create(alumno=self.hijo.perfil, fecha=date(2026, 11, 5),
                                  estado='TARDANZA', hora_registro=time(8, 30))
        self.assertEqual(Notificacion.objects.count(), 0)

    def test_volver_a_guardar_la_misma_falta_no_duplica(self):
        asistencia = marcar(self.hijo, 'FALTA')
        asistencia.save()
        asistencia.hora_registro = None
        asistencia.save()
        self.assertEqual(Notificacion.objects.count(), 2)

    def test_alumno_sin_apoderado_no_genera_nada(self):
        suelto = crear_usuario('suelto@test.pe', Perfil.Rol.ALUMNO)
        marcar(suelto, 'FALTA')
        self.assertEqual(Notificacion.objects.count(), 0)


class MisNotificacionesTests(APITestCase):
    def setUp(self):
        self.hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)
        self.ajeno = crear_usuario('ajeno@test.pe', Perfil.Rol.ALUMNO)
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        otro = crear_usuario('otro@test.pe', Perfil.Rol.APODERADO)
        Apoderado.objects.create(perfil=self.padre.perfil).hijos.add(self.hijo.perfil)
        Apoderado.objects.create(perfil=otro.perfil).hijos.add(self.ajeno.perfil)
        self.client.force_authenticate(self.padre)

    def listar(self):
        return self.client.get('/api/mis-notificaciones/')

    def leer(self, notificacion):
        return self.client.post(f'/api/mis-notificaciones/{notificacion.id}/leer/')

    def test_lista_vacia_sin_notificaciones(self):
        datos = self.listar().json()
        self.assertEqual(datos, {'sin_leer': 0, 'notificaciones': []})

    def test_lista_la_mas_reciente_primero_con_contador_de_no_leidas(self):
        for dia in (date(2026, 11, 2), date(2026, 11, 3)):
            Asistencia.objects.create(alumno=self.hijo.perfil, fecha=dia, estado='FALTA')
        datos = self.listar().json()
        self.assertEqual(datos['sin_leer'], 2)
        self.assertIn('03/11/2026', datos['notificaciones'][0]['mensaje'])
        self.assertIn('02/11/2026', datos['notificaciones'][1]['mensaje'])
        self.assertEqual(datos['notificaciones'][0]['alumno']['email'], 'hijo@test.pe')

    def test_no_muestra_notificaciones_de_hijos_ajenos(self):
        marcar(self.ajeno, 'FALTA')
        marcar(self.hijo, 'FALTA')
        datos = self.listar().json()
        self.assertEqual(len(datos['notificaciones']), 1)
        self.assertEqual(datos['notificaciones'][0]['alumno']['email'], 'hijo@test.pe')
        self.assertEqual(datos['sin_leer'], 1)

    def test_leer_una_notificacion_propia_la_marca_como_leida(self):
        marcar(self.hijo, 'FALTA')
        notificacion = Notificacion.objects.get(apoderado__perfil=self.padre.perfil)
        respuesta = self.leer(notificacion)
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertTrue(respuesta.json()['leida'])
        notificacion.refresh_from_db()
        self.assertTrue(notificacion.leida)
        self.assertEqual(self.listar().json()['sin_leer'], 0)

    def test_leer_dos_veces_es_idempotente(self):
        marcar(self.hijo, 'FALTA')
        notificacion = Notificacion.objects.get(apoderado__perfil=self.padre.perfil)
        self.leer(notificacion)
        self.assertEqual(self.leer(notificacion).status_code, status.HTTP_200_OK)

    def test_leer_una_notificacion_ajena_devuelve_404(self):
        marcar(self.ajeno, 'FALTA')
        ajena = Notificacion.objects.get(alumno=self.ajeno.perfil)
        self.assertEqual(self.leer(ajena).status_code, status.HTTP_404_NOT_FOUND)
        ajena.refresh_from_db()
        self.assertFalse(ajena.leida)

    def test_un_docente_no_puede_usar_las_notificaciones_de_apoderado(self):
        docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.client.force_authenticate(docente)
        self.assertEqual(self.listar().status_code, status.HTTP_403_FORBIDDEN)
