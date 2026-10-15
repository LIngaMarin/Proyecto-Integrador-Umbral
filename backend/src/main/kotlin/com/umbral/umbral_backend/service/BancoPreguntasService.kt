package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.ActualizarPreguntaDTO
import com.umbral.umbral_backend.dto.AprobarRechazarDTO
import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.multipart.MultipartFile

@Service
class BancoPreguntasService(
    private val preguntaRepository: PreguntaRepository
) {
    // HU 18: Extraer preguntas desde boletín (PDF/Imagen)
    fun extraerPreguntasDeBoletin(file: MultipartFile, docenteId: Long): List<Pregunta> {
        val preguntasExtraidas = listOf(
            Pregunta(
                enunciado = "Extraído de ${file.originalFilename}: ¿Cuál es el valor de X si 2x + 6 = 14?",
                tema = "Álgebra",
                dificultad = Dificultad.FACIL,
                opciones = listOf("2", "4", "6", "8"),
                respuestaCorrecta = "4",
                estado = EstadoPregunta.PENDIENTE,
                docenteId = docenteId
            )
        )
        return preguntaRepository.saveAll(preguntasExtraidas)
    }

    // HU 19: Generar variante con IA
    fun generarVariante(preguntaId: Long, docenteId: Long): Pregunta {
        val original = preguntaRepository.findById(preguntaId).orElseThrow {
            RuntimeException("Pregunta no encontrada")
        }
        val variante = Pregunta(
            enunciado = "Variante IA de (${original.enunciado}): Si 3x + 9 = 21, halle X.",
            tema = original.tema,
            dificultad = original.dificultad,
            opciones = listOf("3", "4", "5", "6"),
            respuestaCorrecta = "4",
            estado = EstadoPregunta.PENDIENTE,
            docenteId = docenteId
        )
        return preguntaRepository.save(variante)
    }

    // HU 20: Revisar, editar y aprobar pregunta
    fun editarYAprobar(id: Long, dto: ActualizarPreguntaDTO): Pregunta {
        val pregunta = preguntaRepository.findById(id).orElseThrow {
            RuntimeException("Pregunta no encontrada con el id: $id")
        }

        pregunta.enunciado = dto.enunciado
        pregunta.tema = dto.tema
        pregunta.dificultad = dto.dificultad
        pregunta.opciones = dto.opciones
        pregunta.respuestaCorrecta = dto.respuestaCorrecta
        pregunta.estado = EstadoPregunta.APROBADA
        pregunta.motivoRechazo = null

        return preguntaRepository.save(pregunta)
    }

    // HU 21: Rechazar preguntas con motivo
    fun rechazarPregunta(id: Long, dto: AprobarRechazarDTO): Pregunta {
        require(dto.motivoRechazo.isNotBlank()) { "El motivo de rechazo es obligatorio" }

        val pregunta = preguntaRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Pregunta no encontrada con el id: $id")
        }

        pregunta.estado = EstadoPregunta.RECHAZADA
        pregunta.motivoRechazo = dto.motivoRechazo.trim()

        return preguntaRepository.save(pregunta)
    }
}
