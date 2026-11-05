from django.core.exceptions import ValidationError
from django.core.validators import MinValueValidator
from django.db import models
from django.utils import timezone


class Academia(models.Model):
    nombre = models.CharField(max_length=150)
    activa = models.BooleanField(default=True)
    logo = models.ImageField(upload_to='logos/', null=True, blank=True)
    creado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'adm_academia'

    def __str__(self):
        return self.nombre


class Sede(models.Model):
    academia = models.ForeignKey(Academia, on_delete=models.CASCADE, related_name='sedes')
    nombre = models.CharField(max_length=150)
    direccion = models.CharField(max_length=255)
    contacto = models.CharField(max_length=100, blank=True)
    activa = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_sede'
        unique_together = ('academia', 'nombre', 'direccion')

    def __str__(self):
        return self.nombre


class Aula(models.Model):
    sede = models.ForeignKey(Sede, on_delete=models.CASCADE, related_name='aulas')
    nombre = models.CharField(max_length=100)
    capacidad = models.PositiveIntegerField(validators=[MinValueValidator(1)])
    activa = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_aula'
        unique_together = ('sede', 'nombre')

    def __str__(self):
        return f'{self.nombre} ({self.sede.nombre})'


class Ciclo(models.Model):
    academia = models.ForeignKey(Academia, on_delete=models.CASCADE, related_name='ciclos')
    nombre = models.CharField(max_length=100)
    fecha_inicio = models.DateField()
    fecha_fin = models.DateField()
    activo = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_ciclo'

    def clean(self):
        if self.fecha_fin and self.fecha_inicio and self.fecha_fin <= self.fecha_inicio:
            raise ValidationError('La fecha de fin debe ser posterior a la fecha de inicio.')

    def __str__(self):
        return self.nombre


class Curso(models.Model):
    ciclo = models.ForeignKey(Ciclo, on_delete=models.CASCADE, related_name='cursos')
    nombre = models.CharField(max_length=150)
    activo = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_curso'

    def __str__(self):
        return self.nombre

from django.conf import settings


class Perfil(models.Model):
    class Rol(models.TextChoices):
        ADMIN = 'ADMIN', 'Administrador'
        COORDINADOR = 'COORDINADOR', 'Coordinador académico'
        DOCENTE = 'DOCENTE', 'Docente'
        ALUMNO = 'ALUMNO', 'Alumno'
        APODERADO = 'APODERADO', 'Apoderado'

    user = models.OneToOneField(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='perfil'
    )
    rol = models.CharField(max_length=20, choices=Rol.choices)
    sede = models.ForeignKey(
        Sede, null=True, blank=True, on_delete=models.SET_NULL, related_name='perfiles'
    )
    debe_cambiar_clave = models.BooleanField(default=False)

    class Meta:
        db_table = 'adm_perfil'

    def __str__(self):
        return f'{self.user.username} ({self.rol})'


class Festivo(models.Model):
    academia = models.ForeignKey(Academia, on_delete=models.CASCADE, related_name='festivos')
    fecha = models.DateField()
    nombre = models.CharField(max_length=150)
    activo = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_festivo'
        unique_together = [('academia', 'fecha')]
        ordering = ['fecha']

    def __str__(self):
        return f'{self.fecha} - {self.nombre}'


class Seccion(models.Model):
    curso = models.ForeignKey(Curso, on_delete=models.CASCADE, related_name='secciones')
    nombre = models.CharField(max_length=50)
    aula = models.ForeignKey(Aula, null=True, blank=True, on_delete=models.SET_NULL)
    cupo = models.PositiveIntegerField(validators=[MinValueValidator(1)])
    activa = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_seccion'
        unique_together = [('curso', 'nombre')]

    def __str__(self):
        return f'{self.curso.nombre} - {self.nombre}'


class Matricula(models.Model):
    alumno = models.ForeignKey(Perfil, on_delete=models.CASCADE, related_name='matriculas')
    seccion = models.ForeignKey(Seccion, on_delete=models.CASCADE, related_name='matriculas')
    activa = models.BooleanField(default=True)
    creado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'adm_matricula'
        unique_together = [('alumno', 'seccion')]

    def __str__(self):
        return f'{self.alumno} en {self.seccion}'


class Horario(models.Model):
    class Dia(models.IntegerChoices):
        LUNES = 0, 'Lunes'
        MARTES = 1, 'Martes'
        MIERCOLES = 2, 'Miércoles'
        JUEVES = 3, 'Jueves'
        VIERNES = 4, 'Viernes'
        SABADO = 5, 'Sábado'
        DOMINGO = 6, 'Domingo'

    seccion = models.ForeignKey(Seccion, on_delete=models.CASCADE, related_name='horarios')
    docente = models.ForeignKey(Perfil, on_delete=models.CASCADE, related_name='horarios')
    aula = models.ForeignKey(Aula, on_delete=models.CASCADE, related_name='horarios')
    dia_semana = models.PositiveSmallIntegerField(choices=Dia.choices)
    hora_inicio = models.TimeField()
    hora_fin = models.TimeField()
    activa = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_horario'
        ordering = ['dia_semana', 'hora_inicio']

    def __str__(self):
        return f'{self.seccion} {self.get_dia_semana_display()} {self.hora_inicio:%H:%M}'


