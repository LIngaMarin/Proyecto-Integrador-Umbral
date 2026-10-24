from datetime import date

from django.contrib.auth.models import User
from django.core import mail
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Academia, Ciclo, Curso, Matricula, Perfil, Seccion


class MatricularNuevoAlumnoTests(APITestCase):
    def setUp(self):
        user = User.objects.create_user(
            username='coord@test.pe', email='coord@test.pe', password='clave12345'
        )
        Perfil.objects.create(user=user, rol=Perfil.Rol.COORDINADOR)
        academia = Academia.objects.create(nombre='Academia Test')
        ciclo = Ciclo.objects.create(
            academia=academia, nombre='2026-II',
            fecha_inicio=date(2026, 8, 1), fecha_fin=date(2026, 12, 20),
        )
        curso = Curso.objects.create(ciclo=ciclo, nombre='Matemática')
        self.seccion = Seccion.objects.create(curso=curso, nombre='A', cupo=1)
        self.autenticar('coord@test.pe', 'clave12345')

    def autenticar(self, email, clave):
        respuesta = self.client.post(
            '/api/auth/login/', {'username': email, 'password': clave}, format='json'
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {respuesta.data['access']}")
        return respuesta

    def datos(self, email='nuevo@test.pe'):
        return {
            'first_name': 'Rosa',
            'last_name': 'Quispe',
            'email': email,
            'seccion': self.seccion.id,
        }

    def test_crea_alumno_matricula_y_envia_credenciales(self):
        respuesta = self.client.post('/api/matricular-nuevo/', self.datos(), format='json')
        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertTrue(respuesta.data['correo_enviado'])
        self.assertEqual(Matricula.objects.count(), 1)
        self.assertEqual(Perfil.objects.get(user__email='nuevo@test.pe').rol, Perfil.Rol.ALUMNO)
        self.assertEqual(len(mail.outbox), 1)
        self.assertEqual(mail.outbox[0].to, ['nuevo@test.pe'])

    def test_la_contrasena_del_correo_permite_iniciar_sesion(self):
        self.client.post('/api/matricular-nuevo/', self.datos(), format='json')
        cuerpo = mail.outbox[0].body
        clave = cuerpo.split('Contraseña temporal: ')[1].split()[0]
        self.client.credentials()
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': 'nuevo@test.pe', 'password': clave},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)

    def test_correo_repetido_se_rechaza(self):
        self.client.post('/api/matricular-nuevo/', self.datos(), format='json')
        respuesta = self.client.post(
            '/api/matricular-nuevo/',
            {**self.datos(), 'seccion': self.seccion.id},
            format='json',
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('email', respuesta.data)

    def test_seccion_llena_no_deja_usuario_a_medias(self):
        self.client.post('/api/matricular-nuevo/', self.datos('uno@test.pe'), format='json')
        respuesta = self.client.post(
            '/api/matricular-nuevo/', self.datos('dos@test.pe'), format='json'
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(User.objects.filter(email='dos@test.pe').exists())
