package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.EstadoPregunta
import com.umbral.umbral_backend.model.Pregunta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
/** Acceso JPA a preguntas; Spring Data deriva filtros de los nombres de estos métodos. */
interface PreguntaRepository : JpaRepository<Pregunta, Long> {
    fun findByEstadoAndTemaAndDificultad(
        estado: EstadoPregunta,
        tema: String,
        dificultad: Dificultad
    ): List<Pregunta>

    /** Busca exclusivamente por el identificador docente. */
    fun findByDocenteId(docenteId: Long): List<Pregunta>

    /** Combina el filtro de docente con una coincidencia de tema. */
    fun findByDocenteIdAndTema(docenteId: Long, tema: String): List<Pregunta>

    /** Combina el filtro de docente con el valor de dificultad. */
    fun findByDocenteIdAndDificultad(docenteId: Long, dificultad: Dificultad): List<Pregunta>

    /** Aplica simultáneamente docente, tema y dificultad. */
    fun findByDocenteIdAndTemaAndDificultad(
        docenteId: Long,
        tema: String,
        dificultad: Dificultad
    ): List<Pregunta>
}