class Marca(models.Model):
    academia = models.OneToOneField(Academia, on_delete=models.CASCADE, related_name='marca')
    color_primario = models.CharField(max_length=7, default='#2563EB')
    color_secundario = models.CharField(max_length=7, default='#1F2937')
    borrador_primario = models.CharField(max_length=7, blank=True, default='')
    borrador_secundario = models.CharField(max_length=7, blank=True, default='')
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'adm_marca'

    def __str__(self):
        return f'Marca de {self.academia}'


from django.core.validators import MaxValueValidator  # noqa: E402


class Simulacro(models.Model):
    curso = models.ForeignKey(Curso, on_delete=models.CASCADE, related_name='simulacros')
    nombre = models.CharField(max_length=150)
    fecha = models.DateField()
    activa = models.BooleanField(default=True)

    class Meta:
        db_table = 'adm_simulacro'
        ordering = ['-fecha']

    def __str__(self):
        return f'{self.nombre} ({self.fecha})'


class Resultado(models.Model):
    simulacro = models.ForeignKey(Simulacro, on_delete=models.CASCADE, related_name='resultados')
    matricula = models.ForeignKey(Matricula, on_delete=models.CASCADE, related_name='resultados')
    puntaje = models.DecimalField(
        max_digits=4, decimal_places=2,
        validators=[MinValueValidator(0), MaxValueValidator(20)],
    )

    class Meta:
        db_table = 'adm_resultado'
        unique_together = [('simulacro', 'matricula')]


class Apoderado(models.Model):
    perfil = models.OneToOneField(Perfil, on_delete=models.CASCADE, related_name='apoderado')
    telefono = models.CharField(max_length=20, blank=True)
    hijos = models.ManyToManyField(Perfil, related_name='representantes', blank=True)

    class Meta:
        db_table = 'adm_apoderado'

    def __str__(self):
        return f'Apoderado {self.perfil.user.username}'


class ConfiguracionIA(models.Model):
    academia = models.OneToOneField(
        Academia, on_delete=models.CASCADE, related_name='config_ia'
    )
    activa = models.BooleanField(default=True)
    limite_por_docente = models.PositiveIntegerField(default=100)
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'adm_configuracion_ia'

    def __str__(self):
        estado = 'activa' if self.activa else 'desactivada'
        return f'IA {estado} en {self.academia.nombre} (límite {self.limite_por_docente})'


class UsoIA(models.Model):
    docente = models.ForeignKey(Perfil, on_delete=models.CASCADE, related_name='usos_ia')
    creado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'adm_uso_ia'

    def __str__(self):
        return f'Uso de IA de {self.docente.user.username} el {self.creado_en:%d/%m/%Y}'


class Tema(models.Model):
    curso = models.ForeignKey(Curso, on_delete=models.CASCADE, related_name='temas')
    nombre = models.CharField(max_length=100)

    class Meta:
        db_table = 'adm_tema'
        unique_together = [('curso', 'nombre')]
        ordering = ['nombre']

    def __str__(self):
        return f'{self.nombre} ({self.curso.nombre})'


class ErrorTema(models.Model):
    """Preguntas falladas por un alumno en un tema de un simulacro."""

    resultado = models.ForeignKey(
        Resultado, on_delete=models.CASCADE, related_name='errores_tema'
    )
    tema = models.ForeignKey(Tema, on_delete=models.CASCADE, related_name='errores')
    cantidad = models.PositiveSmallIntegerField()

    class Meta:
        db_table = 'adm_error_tema'
        unique_together = [('resultado', 'tema')]

    def clean(self):
        if self.tema_id and self.resultado_id:
            if self.tema.curso_id != self.resultado.simulacro.curso_id:
                raise ValidationError('El tema debe pertenecer al curso del simulacro.')

    def __str__(self):
        return f'{self.tema.nombre}: {self.cantidad} errores'


class RegistroAuditoria(models.Model):
    """Una acción de escritura hecha en el panel: quién, qué, sobre qué y cuándo."""

    class Accion(models.TextChoices):
        CREAR = 'CREAR', 'Creó'
        EDITAR = 'EDITAR', 'Editó'
        ELIMINAR = 'ELIMINAR', 'Eliminó'

    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True,
        on_delete=models.SET_NULL, related_name='registros_auditoria',
    )
    # Copia del nombre y del rol al momento de la acción: el registro sigue
    # siendo legible aunque después se borre o cambie el usuario.
    usuario_nombre = models.CharField(max_length=150)
    rol = models.CharField(max_length=20, blank=True)
    accion = models.CharField(max_length=10, choices=Accion.choices)
    recurso = models.CharField(max_length=60)
    objeto_id = models.CharField(max_length=20, blank=True)
    metodo = models.CharField(max_length=8)
    ruta = models.CharField(max_length=255)
    estado = models.PositiveSmallIntegerField()
    ip = models.GenericIPAddressField(null=True, blank=True)
    fecha = models.DateTimeField(default=timezone.now, db_index=True)

    class Meta:
        db_table = 'adm_auditoria'
        ordering = ['-fecha', '-id']

    def __str__(self):
        return f'{self.usuario_nombre} {self.accion} {self.recurso} {self.objeto_id}'.strip()
