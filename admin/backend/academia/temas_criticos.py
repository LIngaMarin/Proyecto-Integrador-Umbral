"""HU 6: temas con más errores por salón, con análisis de IA (Gemini)."""
import json
import logging
import urllib.request

from django.conf import settings
from django.db.models import Count, Sum

from .models import ErrorTema, Resultado

logger = logging.getLogger(__name__)

MAX_TEMAS_POR_SECCION = 5


def temas_por_seccion(simulacro, seccion_id=None):
    """Devuelve los salones del simulacro con sus temas ordenados por errores."""
    resultados = Resultado.objects.filter(simulacro=simulacro)
    if seccion_id:
        resultados = resultados.filter(matricula__seccion_id=seccion_id)

    evaluados = {
        f['matricula__seccion']: f
        for f in resultados.values(
            'matricula__seccion',
            'matricula__seccion__nombre',
            'matricula__seccion__curso__nombre',
        ).annotate(alumnos=Count('id'))
    }
    errores = (
        ErrorTema.objects.filter(resultado__in=resultados, cantidad__gt=0)
        .values('resultado__matricula__seccion', 'tema__nombre')
        .annotate(errores=Sum('cantidad'), alumnos_con_error=Count('resultado', distinct=True))
        .order_by('-errores', 'tema__nombre')
    )

    por_seccion = {seccion_id: [] for seccion_id in evaluados}
    for fila in errores:
        seccion = fila['resultado__matricula__seccion']
        total = evaluados[seccion]['alumnos']
        por_seccion[seccion].append(
            {
                'tema': fila['tema__nombre'],
                'errores': fila['errores'],
                'alumnos_con_error': fila['alumnos_con_error'],
                'porcentaje_alumnos': round(100 * fila['alumnos_con_error'] / total),
            }
        )

    secciones = [
        {
            'seccion': seccion_id,
            'nombre': f"{datos['matricula__seccion__curso__nombre']} - {datos['matricula__seccion__nombre']}",
            'alumnos_evaluados': datos['alumnos'],
            'temas': por_seccion[seccion_id][:MAX_TEMAS_POR_SECCION],
        }
        for seccion_id, datos in evaluados.items()
    ]
    secciones.sort(
        key=lambda s: (-sum(t['errores'] for t in s['temas']), s['nombre'])
    )
    return secciones


def resumen_basico(seccion):
    """Resumen sin IA: sirve cuando no hay clave de Gemini o la consulta falla."""
    temas = seccion['temas']
    if not temas:
        return 'Este salón no tiene errores registrados por tema en el simulacro.'
    principales = ', '.join(
        f"{t['tema']} ({t['errores']} errores, {t['porcentaje_alumnos']}% de los alumnos)"
        for t in temas[:3]
    )
    return (
        f"Temas con más errores: {principales}. "
        f"Se sugiere reforzar «{temas[0]['tema']}» en las próximas clases."
    )


def construir_prompt(secciones):
    lineas = []
    for s in secciones:
        temas = '; '.join(
            f"{t['tema']}: {t['errores']} errores, {t['porcentaje_alumnos']}% de alumnos"
            for t in s['temas']
        ) or 'sin errores registrados'
        lineas.append(f"- seccion {s['seccion']} ({s['nombre']}, {s['alumnos_evaluados']} alumnos): {temas}")
    return (
        'Eres un asistente de coordinación académica de una academia preuniversitaria. '
        'Con los datos de errores por tema de cada salón, escribe para cada uno un resumen '
        'de máximo 50 palabras que nombre los temas críticos y proponga una acción de '
        'refuerzo concreta para el docente. Escribe en español.\n\n'
        + '\n'.join(lineas)
        + '\n\nResponde SOLO con JSON con esta forma: '
        '{"secciones": [{"seccion": <id numérico>, "resumen": "<texto>"}]}'
    )


def llamar_gemini(prompt):
    """Envía el prompt a Gemini y devuelve el texto de la respuesta."""
    url = (
        'https://generativelanguage.googleapis.com/v1beta/models/'
        f'{settings.GEMINI_MODEL}:generateContent'
    )
    cuerpo = json.dumps(
        {
            'contents': [{'parts': [{'text': prompt}]}],
            'generationConfig': {'responseMimeType': 'application/json', 'temperature': 0.3},
        }
    ).encode('utf-8')
    peticion = urllib.request.Request(
        url,
        data=cuerpo,
        headers={'Content-Type': 'application/json', 'x-goog-api-key': settings.GEMINI_API_KEY},
    )
    with urllib.request.urlopen(peticion, timeout=25) as respuesta:
        datos = json.load(respuesta)
    return datos['candidates'][0]['content']['parts'][0]['text']


def analizar_con_ia(secciones):
    """Agrega 'resumen' a cada salón. Devuelve (secciones, estado_de_la_ia)."""
    for s in secciones:
        s['resumen'] = resumen_basico(s)

    if not settings.GEMINI_API_KEY:
        return secciones, {'disponible': False, 'motivo': 'No hay clave de Gemini configurada.'}
    if not any(s['temas'] for s in secciones):
        return secciones, {'disponible': False, 'motivo': 'No hay errores por tema para analizar.'}

    try:
        texto = llamar_gemini(construir_prompt(secciones))
        resumenes = {
            int(item['seccion']): str(item['resumen']).strip()
            for item in json.loads(texto)['secciones']
        }
    except Exception as error:  # la IA nunca debe tumbar la pantalla
        logger.warning('No se pudo analizar con Gemini: %s', type(error).__name__)
        return secciones, {'disponible': False, 'motivo': 'No se pudo consultar a Gemini.'}

    for s in secciones:
        if resumenes.get(s['seccion']):
            s['resumen'] = resumenes[s['seccion']]
    return secciones, {'disponible': True, 'motivo': None}
