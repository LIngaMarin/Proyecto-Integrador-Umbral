package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.security.Actor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class RespuestaGrupoService(private val grupos: GrupoDudasRepository,private val dudas: DudaRepository,
    private val respuestas: RespuestaGrupoRepository,private val service: DudaService,
    private val historial: HistorialDudaRepository) {
    @Transactional
    fun responder(docente: Actor,grupoId: Long,texto: String): RespuestaGrupo {
        service.exigirGrupo(docente,grupoId)
        grupos.bloquear(grupoId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val pendientes=dudas.findByGrupoIdOrderByFechaAscIdAsc(grupoId).filter { it.estado=="PENDIENTE" && it.respuestaId==null }
        if (pendientes.isEmpty()) throw ResponseStatusException(HttpStatus.CONFLICT,"No hay dudas sin respuesta")
        val respuesta=respuestas.saveAndFlush(RespuestaGrupo(grupoId=grupoId,docenteId=docente.id,texto=texto.trim()))
        pendientes.forEach {
            it.respuestaId=respuesta.id
            historial.save(HistorialDuda(dudaId=it.id,actorId=docente.id,rol=docente.rol,evento="RESPONDIDA"))
        }
        dudas.saveAll(pendientes)
        return respuesta
    }

    @Transactional(readOnly=true)
    fun consultar(alumno: Actor,dudaId: Long): RespuestaGrupo {
        val d=dudas.findById(dudaId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if(d.alumnoId!=alumno.id) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        return d.respuestaId?.let { respuestas.findById(it).orElse(null) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND,"La duda aún no tiene respuesta")
    }

    @Transactional(readOnly=true)
    fun historial(docente: Actor,grupoId: Long): List<RespuestaGrupo> {
        service.exigirGrupo(docente,grupoId)
        return respuestas.findByGrupoIdOrderByFechaAscIdAsc(grupoId)
    }
}
