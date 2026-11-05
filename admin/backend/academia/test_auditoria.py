from datetime import datetime, timezone as utc
from unittest.mock import patch
from zoneinfo import ZoneInfo

from django.contrib.auth.models import User
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken

from .auditoria import separar_ruta
from .models import Academia, Perfil, RegistroAuditoria, Sede

LIMA = ZoneInfo('America/Lima')


def crear_perfil(email, rol, nombre=''):
    user = User.objects.create_user(
        username=email, email=email, password='clave12345', first_name=nombre
    )
    return Perfil.objects.create(user=user, rol=rol)


def entrar(cliente, perfil):
    token = RefreshToken.for_user(perfil.user).access_token
    cliente.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')


def crear_registro(usuario=None, nombre='Admin', accion='CREAR', recurso='sedes',
                   objeto_id='', fecha=None):
    datos = dict(
        usuario=usuario, usuario_nombre=nombre, rol='ADMIN', accion=accion,
        recurso=recurso, objeto_id=objeto_id, metodo='POST', ruta=f'/api/{recurso}/',
        estado=201,
    )
    if fecha:
        datos['fecha'] = fecha
    return RegistroAuditoria.objects.create(**datos)


class SepararRutaTests(APITestCase):
    def test_recurso_con_identificador(self):
        self.assertEqual(separar_ruta('/api/alumnos/5/'), ('alumnos', '5'))

    def test_recurso_sin_identificador(self):
        self.assertEqual(separar_ruta('/api/sedes/'), ('sedes', ''))

    def test_accion_sobre_un_objeto(self):
        self.assertEqual(separar_ruta('/api/matriculas/12/activar/'), ('matriculas', '12'))

    def test_ruta_con_guion_y_sin_numero(self):
        self.assertEqual(separar_ruta('/api/alumnos-masivo/cargar/'), ('alumnos-masivo', ''))


class RegistroAutomaticoTests(APITestCase):
    def setUp(self):
        self.admin = crear_perfil('admin@test.pe', Perfil.Rol.ADMIN, 'Lucas')
        self.academia = Academia.objects.create(nombre='Academia Test')
        entrar(self.client, self.admin)

    def crear_sede(self):
        return self.client.post('/api/sedes/', {
            'academia': self.academia.id, 'nombre': 'Sede Norte', 'direccion': 'Av. 1',
        }, format='json')

    def test_crear_queda_registrado(self):
        respuesta = self.crear_sede()
        self.assertEqual(respuesta.status_code, 201)
        registro = RegistroAuditoria.objects.get()
        self.assertEqual(registro.usuario, self.admin.user)
        self.assertEqual(registro.usuario_nombre, 'Lucas')
        self.assertEqual(registro.rol, 'ADMIN')
        self.assertEqual(registro.accion, 'CREAR')
        self.assertEqual(registro.recurso, 'sedes')
        self.assertEqual(registro.metodo, 'POST')
        self.assertEqual(registro.estado, 201)
        self.assertEqual(registro.ruta, '/api/sedes/')

    def test_editar_y_eliminar_guardan_el_objeto(self):
        sede = Sede.objects.create(academia=self.academia, nombre='Sede', direccion='Av. 2')
        self.client.patch(f'/api/sedes/{sede.id}/', {'nombre': 'Nueva'}, format='json')
        self.client.delete(f'/api/sedes/{sede.id}/')
        acciones = list(RegistroAuditoria.objects.order_by('id').values_list(
            'accion', 'objeto_id'
        ))
        self.assertEqual(acciones, [('EDITAR', str(sede.id)), ('ELIMINAR', str(sede.id))])

    def test_sin_nombre_usa_el_correo(self):
        otro = crear_perfil('sin.nombre@test.pe', Perfil.Rol.ADMIN)
        entrar(self.client, otro)
        self.crear_sede()
        self.assertEqual(RegistroAuditoria.objects.get().usuario_nombre, 'sin.nombre@test.pe')

    def test_una_lectura_no_se_registra(self):
        self.client.get('/api/sedes/')
        self.assertEqual(RegistroAuditoria.objects.count(), 0)

    def test_una_escritura_rechazada_no_se_registra(self):
        respuesta = self.client.post('/api/sedes/', {'nombre': 'Sin academia'}, format='json')
        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(RegistroAuditoria.objects.count(), 0)

    def test_sin_sesion_no_se_registra(self):
        self.client.credentials()
        self.crear_sede()
        self.assertEqual(RegistroAuditoria.objects.count(), 0)

    def test_token_invalido_no_se_registra_ni_rompe(self):
        self.client.credentials(HTTP_AUTHORIZATION='Bearer token-falso')
        self.crear_sede()
        self.assertEqual(RegistroAuditoria.objects.count(), 0)

    def test_el_inicio_de_sesion_no_se_registra(self):
        self.client.credentials()
        self.client.post('/api/auth/login/', {
            'username': 'admin@test.pe', 'password': 'clave12345',
        }, format='json')
        self.assertEqual(RegistroAuditoria.objects.count(), 0)

    def test_guarda_la_ip_del_cliente(self):
        self.client.post('/api/sedes/', {
            'academia': self.academia.id, 'nombre': 'S', 'direccion': 'D',
        }, format='json', HTTP_X_FORWARDED_FOR='203.0.113.7, 10.0.0.1')
        self.assertEqual(RegistroAuditoria.objects.get().ip, '203.0.113.7')

    def test_un_fallo_al_registrar_no_rompe_la_respuesta(self):
        with patch('academia.auditoria.registrar', side_effect=RuntimeError('falla')):
            with self.assertLogs('academia.auditoria', level='ERROR'):
                respuesta = self.crear_sede()
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(Sede.objects.count(), 1)

    def test_borrar_el_usuario_conserva_el_registro(self):
        self.crear_sede()
        self.admin.user.delete()
        registro = RegistroAuditoria.objects.get()
        self.assertIsNone(registro.usuario)
        self.assertEqual(registro.usuario_nombre, 'Lucas')


