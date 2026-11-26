package com.umbral.app.domain

import java.util.Calendar
import java.util.Locale

/** Un momento de la semana. dia: 1 = lunes ... 7 = domingo. hora en formato "HH:mm". */
data class MomentoSemana(val dia: Int, val hora: String)

private val NOMBRES_DIA = listOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

fun nombreDia(dia: Int): String = NOMBRES_DIA.getOrElse(dia) { "Día $dia" }

/** Convierte la fecha y hora del celular al formato del horario (lunes = 1). */
fun momentoActual(calendario: Calendar = Calendar.getInstance()): MomentoSemana {
    // Calendar: domingo = 1 ... sabado = 7  ->  lunes = 1 ... domingo = 7
    val dia = ((calendario.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
    val hora = String.format(
        Locale.US, "%02d:%02d",
        calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE),
    )
    return MomentoSemana(dia, hora)
}

/**
 * La clase que sigue desde [ahora]: la que esta en curso o la proxima de la semana.
 * Si ya pasaron todas, devuelve la primera de la semana siguiente.
 */
fun proximaClase(clases: List<Clase>, ahora: MomentoSemana): Clase? {
    val ordenadas = clases.sortedWith(compareBy<Clase>({ it.dia }, { it.horaInicio }))
    return ordenadas.firstOrNull { it.dia > ahora.dia || (it.dia == ahora.dia && it.horaFin > ahora.hora) }
        ?: ordenadas.firstOrNull()
}
