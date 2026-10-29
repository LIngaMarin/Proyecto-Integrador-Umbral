package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.HistorialDuda
import org.springframework.data.jpa.repository.JpaRepository

/** Consulta los eventos de una duda en orden de fecha e identificador. */
interface HistorialDudaRepository : JpaRepository<HistorialDuda,Long> {
    fun findByDudaIdOrderByFechaAscIdAsc(dudaId: Long): List<HistorialDuda>
}
