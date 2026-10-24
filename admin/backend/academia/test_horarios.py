from datetime import date

from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Aula, Ciclo, Curso, Festivo, Horario, Perfil, Sede, Seccion


def crear_perfil(email, rol):
    user = User.objects.create_user(username=email, email=email, password='clave12345')
    return Perfil.objects.create(user=user, rol=rol)


class HorariosApiTests(APITestCase):
    def setUp(self):
        crear_perfil('coord@test.pe', Perfil.Rol.COORDINADOR)
        crear_perfil('alumno@test.pe', Perfil.Rol.ALUMNO)
        self.doc1 = crear_perfil('doc1@test.pe', Perfil.Rol.DOCENTE)
        self.doc2 = crear_perfil('doc2@test.pe', Perfil.Rol.DOCENTE)

        self.academia = Academia.objects.create(nombre='Academia Test')
        sede = Sede.objects.create(academia=self.academia, nombre='Central', direccion='Av. 1')
        self.aula1 = Aula.objects.create(sede=sede, nombre='A-101', capacidad=30)
        self.aula2 = Aula.objects.create(sede=sede, nombre='A-102', capacidad=30)
        ciclo = Ciclo.objects.create(
            academia=self.academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        curso = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.sec1 = Seccion.objects.create(curso=curso, nombre='A', cupo=10)
        self.sec2 = Seccion.objects.create(curso=curso, nombre='B', cupo=10)
        self.autenticar('coord@test.pe')

    def autenticar(self, email):
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': email, 'password': 'clave12345'},
            format='json',
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")

    def datos(self, seccion=None, docente=None, aula=None, dia=0, inicio='08:00', fin='10:00'):
        return {
            'seccion': (seccion or self.sec1).id,
            'docente': (docente or self.doc1).id,
            'aula': (aula or self.aula1).id,
            'dia_semana': dia,
            'hora_inicio': inicio,
            'hora_fin': fin,
        }

    def crear(self, **kwargs):
        return self.client.post('/api/horarios/', self.datos(**kwargs), format='json')

    def test_crea_un_horario_valido(self):
        self.assertEqual(self.crear().status_code, status.HTTP_201_CREATED)

    def test_detecta_conflicto_de_aula(self):
        self.crear()
        respuesta = self.crear(seccion=self.sec2, docente=self.doc2, inicio='09:00', fin='11:00')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('aula', respuesta.data)

    def test_detecta_conflicto_de_docente(self):
        self.crear()
        respuesta = self.crear(seccion=self.sec2, aula=self.aula2, inicio='09:00', fin='11:00')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('docente', respuesta.data)

    def test_detecta_conflicto_de_seccion(self):
        self.crear()
        respuesta = self.crear(docente=self.doc2, aula=self.aula2, inicio='09:00', fin='11:00')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('seccion', respuesta.data)

    def test_horarios_contiguos_no_chocan(self):
        self.crear()
        respuesta = self.crear(inicio='10:00', fin='12:00')
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_mismo_horario_otro_dia_no_choca(self):
        self.crear()
        self.assertEqual(self.crear(dia=1).status_code, status.HTTP_201_CREATED)

    def test_hora_fin_debe_ser_posterior(self):
        respuesta = self.crear(inicio='10:00', fin='09:00')
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('hora_fin', respuesta.data)

    def test_editar_no_choca_consigo_mismo(self):
        creado = self.crear()
        respuesta = self.client.put(
            f"/api/horarios/{creado.data['id']}/",
            self.datos(inicio='08:00', fin='09:30'),
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)

    def test_desactivar_libera_el_horario(self):
        creado = self.crear()
        self.client.delete(f"/api/horarios/{creado.data['id']}/")
        respuesta = self.crear(seccion=self.sec2, docente=self.doc2)
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)

    def test_solo_un_docente_puede_dictar(self):
        alumno = Perfil.objects.get(user__email='alumno@test.pe')
        respuesta = self.crear(docente=alumno)
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_sesiones_de_una_fecha(self):
        self.crear(dia=0)
        respuesta = self.client.get('/api/sesiones/?fecha=2026-10-19')
        self.assertEqual(len(respuesta.data['sesiones']), 1)

    def test_no_hay_sesiones_en_dias_festivos(self):
        self.crear(dia=0)
        Festivo.objects.create(academia=self.academia, fecha=date(2026, 10, 19), nombre='Feriado')
        respuesta = self.client.get('/api/sesiones/?fecha=2026-10-19')
        self.assertEqual(respuesta.data['sesiones'], [])
        self.assertEqual(respuesta.data['festivos'], ['Feriado'])
