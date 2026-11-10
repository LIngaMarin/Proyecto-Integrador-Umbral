from django.contrib.auth.models import User
from rest_framework.test import APITestCase

from .models import Apoderado, Perfil
from .test_usuarios import crear_usuario

URL = '/api/mis-datos/contacto/'


class ContactoApoderadoTests(APITestCase):
    def setUp(self):
        self.padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        self.apoderado = Apoderado.objects.create(
            perfil=self.padre.perfil, telefono='999111222'
        )
        crear_usuario('otro@test.pe', Perfil.Rol.APODERADO)
        self.client.force_authenticate(self.padre)

    def guardar(self, email='padre@test.pe', telefono='999111222'):
        return self.client.put(URL, {'email': email, 'telefono': telefono}, format='json')

    # ---------- Lectura ----------

    def test_devuelve_los_datos_de_contacto(self):
        r = self.client.get(URL)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data, {'email': 'padre@test.pe', 'telefono': '999111222'})

    # ---------- Actualización ----------

    def test_actualiza_telefono_y_correo(self):
        r = self.guardar('nuevo@test.pe', '987654321')
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data, {'email': 'nuevo@test.pe', 'telefono': '987654321'})
        self.padre.refresh_from_db()
        self.apoderado.refresh_from_db()
        self.assertEqual(self.padre.email, 'nuevo@test.pe')
        self.assertEqual(self.padre.username, 'nuevo@test.pe')
        self.assertEqual(self.apoderado.telefono, '987654321')

    def test_normaliza_espacios_y_guiones_del_telefono(self):
        r = self.guardar(telefono='+51 987-654-321')
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data['telefono'], '+51987654321')

    def test_guarda_el_correo_en_minusculas(self):
        r = self.guardar(email='  Padre@Test.PE ')
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data['email'], 'padre@test.pe')

    def test_puede_dejar_el_telefono_vacio(self):
        r = self.guardar(telefono='')
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.data['telefono'], '')

    def test_conserva_su_propio_correo_sin_marcarlo_como_repetido(self):
        self.assertEqual(self.guardar().status_code, 200)

    # ---------- Validaciones ----------

    def test_rechaza_telefono_con_letras(self):
        r = self.guardar(telefono='abc12345')
        self.assertEqual(r.status_code, 400)
        self.assertIn('telefono', r.data)

    def test_rechaza_telefono_demasiado_corto(self):
        self.assertEqual(self.guardar(telefono='12345').status_code, 400)

    def test_rechaza_correo_sin_formato_valido(self):
        r = self.guardar(email='no-es-correo')
        self.assertEqual(r.status_code, 400)
        self.assertIn('email', r.data)

    def test_rechaza_correo_vacio(self):
        self.assertEqual(self.guardar(email='').status_code, 400)

    def test_rechaza_correo_de_otro_usuario(self):
        r = self.guardar(email='OTRO@test.pe')
        self.assertEqual(r.status_code, 400)
        self.assertIn('email', r.data)

    def test_un_error_no_cambia_ningun_dato(self):
        self.guardar(email='nuevo@test.pe', telefono='abc')
        self.padre.refresh_from_db()
        self.assertEqual(self.padre.email, 'padre@test.pe')
        self.assertFalse(User.objects.filter(username='nuevo@test.pe').exists())

    # ---------- Acceso ----------

    def test_solo_entran_apoderados(self):
        docente = crear_usuario('docente@test.pe', Perfil.Rol.DOCENTE)
        self.client.force_authenticate(docente)
        self.assertEqual(self.client.get(URL).status_code, 403)
        self.assertEqual(self.guardar().status_code, 403)

    def test_sin_sesion_no_entra(self):
        self.client.force_authenticate(None)
        self.assertIn(self.client.get(URL).status_code, (401, 403))
