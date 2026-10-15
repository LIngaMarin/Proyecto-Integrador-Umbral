from django.core.exceptions import ValidationError
from django.core.validators import MinValueValidator
from django.db import models


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
