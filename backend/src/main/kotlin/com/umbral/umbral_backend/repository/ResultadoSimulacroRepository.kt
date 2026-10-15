package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.ResultadoSimulacro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ResultadoSimulacroRepository : JpaRepository<ResultadoSimulacro, Long> {
    fun findBySeccionId(seccionId: Long): List<ResultadoSimulacro>
}
