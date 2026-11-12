package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.model.EstadoPregunta
import com.umbral.umbral_backend.security.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

data class SolucionAlumnoDTO(val preguntaId: Long, val respuestaCorrecta: String,
    val pasoAPaso: String, val explicacionErroresComunes: String?)

@Service
class ConsultaSolucionarioService(private val preguntas: PreguntaRepository,
    private val respuestas: RespuestaPracticaRepository, private val soluciones: SolucionarioService,
    private val identidad: IdentidadAcademica) {
    /** Consulta la solución aprobada después de comprobar el acceso y un intento propio del alumno. */
    @Transactional(readOnly = true)
    fun consultar(alumno: Actor, preguntaId: Long): SolucionAlumnoDTO {
        // Evita revelar si una pregunta no publicada existe en el banco docente.
        val p = preguntas.findById(preguntaId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "La pregunta no está disponible") }
        if (p.estado != EstadoPregunta.APROBADA) throw ResponseStatusException(HttpStatus.NOT_FOUND, "La pregunta no está disponible")
        identidad.exigirPregunta(alumno.id, p.docenteId)
        if (!respuestas.existsByAlumnoIdAndPreguntaId(alumno.id, p.id))
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Debe responder primero")
        val s = soluciones.aprobadoParaPregunta(p.id)
        return SolucionAlumnoDTO(p.id,p.respuestaCorrecta,s.pasoAPaso,s.explicacionErroresComunes)
    }
}
