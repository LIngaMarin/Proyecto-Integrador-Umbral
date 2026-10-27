package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.security.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class RespuestaPracticaService(
    private val preguntas: PreguntaRepository,
    private val respuestas: RespuestaPracticaRepository,
    private val identidad: IdentidadAcademica
) {
    @Transactional
    fun registrar(alumno: Actor, preguntaId: Long, respuesta: String): RespuestaPractica {
        if (respuesta.isBlank() || respuesta.length > 255 || preguntaId <= 0)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Respuesta o pregunta inválida")
        val pregunta = preguntas.findById(preguntaId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (pregunta.estado != EstadoPregunta.APROBADA) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        identidad.exigirPregunta(alumno.id, pregunta.docenteId)
        if (respuesta !in pregunta.opciones) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione una opción válida")
        return respuestas.save(RespuestaPractica(preguntaId = pregunta.id, alumnoId = alumno.id,
            respuesta = respuesta, correcta = respuesta == pregunta.respuestaCorrecta))
    }
}
