package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.ResumenSemanal
import org.springframework.data.jpa.repository.*
import jakarta.persistence.LockModeType
import java.time.LocalDate

interface ResumenSemanalRepository : JpaRepository<ResumenSemanal,Long> {
    fun findByDocenteIdAndSemanaInicio(docenteId: Long,semanaInicio: LocalDate): ResumenSemanal?
    fun findTop100ByEstadoInAndIntentosLessThanOrderByIdAsc(estados: List<String>,intentos: Int): List<ResumenSemanal>
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResumenSemanal r where r.id=:id")
    fun bloquear(id: Long): ResumenSemanal?
}
