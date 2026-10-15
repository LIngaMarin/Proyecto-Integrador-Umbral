package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.Solucionario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SolucionarioRepository : JpaRepository<Solucionario, Long> {
    fun existsByPregunta_Id(preguntaId: Long): Boolean
}
