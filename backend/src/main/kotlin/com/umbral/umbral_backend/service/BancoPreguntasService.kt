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
/** Coordina las consultas y operaciones de revisión del banco de preguntas. */
class BancoPreguntasService(
    private val preguntaRepository: PreguntaRepository
) {
    // HU 22: Consultar las preguntas de un docente con filtros opcionales.
    /** Mantiene el filtro docente y combina los filtros opcionales presentes. */
    fun obtenerPreguntas(docenteId: Long, tema: String?, dificultad: Dificultad?): List<Pregunta> =
        when {
            tema != null && dificultad != null ->
                preguntaRepository.findByDocenteIdAndTemaAndDificultad(docenteId, tema, dificultad)
            tema != null -> preguntaRepository.findByDocenteIdAndTema(docenteId, tema)
            dificultad != null -> preguntaRepository.findByDocenteIdAndDificultad(docenteId, dificultad)
            else -> preguntaRepository.findByDocenteId(docenteId)
        }

    // HU 18: Extraer preguntas desde boletín (PDF/Imagen)
    /** Construye preguntas a partir del archivo recibido y persiste la lista para el docente. */
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
    /** Busca la pregunta base y guarda una variante pendiente; falla con RuntimeException si falta. */
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
    /** Actualiza los campos del DTO, aprueba la pregunta y limpia el motivo anterior. */
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
    /** Exige un motivo, lo recorta, cambia el estado y guarda la pregunta rechazada. */
    fun rechazarPregunta(id: Long, dto: AprobarRechazarDTO): Pregunta {
        // Evita consultar el repositorio si el motivo solo contiene espacios.
        require(dto.motivoRechazo.isNotBlank()) { "El motivo de rechazo es obligatorio" }

        val pregunta = preguntaRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Pregunta no encontrada con el id: $id")
        }

        pregunta.estado = EstadoPregunta.RECHAZADA
        pregunta.motivoRechazo = dto.motivoRechazo.trim()

        return preguntaRepository.save(pregunta)
    }
}
