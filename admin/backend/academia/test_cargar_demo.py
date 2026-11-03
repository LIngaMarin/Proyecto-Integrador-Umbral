from io import StringIO

from django.core.management import call_command
from django.test import TestCase

from academia.temas_criticos import temas_por_seccion
from academia.models import (
    Apoderado, Aula, Curso, ErrorTema, Horario, Matricula, Perfil, Resultado,
    Seccion, Sede, Simulacro, Tema,
)


def cargar():
    salida = StringIO()
    call_command('cargar_demo', stdout=salida)
    return salida.getvalue()


class CargarDemoTests(TestCase):
    def test_crea_los_datos_esperados(self):
        texto = cargar()
        self.assertIn('Umbral2026!', texto)
        self.assertEqual(Sede.objects.count(), 2)
        self.assertEqual(Aula.objects.count(), 5)
        self.assertEqual(Curso.objects.count(), 4)
        self.assertEqual(Seccion.objects.count(), 8)
        self.assertEqual(Perfil.objects.filter(rol='ALUMNO').count(), 12)
        self.assertEqual(Perfil.objects.filter(rol='DOCENTE').count(), 4)
        self.assertEqual(Apoderado.objects.count(), 3)
        self.assertEqual(Matricula.objects.count(), 48)
        self.assertEqual(Simulacro.objects.count(), 8)
        self.assertEqual(Resultado.objects.count(), 96)
        self.assertEqual(Tema.objects.count(), 13)
        self.assertEqual(ErrorTema.objects.count(), 24 * 13)

    def test_el_salon_debil_tiene_mas_errores_en_el_primer_tema(self):
        cargar()
        simulacro = Simulacro.objects.get(curso__nombre='Matemática', nombre='Simulacro 2')
        salones = temas_por_seccion(simulacro)
        self.assertEqual(salones[0]['nombre'], 'Matemática - B')
        self.assertEqual(salones[0]['temas'][0]['tema'], 'Álgebra')

    def test_es_idempotente(self):
        cargar()
        antes = (
            Perfil.objects.count(), Matricula.objects.count(),
            Horario.objects.count(), Resultado.objects.count(), ErrorTema.objects.count(),
        )
        cargar()
        despues = (
            Perfil.objects.count(), Matricula.objects.count(),
            Horario.objects.count(), Resultado.objects.count(), ErrorTema.objects.count(),
        )
        self.assertEqual(antes, despues)

    def test_las_cuentas_demo_pueden_iniciar_sesion(self):
        cargar()
        respuesta = self.client.post(
            '/api/auth/login/',
            {'username': 'carlos.ramirez@demo.umbral.pe', 'password': 'Umbral2026!'},
            content_type='application/json',
        )
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json()['rol'], 'DOCENTE')

    def test_horarios_sin_cruces_de_docente_ni_aula(self):
        cargar()
        bloques = list(Horario.objects.all())
        for i, a in enumerate(bloques):
            for b in bloques[i + 1:]:
                if a.dia_semana != b.dia_semana:
                    continue
                se_cruzan = a.hora_inicio < b.hora_fin and b.hora_inicio < a.hora_fin
                if se_cruzan:
                    self.assertNotEqual(a.docente_id, b.docente_id)
                    self.assertNotEqual(a.aula_id, b.aula_id)
