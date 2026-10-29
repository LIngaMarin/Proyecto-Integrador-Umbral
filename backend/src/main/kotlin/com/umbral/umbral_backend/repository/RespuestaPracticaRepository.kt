package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.RespuestaPractica
import org.springframework.data.jpa.repository.JpaRepository

/** Persiste intentos y comprueba si el alumno ya respondió antes de consultar la solución. */
interface RespuestaPracticaRepository : JpaRepository<RespuestaPractica, Long> {
    fun existsByAlumnoIdAndPreguntaId(alumnoId: Long, preguntaId: Long): Boolean
}
