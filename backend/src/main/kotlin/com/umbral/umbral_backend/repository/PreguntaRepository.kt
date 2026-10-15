package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.Pregunta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PreguntaRepository : JpaRepository<Pregunta, Long> {
    fun findByDocenteId(docenteId: Long): List<Pregunta>

    fun findByDocenteIdAndTema(docenteId: Long, tema: String): List<Pregunta>

    fun findByDocenteIdAndDificultad(docenteId: Long, dificultad: Dificultad): List<Pregunta>

    fun findByDocenteIdAndTemaAndDificultad(
        docenteId: Long,
        tema: String,
        dificultad: Dificultad
    ): List<Pregunta>
}