class AuditoriaApiTests(APITestCase):
    def setUp(self):
        self.admin = crear_perfil('admin@test.pe', Perfil.Rol.ADMIN, 'Lucas')
        self.otro = crear_perfil('otro@test.pe', Perfil.Rol.ADMIN, 'Otro')
        entrar(self.client, self.admin)

    def test_solo_el_administrador_puede_ver_el_historial(self):
        for rol in (Perfil.Rol.COORDINADOR, Perfil.Rol.DOCENTE):
            perfil = crear_perfil(f'{rol}@test.pe', rol)
            entrar(self.client, perfil)
            self.assertEqual(self.client.get('/api/auditoria/').status_code, 403)
        self.client.credentials()
        self.assertEqual(self.client.get('/api/auditoria/').status_code, 401)

    def test_lista_de_la_mas_reciente_a_la_mas_antigua(self):
        crear_registro(fecha=datetime(2026, 11, 1, 15, tzinfo=utc.utc), objeto_id='1')
        crear_registro(fecha=datetime(2026, 11, 3, 15, tzinfo=utc.utc), objeto_id='3')
        crear_registro(fecha=datetime(2026, 11, 2, 15, tzinfo=utc.utc), objeto_id='2')
        datos = self.client.get('/api/auditoria/').json()
        self.assertEqual(datos['total'], 3)
        self.assertEqual([r['objeto_id'] for r in datos['resultados']], ['3', '2', '1'])

    def test_cada_registro_trae_lo_necesario(self):
        crear_registro(usuario=self.admin.user, nombre='Lucas', accion='EDITAR',
                       recurso='alumnos', objeto_id='9')
        fila = self.client.get('/api/auditoria/').json()['resultados'][0]
        self.assertEqual(fila['usuario'], self.admin.id)
        self.assertEqual(fila['usuario_nombre'], 'Lucas')
        self.assertEqual(fila['accion'], 'EDITAR')
        self.assertEqual(fila['accion_texto'], 'Editó')
        self.assertEqual(fila['recurso'], 'alumnos')
        self.assertEqual(fila['objeto_id'], '9')
        for campo in ('id', 'fecha', 'rol', 'metodo', 'ruta', 'estado', 'ip'):
            self.assertIn(campo, fila)

    def test_filtra_por_usuario(self):
        crear_registro(usuario=self.admin.user)
        crear_registro(usuario=self.otro.user)
        datos = self.client.get(f'/api/auditoria/?usuario={self.otro.id}').json()
        self.assertEqual(datos['total'], 1)
        self.assertEqual(datos['resultados'][0]['usuario'], self.otro.id)

    def test_filtra_por_accion_y_recurso(self):
        crear_registro(accion='CREAR', recurso='sedes')
        crear_registro(accion='ELIMINAR', recurso='sedes')
        crear_registro(accion='ELIMINAR', recurso='alumnos')
        self.assertEqual(self.client.get('/api/auditoria/?accion=ELIMINAR').json()['total'], 2)
        self.assertEqual(self.client.get('/api/auditoria/?recurso=sedes').json()['total'], 2)
        datos = self.client.get('/api/auditoria/?accion=ELIMINAR&recurso=alumnos').json()
        self.assertEqual(datos['total'], 1)

    def test_filtra_por_fechas_con_hora_de_lima(self):
        # 04:30 UTC del 3 es 23:30 del día 2 en Lima.
        crear_registro(fecha=datetime(2026, 11, 3, 4, 30, tzinfo=utc.utc), objeto_id='noche')
        crear_registro(fecha=datetime(2026, 11, 3, 15, 0, tzinfo=utc.utc), objeto_id='dia3')
        crear_registro(fecha=datetime(2026, 11, 5, 3, 0, tzinfo=utc.utc), objeto_id='noche4')

        dia2 = self.client.get('/api/auditoria/?desde=2026-11-02&hasta=2026-11-02').json()
        self.assertEqual([r['objeto_id'] for r in dia2['resultados']], ['noche'])

        dia3 = self.client.get('/api/auditoria/?desde=2026-11-03&hasta=2026-11-03').json()
        self.assertEqual([r['objeto_id'] for r in dia3['resultados']], ['dia3'])

        desde = self.client.get('/api/auditoria/?desde=2026-11-03').json()
        self.assertEqual(desde['total'], 2)
        hasta = self.client.get('/api/auditoria/?hasta=2026-11-03').json()
        self.assertEqual(hasta['total'], 2)
        todo = self.client.get('/api/auditoria/?hasta=2026-11-04').json()
        self.assertEqual(todo['total'], 3)

    def test_pagina_los_resultados(self):
        for numero in range(25):
            crear_registro(objeto_id=str(numero))
        primera = self.client.get('/api/auditoria/').json()
        self.assertEqual((primera['total'], primera['pagina'], primera['paginas']), (25, 1, 2))
        self.assertEqual(len(primera['resultados']), 20)
        segunda = self.client.get('/api/auditoria/?pagina=2').json()
        self.assertEqual(len(segunda['resultados']), 5)
        corta = self.client.get('/api/auditoria/?por_pagina=10').json()
        self.assertEqual((len(corta['resultados']), corta['paginas']), (10, 3))

    def test_limita_el_tamano_de_pagina(self):
        for numero in range(105):
            crear_registro(objeto_id=str(numero))
        datos = self.client.get('/api/auditoria/?por_pagina=500').json()
        self.assertEqual(len(datos['resultados']), 100)

    def test_historial_vacio(self):
        datos = self.client.get('/api/auditoria/').json()
        self.assertEqual(
            (datos['total'], datos['pagina'], datos['paginas'], datos['resultados']),
            (0, 1, 1, []),
        )

    def test_parametros_invalidos_devuelven_400(self):
        for consulta, campo in [
            ('usuario=abc', 'usuario'),
            ('accion=BORRAR', 'accion'),
            ('desde=ayer', 'desde'),
            ('hasta=2026-13-45', 'hasta'),
        ]:
            respuesta = self.client.get(f'/api/auditoria/?{consulta}')
            self.assertEqual(respuesta.status_code, 400, consulta)
            self.assertIn(campo, respuesta.json())

    def test_consultar_el_historial_no_genera_registros(self):
        self.client.get('/api/auditoria/')
        self.assertEqual(RegistroAuditoria.objects.count(), 0)
