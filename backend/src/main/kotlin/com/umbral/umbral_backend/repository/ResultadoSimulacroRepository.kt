package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.ResultadoSimulacro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
/** Acceso JPA a los resultados almacenados de simulacros. */
interface ResultadoSimulacroRepository : JpaRepository<ResultadoSimulacro, Long> {
    /** Recupera los registros cuyo seccionId coincide con el argumento. */
    fun findBySeccionId(seccionId: Long): List<ResultadoSimulacro>
}
