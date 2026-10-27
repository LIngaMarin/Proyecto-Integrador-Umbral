package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.RespuestaPractica
import org.springframework.data.jpa.repository.JpaRepository

interface RespuestaPracticaRepository : JpaRepository<RespuestaPractica, Long> {
    fun existsByAlumnoIdAndPreguntaId(alumnoId: Long, preguntaId: Long): Boolean
}
