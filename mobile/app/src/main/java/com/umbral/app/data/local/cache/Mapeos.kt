package com.umbral.app.data.local.cache

import com.umbral.app.domain.Clase
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.PreguntaPractica

/** Separador para guardar una lista de textos en una sola columna. */
private const val SEP = "\u001F"

fun Clase.aEntidad() = ClaseCacheEntity(id, curso, seccion, aula, sede, dia, horaInicio, horaFin, docente)

fun ClaseCacheEntity.aDominio() = Clase(id, curso, seccion, aula, sede, dia, horaInicio, horaFin, docente)

fun PreguntaPractica.aEntidad() = PreguntaCacheEntity(
    id = id,
    tema = tema,
    enunciado = enunciado,
    opciones = opciones.joinToString(SEP),
    correcta = correcta,
    solucionario = solucionario.joinToString(SEP),
    dificultad = dificultad.name,
)

fun PreguntaCacheEntity.aDominio() = PreguntaPractica(
    id = id,
    tema = tema,
    enunciado = enunciado,
    opciones = opciones.split(SEP),
    correcta = correcta,
    solucionario = if (solucionario.isEmpty()) emptyList() else solucionario.split(SEP),
    dificultad = Dificultad.entries.firstOrNull { it.name == dificultad } ?: Dificultad.MEDIA,
)
