package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.RegistrarDudaDTO
import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.security.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

@Service
class DudaService(private val grupos: GrupoDudasRepository, private val dudas: DudaRepository,
    private val preguntas: PreguntaRepository, private val identidad: IdentidadAcademica,
    private val historial: HistorialDudaRepository) {
    @Transactional
    fun registrar(alumno: Actor, dto: RegistrarDudaDTO): Duda {
        identidad.exigirMatricula(alumno.id,dto.seccionId)
        identidad.exigirSeccion(dto.docenteId,dto.seccionId)
        val pregunta = dto.preguntaId?.let { preguntas.findById(it).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) } }
        if (pregunta != null && (pregunta.estado != EstadoPregunta.APROBADA || pregunta.docenteId != dto.docenteId))
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val tema = pregunta?.tema ?: dto.tema?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Indique tema o pregunta")
        val agrupacion = pregunta?.let { "P:${it.id}" } ?: "T:${normalizarTema(tema)}"
        val clave = MessageDigest.getInstance("SHA-256").digest("${dto.docenteId}:${dto.seccionId}:$agrupacion".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val grupo = grupos.findByClave(clave) ?: grupos.saveAndFlush(GrupoDudas(clave=clave,docenteId=dto.docenteId,
            seccionId=dto.seccionId,tema=tema,preguntaId=pregunta?.id))
        // Serializa registro y respuesta docente para no perder dudas concurrentes.
        grupos.bloquear(grupo.id)
        val duda=dudas.saveAndFlush(Duda(grupoId=grupo.id,alumnoId=alumno.id,texto=dto.texto.trim()))
        historial.save(HistorialDuda(dudaId=duda.id,actorId=alumno.id,rol=alumno.rol,evento="REGISTRADA"))
        return duda
    }

    @Transactional(readOnly=true)
    fun propias(alumno: Actor,estado: EstadoDuda? = null): List<Duda> = dudas.findByAlumnoIdOrderByFechaDescIdDesc(alumno.id)
        .filter { estado==null || it.estado==estado.name }

    @Transactional(readOnly=true)
    fun agrupaciones(docente: Actor): List<GrupoDudas> = grupos.findByDocenteIdOrderByIdDesc(docente.id)
        .filter { grupo ->
            try { identidad.exigirSeccion(docente.id,grupo.seccionId); true }
            catch (ex: ResponseStatusException) { if (ex.statusCode.value()==403) false else throw ex }
        }

    @Transactional(readOnly=true)
    fun delGrupo(docente: Actor, grupoId: Long,estado: EstadoDuda? = null): List<Duda> {
        exigirGrupo(docente,grupoId)
        return dudas.findByGrupoIdOrderByFechaAscIdAsc(grupoId).filter { estado==null || it.estado==estado.name }
    }

    fun exigirGrupo(docente: Actor, grupoId: Long): GrupoDudas {
        val g = grupos.findById(grupoId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (g.docenteId!=docente.id) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        identidad.exigirSeccion(docente.id,g.seccionId)
        return g
    }

    companion object {
        fun normalizarTema(tema: String): String = Normalizer.normalize(tema,Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"),"").trim().lowercase(Locale.ROOT).replace(Regex("\\s+")," ")
    }
}
