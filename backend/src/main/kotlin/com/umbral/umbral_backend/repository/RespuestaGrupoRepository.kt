package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.RespuestaGrupo
import org.springframework.data.jpa.repository.JpaRepository

interface RespuestaGrupoRepository : JpaRepository<RespuestaGrupo,Long> {
    fun findByGrupoIdOrderByFechaAscIdAsc(grupoId: Long): List<RespuestaGrupo>
}
