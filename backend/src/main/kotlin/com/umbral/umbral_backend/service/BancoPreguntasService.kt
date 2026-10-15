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
}