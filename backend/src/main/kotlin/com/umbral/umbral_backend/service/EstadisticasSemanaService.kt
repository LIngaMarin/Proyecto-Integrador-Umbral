package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.security.Actor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.*
import java.time.temporal.TemporalAdjusters

data class EstadisticasSemanaDTO(
    val inicio: LocalDate,val finExclusivo: LocalDate,
    val preguntasCreadas: Long,val preguntasCreadasActualmenteAprobadas: Long,
    val intentosPractica: Long,val aciertosPractica: Long,val alumnosPractica: Long,
    val dudasRegistradas: Long,val respuestasAgrupadas: Long,val dudasResueltas: Long
) {
    /** Prepara el texto del correo con las estadísticas calculadas de la semana. */
    fun texto(): String = """
        Resumen UMBRAL del $inicio al ${finExclusivo.minusDays(1)}
        Preguntas creadas: $preguntasCreadas
        De esas preguntas, actualmente aprobadas: $preguntasCreadasActualmenteAprobadas
        Intentos de práctica: $intentosPractica
        Aciertos: $aciertosPractica
        Alumnos que practicaron: $alumnosPractica
        Dudas registradas: $dudasRegistradas
        Respuestas a agrupaciones: $respuestasAgrupadas
        Dudas resueltas: $dudasResueltas
    """.trimIndent()
}

@Service
class EstadisticasSemanaService(private val jdbc: JdbcTemplate,private val clock: Clock) {
    /** Obtiene el lunes de la semana anterior según la zona horaria configurada. */
    fun semanaAnterior(): LocalDate = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)

    /** Cuenta la actividad del docente durante una semana completa usando registros persistidos. */
    @Transactional(readOnly=true)
    fun calcular(docente: Actor,inicio: LocalDate): EstadisticasSemanaDTO {
        validarSemana(inicio,LocalDate.now(clock))
        val desde=inicio.atStartOfDay()
        // Incluye el lunes inicial y excluye el siguiente para no mezclar semanas.
        val hasta=inicio.plusWeeks(1).atStartOfDay()
        fun contar(sql: String): Long = jdbc.queryForObject(sql,Long::class.java,docente.id,desde,hasta) ?: 0L
        val preguntas="FROM preguntas WHERE docente_id=? AND fecha_creacion>=? AND fecha_creacion<?"
        val intentos="FROM respuestas_practica r JOIN preguntas p ON p.id=r.pregunta_id WHERE p.docente_id=? AND r.fecha>=? AND r.fecha<?"
        return EstadisticasSemanaDTO(inicio,inicio.plusWeeks(1),
            contar("SELECT COUNT(*) $preguntas"),
            contar("SELECT COUNT(*) $preguntas AND estado='APROBADA'"),
            contar("SELECT COUNT(*) $intentos"),
            contar("SELECT COUNT(*) $intentos AND r.correcta=true"),
            contar("SELECT COUNT(DISTINCT r.alumno_id) $intentos"),
            contar("""SELECT COUNT(*) FROM dudas d JOIN grupos_dudas g ON g.id=d.grupo_id
                WHERE g.docente_id=? AND d.fecha>=? AND d.fecha<?"""),
            contar("SELECT COUNT(*) FROM respuestas_grupo WHERE docente_id=? AND fecha>=? AND fecha<?"),
            contar("""SELECT COUNT(*) FROM historial_dudas h JOIN dudas d ON d.id=h.duda_id
                JOIN grupos_dudas g ON g.id=d.grupo_id WHERE g.docente_id=? AND h.fecha>=? AND h.fecha<? AND h.evento='RESUELTA'"""))
    }
    companion object {
        /** Exige un lunes de inicio y una semana que ya haya terminado. */
        fun validarSemana(inicio: LocalDate,hoy: LocalDate) {
            // Rechaza fechas futuras antes de sumar días para evitar desbordamientos.
            if(inicio.dayOfWeek!=DayOfWeek.MONDAY || inicio.isAfter(hoy) || inicio.plusWeeks(1).isAfter(hoy))
                throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Indique el lunes de una semana completa")
        }
    }
}
