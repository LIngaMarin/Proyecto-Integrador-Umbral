package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.security.Actor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class ResolucionDudaService(private val dudas: DudaRepository,private val grupos: GrupoDudasRepository,
    private val historial: HistorialDudaRepository,private val service: DudaService) {
    @Transactional
    fun resolver(actor: Actor,id: Long): Duda {
        val grupoId=dudas.grupoDe(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        grupos.bloquear(grupoId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val d=exigirAcceso(actor,id)
        if(d.estado=="RESUELTA") return d
        if(d.respuestaId==null) throw ResponseStatusException(HttpStatus.CONFLICT,"La duda necesita una respuesta antes de resolverse")
        d.estado="RESUELTA"
        historial.save(HistorialDuda(dudaId=d.id,actorId=actor.id,rol=actor.rol,evento="RESUELTA"))
        return dudas.save(d)
    }
    @Transactional(readOnly=true)
    fun consultarHistorial(actor: Actor,id: Long): List<HistorialDuda> {
        exigirAcceso(actor,id)
        return historial.findByDudaIdOrderByFechaAscIdAsc(id)
    }
    private fun exigirAcceso(actor: Actor,id: Long): Duda {
        val d=dudas.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        when(actor.rol) {
            "ALUMNO" -> if(d.alumnoId!=actor.id) throw ResponseStatusException(HttpStatus.FORBIDDEN)
            "DOCENTE" -> service.exigirGrupo(actor,d.grupoId)
            else -> throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        return d
    }
}
