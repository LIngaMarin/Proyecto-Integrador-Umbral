package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.Solucionario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
/** Acceso JPA a solucionarios y comprobación de su asociación con preguntas. */
interface SolucionarioRepository : JpaRepository<Solucionario, Long> {
    /** Comprueba si existe un solucionario relacionado con el id de pregunta indicado. */
    fun existsByPregunta_Id(preguntaId: Long): Boolean
}
