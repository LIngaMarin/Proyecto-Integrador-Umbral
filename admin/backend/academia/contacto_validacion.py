import re

from django.contrib.auth.models import User
from django.core.exceptions import ValidationError
from django.core.validators import validate_email

PATRON_TELEFONO = re.compile(r'^\+?\d{7,15}$')


def validar_telefono(valor):
    """Devuelve el teléfono sin espacios ni guiones. Vacío significa sin teléfono."""
    limpio = re.sub(r'[\s-]', '', str(valor or ''))
    if limpio and not PATRON_TELEFONO.match(limpio):
        raise ValidationError('Ingresa un teléfono válido: solo números, de 7 a 15 dígitos.')
    return limpio


def validar_correo(valor, usuario):
    """Devuelve el correo en minúsculas. No puede repetirse en otro usuario."""
    correo = str(valor or '').strip().lower()
    if not correo:
        raise ValidationError('El correo es obligatorio.')
    validate_email(correo)
    if User.objects.filter(username__iexact=correo).exclude(pk=usuario.pk).exists():
        raise ValidationError('Ya existe un usuario con este correo.')
    return correo
