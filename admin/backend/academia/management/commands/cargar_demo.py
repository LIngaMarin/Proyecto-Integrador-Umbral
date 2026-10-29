"""Carga una academia de demostración con datos coherentes.

Uso:  python manage.py cargar_demo

Es idempotente: se puede ejecutar varias veces sin duplicar datos y no
modifica contraseñas de cuentas que ya existan. Todas las cuentas de
demostración usan el dominio @demo.umbral.pe.
"""
from datetime import date, time
from decimal import Decimal

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand
from django.db import transaction

from academia.models import (
    Academia, Apoderado, Aula, Ciclo, ConfiguracionIA, Curso, Festivo,
    Horario, Marca, Matricula, Perfil, Resultado, Seccion, Sede, Simulacro,
    UsoIA,
)

User = get_user_model()

CLAVE = 'Umbral2026!'
DOMINIO = 'demo.umbral.pe'

SEDES = [
    ('Sede Lima Centro', 'Av. Abancay 1250, Cercado de Lima', 'Tel. 01 555 0101',
     ['A-101', 'A-102', 'A-103'], 35),
    ('Sede San Miguel', 'Av. La Marina 2480, San Miguel', 'Tel. 01 555 0202',
     ['B-201', 'B-202'], 30),
]

# (curso, docente, [(sección, aula, días, hora inicio, hora fin), ...])
CURSOS = [
    ('Matemática', ('Carlos', 'Ramírez'),
     [('A', 'A-101', (0, 2), time(8, 0), time(10, 0)),
      ('B', 'B-201', (0, 2), time(15, 0), time(17, 0))]),
    ('Física', ('Lucía', 'Fernández'),
     [('A', 'A-101', (0, 2), time(10, 15), time(12, 15)),
      ('B', 'B-201', (0, 2), time(17, 15), time(19, 15))]),
    ('Química', ('Jorge', 'Salazar'),
     [('A', 'A-102', (1, 3), time(8, 0), time(10, 0)),
      ('B', 'B-202', (1, 3), time(15, 0), time(17, 0))]),
    ('Comunicación', ('Patricia', 'Huamán'),
     [('A', 'A-102', (1, 3), time(10, 15), time(12, 15)),
      ('B', 'B-202', (1, 3), time(17, 15), time(19, 15))]),
]

ALUMNOS = [
    ('Diego', 'Quispe Mamani'), ('Valeria', 'Torres Rojas'),
    ('Mateo', 'Castillo Vega'), ('Camila', 'Paredes León'),
    ('Sebastián', 'Mendoza Cruz'), ('Fernanda', 'Ríos Alarcón'),
    ('Joaquín', 'Silva Pinedo'), ('Andrea', 'Chávez Núñez'),
    ('Renzo', 'Gutiérrez Soto'), ('Daniela', 'Vargas Ibáñez'),
    ('Adrián', 'Campos Ledesma'), ('Luciana', 'Ortiz Benites'),
]

APODERADOS = [
    ('Rosa', 'Mamani Flores', '987000111', [0, 1]),
    ('Miguel', 'Castillo Reyes', '987000222', [2]),
    ('Elena', 'Silva Prado', '987000333', [6, 7]),
]

FESTIVOS = [
    (date(2026, 11, 1), 'Día de Todos los Santos'),
    (date(2026, 12, 8), 'Inmaculada Concepción'),
    (date(2026, 12, 25), 'Navidad'),
]

SIMULACROS = [('Simulacro 1', date(2026, 9, 26)), ('Simulacro 2', date(2026, 10, 17))]

# Secciones con rendimiento bajo, para que el reporte muestre "refuerzo".
RENDIMIENTO_BAJO = {('Matemática', 'B'): 9.5, ('Química', 'B'): 10.0}


def correo(nombre, apellido):
    limpio = lambda s: (
        s.lower().split()[0]
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')
    )
    return f'{limpio(nombre)}.{limpio(apellido)}@{DOMINIO}'


