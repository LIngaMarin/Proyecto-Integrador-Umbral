package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.RespuestaGrupo
import org.springframework.data.jpa.repository.JpaRepository

/** Persiste las respuestas docentes y consulta el historial de cada agrupación. */
interface RespuestaGrupoRepository : JpaRepository<RespuestaGrupo,Long> {
    fun findByGrupoIdOrderByFechaAscIdAsc(grupoId: Long): List<RespuestaGrupo>
}
