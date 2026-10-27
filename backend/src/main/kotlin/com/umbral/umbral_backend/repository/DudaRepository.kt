package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.*
import org.springframework.data.jpa.repository.*
import jakarta.persistence.LockModeType

interface GrupoDudasRepository : JpaRepository<GrupoDudas,Long> {
    fun findByClave(clave: String): GrupoDudas?
    fun findByDocenteIdOrderByIdDesc(docenteId: Long): List<GrupoDudas>
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GrupoDudas g where g.id=:id")
    fun bloquear(id: Long): GrupoDudas?
}

interface DudaRepository : JpaRepository<Duda,Long> {
    fun findByAlumnoIdOrderByFechaDescIdDesc(alumnoId: Long): List<Duda>
    fun findByGrupoIdOrderByFechaAscIdAsc(grupoId: Long): List<Duda>
}
