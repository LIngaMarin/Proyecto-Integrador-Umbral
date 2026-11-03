"""HU 7: comparar el rendimiento de un salón entre dos simulacros."""
from django.db.models import Avg, Count

from .models import Resultado
from .rendimiento import UMBRAL_REFUERZO


def promedios_por_seccion(simulacro):
    """Devuelve {id_seccion: {nombre, promedio, evaluados}} de un simulacro."""
    filas = (
        Resultado.objects.filter(simulacro=simulacro)
        .values(
            'matricula__seccion',
            'matricula__seccion__nombre',
            'matricula__seccion__curso__nombre',
        )
        .annotate(promedio=Avg('puntaje'), evaluados=Count('id'))
    )
    return {
        f['matricula__seccion']: {
            'nombre': f"{f['matricula__seccion__curso__nombre']} - {f['matricula__seccion__nombre']}",
            'promedio': round(float(f['promedio']), 2),
            'evaluados': f['evaluados'],
        }
        for f in filas
    }


def _tendencia(diferencia):
    if diferencia is None:
        return 'sin_datos'
    if diferencia > 0:
        return 'mejora'
    if diferencia < 0:
        return 'baja'
    return 'igual'


def comparar_simulacros(anterior, actual):
    """Compara el promedio de cada sección entre dos simulacros.

    Una sección que solo aparece en uno de los dos queda con tendencia
    'sin_datos', porque no hay nada con qué compararla.
    """
    antes = promedios_por_seccion(anterior)
    ahora = promedios_por_seccion(actual)

    secciones = []
    for seccion_id in set(antes) | set(ahora):
        a, b = antes.get(seccion_id), ahora.get(seccion_id)
        prom_antes = a['promedio'] if a else None
        prom_ahora = b['promedio'] if b else None
        diferencia = (
            round(prom_ahora - prom_antes, 2)
            if prom_antes is not None and prom_ahora is not None
            else None
        )
        secciones.append(
            {
                'seccion': seccion_id,
                'nombre': (b or a)['nombre'],
                'promedio_anterior': prom_antes,
                'promedio_actual': prom_ahora,
                'evaluados_anterior': a['evaluados'] if a else 0,
                'evaluados_actual': b['evaluados'] if b else 0,
                'diferencia': diferencia,
                'tendencia': _tendencia(diferencia),
                'necesita_refuerzo': prom_ahora is not None and prom_ahora < UMBRAL_REFUERZO,
                'refuerzo_resuelto': (
                    prom_antes is not None
                    and prom_ahora is not None
                    and prom_antes < UMBRAL_REFUERZO <= prom_ahora
                ),
            }
        )
    secciones.sort(key=lambda s: s['nombre'])

    return {
        'umbral': UMBRAL_REFUERZO,
        'resumen': {
            'mejoraron': sum(s['tendencia'] == 'mejora' for s in secciones),
            'bajaron': sum(s['tendencia'] == 'baja' for s in secciones),
            'igual': sum(s['tendencia'] == 'igual' for s in secciones),
            'sin_datos': sum(s['tendencia'] == 'sin_datos' for s in secciones),
        },
        'secciones': secciones,
    }
