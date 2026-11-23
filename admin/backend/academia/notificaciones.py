from django.db.models.signals import post_save
from django.dispatch import receiver

from .models import Apoderado, Asistencia, Notificacion


def _mensaje_inasistencia(asistencia):
    alumno = asistencia.alumno.user
    nombre = alumno.get_full_name() or alumno.username
    return f'{nombre} no asistió a la academia el {asistencia.fecha:%d/%m/%Y}.'


@receiver(post_save, sender=Asistencia)
def notificar_inasistencia(sender, instance, raw=False, **kwargs):
    """Avisa a cada apoderado del alumno cuando se registra una falta.

    get_or_create evita duplicar si la misma asistencia se guarda de nuevo.
    """
    if raw or instance.estado != Asistencia.Estado.FALTA:
        return
    mensaje = _mensaje_inasistencia(instance)
    for apoderado in Apoderado.objects.filter(hijos=instance.alumno):
        Notificacion.objects.get_or_create(
            apoderado=apoderado,
            asistencia=instance,
            defaults={
                'alumno': instance.alumno,
                'tipo': Notificacion.Tipo.INASISTENCIA,
                'mensaje': mensaje,
            },
        )
