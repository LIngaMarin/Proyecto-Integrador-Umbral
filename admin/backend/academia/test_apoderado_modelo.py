from django.test import TestCase

from .models import Apoderado, Perfil
from .test_usuarios import crear_usuario


class ApoderadoModeloTests(TestCase):
    def test_apoderado_se_vincula_a_varios_hijos(self):
        padre = crear_usuario('padre@test.pe', Perfil.Rol.APODERADO)
        hijo1 = crear_usuario('hijo1@test.pe', Perfil.Rol.ALUMNO)
        hijo2 = crear_usuario('hijo2@test.pe', Perfil.Rol.ALUMNO)
        apoderado = Apoderado.objects.create(perfil=padre.perfil, telefono='999888777')
        apoderado.hijos.add(hijo1.perfil, hijo2.perfil)
        self.assertEqual(apoderado.hijos.count(), 2)
        self.assertEqual(list(hijo1.perfil.representantes.all()), [apoderado])

    def test_alumno_puede_tener_dos_apoderados(self):
        hijo = crear_usuario('hijo@test.pe', Perfil.Rol.ALUMNO)
        for correo in ('madre@test.pe', 'padre@test.pe'):
            usuario = crear_usuario(correo, Perfil.Rol.APODERADO)
            Apoderado.objects.create(perfil=usuario.perfil).hijos.add(hijo.perfil)
        self.assertEqual(hijo.perfil.representantes.count(), 2)
