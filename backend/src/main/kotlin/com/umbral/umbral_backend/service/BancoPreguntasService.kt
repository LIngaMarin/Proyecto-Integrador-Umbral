package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.springframework.stereotype.Service
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
}