class Command(BaseCommand):
    help = 'Carga una academia de demostración (idempotente).'

    @transaction.atomic
    def handle(self, *args, **opciones):
        self.cuentas = []
        academia = (
            Academia.objects.filter(activa=True).order_by('id').first()
            or Academia.objects.create(nombre='Academia Preuniversitaria Umbral')
        )

        sedes = {}
        aulas = {}
        for nombre, direccion, contacto, nombres_aulas, capacidad in SEDES:
            sede, _ = Sede.objects.get_or_create(
                academia=academia, nombre=nombre, direccion=direccion,
                defaults={'contacto': contacto},
            )
            sedes[nombre] = sede
            for aula in nombres_aulas:
                obj, _ = Aula.objects.get_or_create(
                    sede=sede, nombre=aula, defaults={'capacidad': capacidad}
                )
                aulas[aula] = obj
        sede_centro, sede_sm = sedes['Sede Lima Centro'], sedes['Sede San Miguel']

        ciclo, _ = Ciclo.objects.get_or_create(
            academia=academia, nombre='Ciclo 2026-II',
            defaults={'fecha_inicio': date(2026, 8, 17), 'fecha_fin': date(2026, 12, 18)},
        )

        for fecha, nombre in FESTIVOS:
            Festivo.objects.get_or_create(
                academia=academia, fecha=fecha, defaults={'nombre': nombre}
            )

        alumnos = [
            self._usuario(n, a, Perfil.Rol.ALUMNO, sede_centro if i < 6 else sede_sm)
            for i, (n, a) in enumerate(ALUMNOS)
        ]

        docentes = []
        secciones = {}
        for nombre_curso, (dn, da), turnos in CURSOS:
            docente = self._usuario(dn, da, Perfil.Rol.DOCENTE, sede_centro)
            docentes.append(docente)
            curso, _ = Curso.objects.get_or_create(ciclo=ciclo, nombre=nombre_curso)
            for letra, nombre_aula, dias, inicio, fin in turnos:
                seccion, _ = Seccion.objects.get_or_create(
                    curso=curso, nombre=letra,
                    defaults={'aula': aulas[nombre_aula], 'cupo': 30},
                )
                secciones[(nombre_curso, letra)] = seccion
                for dia in dias:
                    Horario.objects.get_or_create(
                        seccion=seccion, docente=docente, aula=aulas[nombre_aula],
                        dia_semana=dia, hora_inicio=inicio,
                        defaults={'hora_fin': fin},
                    )

        # Los 6 primeros alumnos van a la sección A de todos los cursos y el resto a la B.
        matriculas = {}
        for i, alumno in enumerate(alumnos):
            letra = 'A' if i < 6 else 'B'
            for nombre_curso, _, _ in CURSOS:
                m, _ = Matricula.objects.get_or_create(
                    alumno=alumno, seccion=secciones[(nombre_curso, letra)]
                )
                matriculas[(i, nombre_curso)] = m

        for indice_curso, (nombre_curso, _, _) in enumerate(CURSOS):
            curso = Curso.objects.get(ciclo=ciclo, nombre=nombre_curso)
            for indice_sim, (nombre_sim, fecha) in enumerate(SIMULACROS):
                simulacro, _ = Simulacro.objects.get_or_create(
                    curso=curso, nombre=nombre_sim, defaults={'fecha': fecha}
                )
                for i in range(len(alumnos)):
                    letra = 'A' if i < 6 else 'B'
                    base = RENDIMIENTO_BAJO.get((nombre_curso, letra), 14.5)
                    variacion = ((i * 7 + indice_curso * 3 + indice_sim * 2) % 5) - 2
                    puntaje = min(20.0, max(0.0, base + variacion))
                    Resultado.objects.get_or_create(
                        simulacro=simulacro, matricula=matriculas[(i, nombre_curso)],
                        defaults={'puntaje': Decimal(str(puntaje))},
                    )

        for nombre, apellido, telefono, hijos in APODERADOS:
            perfil = self._usuario(nombre, apellido, Perfil.Rol.APODERADO, None)
            apoderado, _ = Apoderado.objects.get_or_create(
                perfil=perfil, defaults={'telefono': telefono}
            )
            apoderado.hijos.add(*[alumnos[i] for i in hijos])

        Marca.objects.get_or_create(
            academia=academia,
            defaults={'color_primario': '#0F766E', 'color_secundario': '#0F4C4A'},
        )
        ConfiguracionIA.objects.get_or_create(academia=academia)
        for docente, usos in zip(docentes, (12, 5, 0, 2)):
            if usos and not UsoIA.objects.filter(docente=docente).exists():
                UsoIA.objects.bulk_create([UsoIA(docente=docente) for _ in range(usos)])

        self._resumen(academia)

    def _usuario(self, nombre, apellido, rol, sede):
        email = correo(nombre, apellido)
        user, creado = User.objects.get_or_create(
            username=email,
            defaults={'email': email, 'first_name': nombre, 'last_name': apellido},
        )
        if creado:
            user.set_password(CLAVE)
            user.save()
        perfil, _ = Perfil.objects.get_or_create(
            user=user, defaults={'rol': rol, 'sede': sede}
        )
        self.cuentas.append((perfil.get_rol_display(), f'{nombre} {apellido}', email))
        return perfil

    def _resumen(self, academia):
        self.stdout.write(self.style.SUCCESS(f'Datos de demostración listos en "{academia.nombre}".'))
        self.stdout.write(f'Contraseña de todas las cuentas demo: {CLAVE}\n')
        for rol, nombre, email in self.cuentas:
            self.stdout.write(f'  {rol:<22} {nombre:<24} {email}')
