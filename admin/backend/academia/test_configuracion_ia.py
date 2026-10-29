from django.db import IntegrityError, transaction
from django.test import TestCase

from .models import Academia, ConfiguracionIA


class ConfiguracionIAModeloTests(TestCase):
    def test_valores_por_defecto(self):
        academia = Academia.objects.create(nombre='Academia Test')
        config = ConfiguracionIA.objects.create(academia=academia)
        self.assertTrue(config.activa)
        self.assertEqual(config.limite_por_docente, 100)

    def test_una_sola_configuracion_por_academia(self):
        academia = Academia.objects.create(nombre='Academia Test')
        ConfiguracionIA.objects.create(academia=academia)
        with self.assertRaises(IntegrityError):
            with transaction.atomic():
                ConfiguracionIA.objects.create(academia=academia)
