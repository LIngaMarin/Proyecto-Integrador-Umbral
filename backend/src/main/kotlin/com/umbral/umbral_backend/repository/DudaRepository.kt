package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.*
import org.springframework.data.jpa.repository.*
import jakarta.persistence.LockModeType

/** Acceso JPA a agrupaciones por criterio y docente. */
interface GrupoDudasRepository : JpaRepository<GrupoDudas,Long> {
    fun findByClave(clave: String): GrupoDudas?
    fun findByDocenteIdOrderByIdDesc(docenteId: Long): List<GrupoDudas>
    /** Bloquea el grupo durante registros, respuestas o resoluciones simultáneas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GrupoDudas g where g.id=:id")
    fun bloquear(id: Long): GrupoDudas?
}

/** Acceso JPA a dudas propias del alumno o pertenecientes a una agrupación. */
interface DudaRepository : JpaRepository<Duda,Long> {
    @Query("select d.grupoId from Duda d where d.id=:id")
    fun grupoDe(id: Long): Long?
    fun findByAlumnoIdOrderByFechaDescIdDesc(alumnoId: Long): List<Duda>
    fun findByGrupoIdOrderByFechaAscIdAsc(grupoId: Long): List<Duda>
}
